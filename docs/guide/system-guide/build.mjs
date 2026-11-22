#!/usr/bin/env node
/**
 * Build docs/system-guide.html from the template and the PlantUML SVGs.
 *
 * The output is one self-contained file: every diagram is inlined as a
 * base64 SVG data URI and every PlantUML source is embedded, so the guide
 * works offline and can be opened straight from disk.
 *
 * Usage:
 *   node docs/system-guide/build.mjs
 */

import { readFile, writeFile, stat } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const here = path.dirname(fileURLToPath(import.meta.url));
const repo = path.resolve(here, "..", "..");
const templatePath = path.join(here, "system-guide.template.html");
const diagramsDir = path.join(here, "diagrams");
const outputPath = path.join(repo, "docs", "system-guide.html");

const diagrams = {
  "use-case": {
    alt: "UML use-case diagram: one local shopper, ten use cases and three external participants.",
  },
  component: {
    alt: "UML component diagram: browser, Spring Boot process, H2 file database, price feed and DeepSeek API.",
  },
  "domain-catalog": {
    alt: "UML class diagram of the catalogue, shopping list and wishlist entities.",
  },
  "domain-tracking": {
    alt: "UML class diagram of tracking, price observations, alerts, watch rules, reminders and settings.",
  },
  "sequence-import": {
    alt: "UML sequence diagram of a LIVE catalogue refresh inside one transaction.",
  },
  "sequence-cart": {
    alt: "UML sequence diagram of adding to a cart, pricing it, transferring JSON and rendering an offline checklist.",
  },
  "sequence-alerts": {
    alt: "UML sequence diagram of retailer tracking, the 60-second scheduler and the merged alert timeline.",
  },
  "sequence-ask": {
    alt: "UML sequence diagram of one Ask turn with bounded tool calls and confirmation.",
  },
};

function escapeHtml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;");
}

function readSvgSize(svg) {
  const tag = svg.match(/<svg\b[^>]*>/i);
  if (!tag) return null;
  const width = tag[0].match(/\bwidth="([\d.]+)(?:px)?"/i);
  const height = tag[0].match(/\bheight="([\d.]+)(?:px)?"/i);
  if (!width || !height) return null;
  return { width: Math.round(Number(width[1])), height: Math.round(Number(height[1])) };
}

const template = await readFile(templatePath, "utf8");
let html = template;
const report = [];

for (const [name, meta] of Object.entries(diagrams)) {
  const svgPath = path.join(diagramsDir, `${name}.svg`);
  const pumlPath = path.join(diagramsDir, `${name}.puml`);

  let svg;
  let puml;
  try {
    [svg, puml] = await Promise.all([
      readFile(svgPath, "utf8"),
      readFile(pumlPath, "utf8"),
    ]);
  } catch (error) {
    throw new Error(`Missing diagram assets for "${name}": ${error.message}`);
  }

  const size = readSvgSize(svg);
  if (!size) throw new Error(`Could not read width/height from ${svgPath}`);

  const dataUri = `data:image/svg+xml;base64,${Buffer.from(svg, "utf8").toString("base64")}`;
  const img = `<img src="${dataUri}" alt="${escapeHtml(meta.alt)}" width="${size.width}" height="${size.height}" decoding="async">`;

  const diagramToken = `{{DIAGRAM:${name}}}`;
  const sourceToken = `{{SOURCE:${name}}}`;
  if (!html.includes(diagramToken)) throw new Error(`Template is missing ${diagramToken}`);
  if (!html.includes(sourceToken)) throw new Error(`Template is missing ${sourceToken}`);

  html = html.replaceAll(diagramToken, img);
  html = html.replaceAll(sourceToken, escapeHtml(puml.trimEnd()));

  const svgStat = await stat(svgPath);
  report.push({ name, svgKB: (svgStat.size / 1024).toFixed(1), width: size.width, height: size.height });
}

const leftovers = html.match(/\{\{(?:DIAGRAM|SOURCE):[^}]+\}\}/g);
if (leftovers) throw new Error(`Unresolved template tokens: ${leftovers.join(", ")}`);

await writeFile(outputPath, html, "utf8");

const outputStat = await stat(outputPath);
console.log(`Built ${path.relative(repo, outputPath)} (${(outputStat.size / 1024).toFixed(1)} KB)`);
for (const row of report) {
  console.log(`  ${row.name.padEnd(18)} ${String(row.width).padStart(4)}x${String(row.height).padEnd(4)}  ${row.svgKB.padStart(6)} KB SVG`);
}
