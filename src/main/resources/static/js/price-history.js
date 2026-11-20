"use strict";

const priceChart = document.querySelector("[data-price-chart]");
if (priceChart) {
  const plot = priceChart.querySelector("[data-history-plot]");
  const guide = plot.querySelector("[data-history-guide]");
  const picker = priceChart.querySelector("[data-history-observation]");
  const readout = priceChart.querySelector("[data-history-readout]");
  const time = readout.querySelector("[data-history-time]");
  const prices = readout.querySelector("[data-history-prices]");
  const dateFormat = new Intl.DateTimeFormat(document.documentElement.lang, {
    timeZone: "Asia/Hong_Kong", year: "numeric", month: "short", day: "numeric",
    hour: "numeric", minute: "2-digit", second: "2-digit", timeZoneName: "short"
  });
  const series = new Map([...plot.querySelectorAll("[data-history-retailer]")]
    .map(group => [group.dataset.historyRetailer, group]));
  const byDate = new Map();

  for (const row of document.querySelectorAll(".price-history tbody tr[data-history-x]")) {
    const retailer = row.cells[0].textContent.trim();
    if (!series.has(retailer)) continue;
    const date = row.querySelector("time").dateTime;
    if (!byDate.has(date)) {
      byDate.set(date, {
        date, label: dateFormat.format(new Date(date)), x: Number(row.dataset.historyX), rows: [], points: []
      });
    }
    byDate.get(date).rows.push({ retailer, price: row.cells[2].textContent.trim() });
  }
  for (const point of plot.querySelectorAll("[data-observed-at]")) {
    byDate.get(point.dataset.observedAt)?.points.push(point);
  }

  // The table is newest first. The control follows the plot from left to right.
  const observations = [...byDate.values()].reverse();
  let visible = [];
  let selected;

  function showing(retailer) {
    return !series.get(retailer).hasAttribute("hidden");
  }

  function inspect(observation) {
    for (const point of selected?.points ?? []) point.classList.remove("is-selected");
    selected = observation;
    if (!observation) {
      guide.setAttribute("hidden", "");
      time.removeAttribute("datetime");
      time.textContent = readout.dataset.empty;
      prices.replaceChildren();
      return;
    }
    for (const point of observation.points) point.classList.add("is-selected");
    guide.removeAttribute("hidden");
    guide.setAttribute("x1", observation.x);
    guide.setAttribute("x2", observation.x);
    picker.value = observation.date;
    time.dateTime = observation.date;
    time.textContent = observation.label;
    prices.replaceChildren(...observation.rows.filter(row => showing(row.retailer)).map(row => {
      const item = document.createElement("div");
      const label = document.createElement("dt");
      const value = document.createElement("dd");
      label.textContent = row.retailer;
      value.textContent = row.price;
      item.append(label, value);
      return item;
    }));
  }

  function updateSeries() {
    visible = observations.filter(observation => observation.rows.some(row => showing(row.retailer)));
    picker.replaceChildren(...visible.map(observation => new Option(observation.label, observation.date)));
    picker.disabled = visible.length === 0;
    inspect(visible.includes(selected) ? selected : visible.at(-1));
  }

  function inspectPointer(event) {
    if (!visible.length) return;
    const bounds = plot.getBoundingClientRect();
    if (!bounds.width) return;
    const x = (event.clientX - bounds.left) * plot.viewBox.baseVal.width / bounds.width;
    const closest = visible.reduce((best, item) => Math.abs(item.x - x) < Math.abs(best.x - x) ? item : best);
    if (closest !== selected) inspect(closest);
  }

  for (const toggle of priceChart.querySelectorAll("[data-history-series]")) {
    toggle.disabled = false;
    toggle.addEventListener("click", () => {
      const group = series.get(toggle.dataset.historySeries);
      const wasVisible = !group.hasAttribute("hidden");
      group.toggleAttribute("hidden", wasVisible);
      toggle.setAttribute("aria-pressed", String(!wasVisible));
      updateSeries();
    });
  }
  picker.addEventListener("change", () => inspect(byDate.get(picker.value)));
  plot.addEventListener("pointermove", event => { if (event.pointerType !== "touch") inspectPointer(event); });
  plot.addEventListener("pointerdown", inspectPointer);
  updateSeries();
  priceChart.querySelector("[data-history-controls]").hidden = false;
}
