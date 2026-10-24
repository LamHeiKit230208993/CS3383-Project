"use strict";
// Discover, Compare, and List are server-rendered. This file only drives the
// sample scenes (Saved, Updates, Ask) and the settings dialog.
(() => {
  const state = { language: "en" };
  const $ = selector => document.querySelector(selector);
  const $$ = selector => [...document.querySelectorAll(selector)];
  const escapeHtml = value => String(value).replace(/[&<>"']/g, character => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[character]));
  const icon = id => `<svg class="icon" aria-hidden="true"><use href="#i-${id}"/></svg>`;
  const translations = {
    en: { discover: "Discover", list: "My list", saved: "Saved", updates: "Updates", assistant: "Ask", discoverTitle: "Everyday essentials.\nA better way to shop.", listTitle: "Your next shop, sorted.", savedTitle: "Saved for another day.", updatesTitle: "A little heads-up.", assistantTitle: "Ask. Compare. Decide." },
    "zh-Hant": { discover: "探索", list: "購物清單", saved: "收藏", updates: "提醒", assistant: "問助手", discoverTitle: "日常所需，\n買得更清楚。", listTitle: "下次購物，準備好了。", savedTitle: "收藏日常所需。", updatesTitle: "實用的小提醒。", assistantTitle: "提問、比較、決定。" },
    "zh-Hans": { discover: "探索", list: "购物清单", saved: "收藏", updates: "提醒", assistant: "问助手", discoverTitle: "日常所需，\n买得更清楚。", listTitle: "下次购物，准备好了。", savedTitle: "收藏日常所需。", updatesTitle: "实用的小提醒。", assistantTitle: "提问、比较、决定。" }
  };
  const productArt = {
    milk: '<path d="m43 13 30 0 13 21v86H34V34Z" fill="#fff" stroke="#ced8e4"/><path d="m43 13-9 21h52L73 13Z" fill="#d4e3f3"/><path d="M34 56h52v45H34Z" fill="#3b64d5"/><text x="60" y="76" text-anchor="middle" fill="white" font-size="12" font-weight="700">MILK</text>',
    rice: '<path d="M34 17h52l4 103H30Z" fill="#e5d8bc" stroke="#d6c5a7"/><path d="M33 45h54v53H33Z" fill="#42644d"/><text x="60" y="67" text-anchor="middle" fill="#f7f4e9" font-size="11" font-weight="700">RICE</text>',
    eggs: '<path d="m17 63 17-16h54l17 16v35H17Z" fill="#c8bca5" stroke="#b0a48e"/><ellipse cx="35" cy="59" rx="12" ry="19" fill="#faf3e7"/><ellipse cx="60" cy="55" rx="12" ry="20" fill="#f4e7d4"/><ellipse cx="85" cy="59" rx="12" ry="19" fill="#f9eddc"/>',
    oats: '<rect x="29" y="24" width="62" height="94" rx="7" fill="#d0d7bb" stroke="#b0ba95"/><path d="M29 48h62v49H29Z" fill="#f8f5e8"/><text x="60" y="75" text-anchor="middle" fill="#53653d" font-size="13" font-weight="700">OATS</text>',
    oil: '<rect x="48" y="11" width="24" height="17" rx="3" fill="#476b40"/><path d="M45 27h30v14l10 12v65H35V53l10-12Z" fill="#e6c85b" stroke="#cbb145"/><text x="60" y="85" text-anchor="middle" fill="#53653d" font-size="10" font-weight="700">OIL</text>',
    laundry: '<path d="M45 23h31v14l17 15v65H28V52l17-15Z" fill="#94c9bb" stroke="#78b09f"/><rect x="43" y="10" width="35" height="15" rx="4" fill="#3d796f"/>'
  };
  const sample = [
    { art: "milk", brand: "Sample brand", name: "Sample milk", pack: "946 mL · sample" },
    { art: "rice", brand: "Sample brand", name: "Sample rice", pack: "5 kg · sample" },
    { art: "oats", brand: "Sample brand", name: "Sample oats", pack: "500 g · sample" }
  ];
  const art = product => `<svg viewBox="0 0 120 140" aria-hidden="true" focusable="false">${productArt[product.art]}</svg>`;
  const card = product => `<article class="product-card"><div class="product-art" style="--art-bg:#eef2f8">${art(product)}</div><div class="product-details"><p class="product-brand">${escapeHtml(product.brand)}</p><p class="product-name">${escapeHtml(product.name)}</p><p class="pack-label">${escapeHtml(product.pack)}</p></div></article>`;
  let toastTimeout;

  function toast(message) {
    window.clearTimeout(toastTimeout);
    $("#toast").textContent = message;
    $("#toast").classList.add("visible");
    toastTimeout = window.setTimeout(() => { $("#toast").classList.remove("visible"); }, 4000);
  }

  function navigate() {
    const pathScene = window.location.pathname === "/list"
      ? "list"
      : window.location.pathname.startsWith("/compare/") ? "compare" : null;
    const scene = (pathScene || window.location.hash.slice(1) || "discover").split("/")[0];
    const known = ["discover", "list", "compare", "saved", "updates", "assistant"];
    if (!known.includes(scene)) return;
    $$(".scene").forEach(section => { section.hidden = section.id !== `scene-${scene}`; });
    $$("[data-nav]").forEach(link => {
      if (link.dataset.nav === scene) link.setAttribute("aria-current", "page");
      else link.removeAttribute("aria-current");
    });
    const label = $("#breadcrumb-label");
    if (label) label.textContent = translations[state.language][scene] || scene;
  }

  function renderSampleScenes() {
    const saved = $("#saved-products");
    if (saved) saved.innerHTML = sample.map(card).join("");
    const tracked = $("#tracked-products");
    if (tracked) tracked.innerHTML = '<p class="helper">Tracking and older price records are a later baseline job (J05).</p>';
  }

  document.addEventListener("click", event => {
    if (event.target.closest("[data-open-settings]")) { $("#settings-dialog").showModal(); return; }
    const close = event.target.closest("[data-close-dialog]");
    if (close) close.closest("dialog").close();
  });

  const displayLanguage = $("#display-language");
  if (displayLanguage) {
    displayLanguage.addEventListener("change", event => {
      state.language = event.target.value;
      $$("[data-i18n]").forEach(element => { element.textContent = translations[state.language][element.dataset.i18n]; });
      navigate();
      toast("Navigation preview updated. Product labels come from the stored catalogue.");
    });
  }
  const highContrast = $("#high-contrast");
  if (highContrast) highContrast.addEventListener("change", event => { document.documentElement.classList.toggle("high-contrast", event.target.checked); });
  const reset = $("#reset-demo");
  if (reset) reset.addEventListener("click", () => {
    if (window.confirm("This prototype's sample scenes reset on reload. The imported catalogue and your saved list stay.")) window.location.reload();
  });

  $$("[data-chat-prompt]").forEach(button => button.addEventListener("click", () => {
    const feed = $("#chat-feed");
    if (feed) feed.append(icon("chat"));
    toast("The chat helper is not part of the baseline jobs (J09).");
  }));
  const chatForm = $("#chat-form");
  if (chatForm) chatForm.addEventListener("submit", event => {
    event.preventDefault();
    toast("The chat helper is not part of the baseline jobs (J09).");
    $("#chat-question").value = "";
  });

  window.addEventListener("hashchange", navigate);
  renderSampleScenes();
  navigate();
})();
