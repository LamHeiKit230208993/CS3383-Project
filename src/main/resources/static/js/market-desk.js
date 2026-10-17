"use strict";
// The UI prototype uses temporary sample state only. No request or storage API is used.
(() => {
  const products = [
    { id: "milk", code: "DEMO-MILK-946", brand: "Daily Dairy", names: { en: "Fresh milk", "zh-Hant": "鮮牛奶", "zh-Hans": "鲜牛奶" }, pack: "946 mL · 1 carton", category: "dairy", art: "milk", color: "#edf3fa", prices: [{ store: "AEON", cents: 2290, offer: "No additional offer" }, { store: "WELLCOME", cents: 2490, offer: "No additional offer" }, { store: "PARKNSHOP", cents: 2550, offer: "Buy 2 to save HK$2.00" }] },
    { id: "rice", code: "DEMO-RICE-5KG", brand: "Good Harvest", names: { en: "Jasmine rice", "zh-Hant": "茉莉香米", "zh-Hans": "茉莉香米" }, pack: "5 kg · 1 bag", category: "pantry", art: "rice", color: "#f5f0e5", prices: [{ store: "WELLCOME", cents: 7200, offer: "No additional offer" }, { store: "AEON", cents: 7600, offer: "No additional offer" }, { store: "PARKNSHOP", cents: 7800, offer: "No additional offer" }] },
    { id: "eggs", code: "DEMO-EGGS-10", brand: "Morning Farm", names: { en: "Free-range eggs", "zh-Hant": "放養雞蛋", "zh-Hans": "放养鸡蛋" }, pack: "10 eggs · 1 box", category: "dairy", art: "eggs", color: "#f6f0e8", prices: [{ store: "PARKNSHOP", cents: 2990, offer: "No additional offer" }, { store: "WELLCOME", cents: 3190, offer: "No additional offer" }, { store: "AEON", cents: 3290, offer: "No additional offer" }] },
    { id: "oats", code: "DEMO-OATS-500", brand: "The Breakfast Co.", names: { en: "Wholegrain oats", "zh-Hant": "全穀燕麥片", "zh-Hans": "全谷燕麦片" }, pack: "500 g · 1 tin", category: "pantry", art: "oats", color: "#edf2e8", prices: [{ store: "WELLCOME", cents: 3490, offer: "No additional offer" }, { store: "AEON", cents: 3490, offer: "No additional offer" }, { store: "PARKNSHOP", cents: 3800, offer: "No additional offer" }] },
    { id: "oil", code: "DEMO-OIL-1L", brand: "Everyday Kitchen", names: { en: "Canola cooking oil", "zh-Hant": "芥花籽油", "zh-Hans": "芥花籽油" }, pack: "1 L · 1 bottle", category: "pantry", art: "oil", color: "#f8f3df", prices: [{ store: "AEON", cents: 4200, offer: "No additional offer" }, { store: "PARKNSHOP", cents: 4500, offer: "No additional offer" }, { store: "WELLCOME", cents: 4590, offer: "No additional offer" }] },
    { id: "laundry", code: "DEMO-WASH-2L", brand: "Clean Days", names: { en: "Laundry liquid", "zh-Hant": "洗衣液", "zh-Hans": "洗衣液" }, pack: "2 L · 1 bottle", category: "household", art: "laundry", color: "#eaf1f0", prices: [{ store: "WATSONS", cents: 6500, offer: "No additional offer" }, { store: "PARKNSHOP", cents: 7000, offer: "No additional offer" }, { store: "WELLCOME", cents: null, offer: "No sample listing available" }] }
  ];
  const categories = { dairy: "Dairy & eggs", pantry: "Pantry", household: "Household" };
  const translations = {
    en: { discover: "Discover", list: "My list", saved: "Saved", updates: "Updates", assistant: "Ask", discoverTitle: "Everyday essentials.\nA better way to shop.", listTitle: "Your next shop, sorted.", savedTitle: "Saved for another day.", updatesTitle: "A little heads-up.", assistantTitle: "Ask. Compare. Decide." },
    "zh-Hant": { discover: "探索", list: "購物清單", saved: "收藏", updates: "提醒", assistant: "問助手", discoverTitle: "日常所需，\n買得更清楚。", listTitle: "下次購物，準備好了。", savedTitle: "收藏日常所需。", updatesTitle: "實用的小提醒。", assistantTitle: "提問、比較、決定。" },
    "zh-Hans": { discover: "探索", list: "购物清单", saved: "收藏", updates: "提醒", assistant: "问助手", discoverTitle: "日常所需，\n买得更清楚。", listTitle: "下次购物，准备好了。", savedTitle: "收藏日常所需。", updatesTitle: "实用的小提醒。", assistantTitle: "提问、比较、决定。" }
  };
  const state = {
    language: "en", category: "all", query: "", selected: "milk", quantityProduct: "milk",
    wishlist: new Set(["milk", "oats"]), tracked: new Set(["milk"]),
    list: [{ id: "milk", quantity: 2, checked: false }, { id: "rice", quantity: 1, checked: false }, { id: "eggs", quantity: 1, checked: false }]
  };
  const $ = selector => document.querySelector(selector);
  const $$ = selector => [...document.querySelectorAll(selector)];
  const productById = id => products.find(product => product.id === id);
  const name = product => product.names[state.language];
  const money = cents => new Intl.NumberFormat(state.language === "en" ? "en-US" : state.language, { style: "currency", currency: "HKD" }).format(cents / 100);
  const date = day => new Intl.DateTimeFormat(state.language === "en" ? "en-GB" : state.language, { day: "numeric", month: "short", timeZone: "Asia/Hong_Kong" }).format(new Date(`2026-10-${String(day).padStart(2, "0")}T12:00:00+08:00`));
  const escapeHtml = value => String(value).replace(/[&<>"']/g, character => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[character]));
  const icon = id => `<svg class="icon" aria-hidden="true"><use href="#i-${id}"/></svg>`;
  const validPrices = product => product.prices.filter(price => price.cents !== null).sort((a, b) => a.cents - b.cents);
  const lowest = product => validPrices(product)[0];
  let toastTimeout;

  // These drawings are generic demo packs, not retailer or product brand assets.
  function art(product) {
    const shapes = {
      milk: '<path d="m43 13 30 0 13 21v86H34V34Z" fill="#fff" stroke="#ced8e4"/><path d="m43 13-9 21h52L73 13Z" fill="#d4e3f3"/><path d="M43 13h30v8H43Z" fill="#2854ce"/><path d="M34 56h52v45H34Z" fill="#3b64d5"/><path d="M48 44h24M50 47h20" stroke="#9cabbc" stroke-width="2"/><text x="60" y="76" text-anchor="middle" fill="white" font-size="14" font-weight="700">MILK</text><text x="60" y="91" text-anchor="middle" fill="#e3ecff" font-size="7">FRESH · 946 mL</text><path d="m63 30 8-7 8 11v12H63Z" fill="#fff"/><circle cx="72" cy="36" r="3" fill="#d0dcec"/>',
      rice: '<path d="M34 17h52l4 103H30Z" fill="#e5d8bc" stroke="#d6c5a7"/><path d="M35 17h50v10H35Z" fill="#a78c5c"/><path d="M33 45h54v53H33Z" fill="#42644d"/><text x="60" y="67" text-anchor="middle" fill="#f7f4e9" font-size="13" font-weight="700">JASMINE</text><text x="60" y="81" text-anchor="middle" fill="#e4ecd9" font-size="8">GOOD HARVEST</text><path d="M49 91c6-4 16-4 22 0M43 108h34" fill="none" stroke="#f4eddc" stroke-width="2"/><text x="60" y="118" text-anchor="middle" fill="#594a32" font-size="7">5 kg</text>',
      eggs: '<path d="m17 63 17-16h54l17 16v35H17Z" fill="#c8bca5" stroke="#b0a48e"/><path d="M17 65h88v29H17Z" fill="#dbd2bf"/><ellipse cx="35" cy="59" rx="12" ry="19" fill="#faf3e7"/><ellipse cx="60" cy="55" rx="12" ry="20" fill="#f4e7d4"/><ellipse cx="85" cy="59" rx="12" ry="19" fill="#f9eddc"/><path d="M19 76h84v20H19Z" fill="#71875a"/><text x="61" y="89" text-anchor="middle" fill="white" font-size="10" font-weight="700">MORNING FARM</text><path d="M28 100h63" stroke="#b0a48e" stroke-width="2"/>',
      oats: '<rect x="29" y="24" width="62" height="94" rx="7" fill="#d0d7bb" stroke="#b0ba95"/><ellipse cx="60" cy="24" rx="31" ry="7" fill="#dfe5d1" stroke="#b0ba95"/><path d="M29 48h62v49H29Z" fill="#f8f5e8"/><text x="60" y="69" text-anchor="middle" fill="#53653d" font-size="15" font-weight="700">OATS</text><text x="60" y="82" text-anchor="middle" fill="#667752" font-size="7">WHOLEGRAIN</text><path d="m50 106 10-9 10 9M60 96v16" fill="none" stroke="#8f9e6a" stroke-width="2"/>',
      oil: '<rect x="48" y="11" width="24" height="17" rx="3" fill="#476b40"/><path d="M45 27h30v14l10 12v65H35V53l10-12Z" fill="#e6c85b" stroke="#cbb145"/><path d="M35 65h50v37H35Z" fill="#f9f6e6"/><text x="60" y="81" text-anchor="middle" fill="#53653d" font-size="11" font-weight="700">CANOLA</text><text x="60" y="94" text-anchor="middle" fill="#727753" font-size="7">COOKING OIL</text><path d="M43 49v12M43 109v7" stroke="#f6e4a0" stroke-width="4" stroke-linecap="round"/>',
      laundry: '<path d="M45 23h31v14l17 15v65H28V52l17-15Z" fill="#94c9bb" stroke="#78b09f"/><rect x="43" y="10" width="35" height="15" rx="4" fill="#3d796f"/><path d="M75 43c17 5 17 28 0 31V63c5-5 5-10 0-12Z" fill="#eaf1f0" stroke="#78b09f"/><path d="M30 65h44v35H30Z" fill="#f8fbf8"/><text x="52" y="80" text-anchor="middle" fill="#386c62" font-size="10" font-weight="700">CLEAN</text><text x="52" y="91" text-anchor="middle" fill="#386c62" font-size="10">DAYS</text><path d="M34 108h40" stroke="#bde0d1" stroke-width="3"/>'
    };
    return `<svg viewBox="0 0 120 140" aria-hidden="true" focusable="false">${shapes[product.art]}</svg>`;
  }

  function productCard(product) {
    const price = lowest(product);
    return `<article class="product-card"><div class="product-art" style="--art-bg:${product.color}">${art(product)}<button type="button" class="save-button" data-action="save" data-product="${product.id}" aria-label="Save ${escapeHtml(name(product))} to wishlist" aria-pressed="${state.wishlist.has(product.id)}">${icon("heart")}</button></div><div class="product-details"><p class="product-brand" translate="no">${product.brand}</p><a class="product-name" href="#compare/${product.id}">${escapeHtml(name(product))}</a><p class="pack-label">${product.pack}</p><div class="product-action-row"><div><span class="price-label">Lowest sample price</span><strong class="product-price">${money(price.cents)}</strong><p class="retailer-label" translate="no">at ${price.store}</p></div><button class="add-button" type="button" data-action="add" data-product="${product.id}" aria-label="Add ${escapeHtml(name(product))} to sample list">${icon("plus")}</button></div></div></article>`;
  }

  function renderProducts() {
    const query = state.query.trim().toLocaleLowerCase();
    const filtered = products.filter(product => (state.category === "all" || product.category === state.category) && [product.brand, categories[product.category], product.pack, ...Object.values(product.names)].join(" ").toLocaleLowerCase().includes(query));
    $("#product-grid").innerHTML = filtered.map(productCard).join("");
    $("#result-count").textContent = `${filtered.length} sample product${filtered.length === 1 ? "" : "s"}`;
    $("#search-empty").hidden = filtered.length > 0;
    $$("[data-category]").forEach(button => {
      const active = button.dataset.category === state.category;
      button.classList.toggle("active", active);
      button.setAttribute("aria-pressed", String(active));
    });
  }

  function renderBasket() {
    let total = 0;
    const categoryTotals = {};
    state.list.forEach(item => {
      const product = productById(item.id);
      const amount = lowest(product).cents * item.quantity;
      total += amount;
      categoryTotals[product.category] = (categoryTotals[product.category] || 0) + amount;
    });
    $$("[data-basket-total]").forEach(element => { element.textContent = money(total); });
    $$("[data-list-count]").forEach(element => { element.textContent = state.list.length; });
    $("#list-progress").textContent = `${state.list.length} products · ${state.list.filter(item => item.checked).length} checked`;
    $("#preview-budget").value = Math.min(total / 100, 250);
    $("#budget-remaining").textContent = total <= 25000 ? `${money(25000 - total)} left in your plan` : `${money(total - 25000)} above the sample budget`;
    $("#basket-preview-items").innerHTML = state.list.slice(0, 3).map(item => {
      const product = productById(item.id);
      return `<div class="preview-item"><div class="preview-item-art" style="--art-bg:${product.color}">${art(product)}</div><div class="preview-item-text">${escapeHtml(name(product))}<small>${item.quantity} ${item.quantity === 1 ? "pack" : "packs"} · ${lowest(product).store}</small></div><strong>${money(lowest(product).cents * item.quantity)}</strong></div>`;
    }).join("") || '<p class="helper">Your sample list is empty. Add an item to start planning.</p>';
    $("#shopping-list").innerHTML = state.list.map(item => {
      const product = productById(item.id);
      const price = lowest(product);
      return `<div class="shopping-row ${item.checked ? "is-checked" : ""}"><label class="check-label"><input type="checkbox" name="checked-${product.id}" data-check-product="${product.id}" ${item.checked ? "checked" : ""}><span><span class="list-product-name">${escapeHtml(name(product))}</span><span class="list-product-detail">${product.pack} · ${item.quantity} ${item.quantity === 1 ? "pack" : "packs"}</span></span></label><div class="row-price">${money(price.cents * item.quantity)}<small>${price.store}</small></div><button class="icon-button" type="button" data-action="edit" data-product="${product.id}" aria-label="Edit quantity for ${escapeHtml(name(product))}">${icon("settings")}</button></div>`;
    }).join("") || '<div class="empty-state"><h3>A fresh start for your next shop.</h3><p>Add a sample product to see your list here.</p><a class="button primary" href="#discover">Discover sample products</a></div>';
    $("#category-summary").innerHTML = Object.entries(categoryTotals).map(([category, amount]) => `<div class="category-cost"><span>${categories[category]}</span><strong>${money(amount)}</strong></div>`).join("") || '<p class="helper">Add an item to see category totals.</p>';
  }

  function renderSaved() {
    const saved = products.filter(product => state.wishlist.has(product.id));
    $("#saved-products").innerHTML = saved.map(productCard).join("") || '<div class="empty-state"><h2>No saved items yet</h2><p>Use the heart on a sample product to keep it here.</p><a class="button secondary" href="#discover">Browse products</a></div>';
    // Include an untracked saved item so the separate tracking action can be evaluated.
    const trackingChoices = products.filter(product => state.tracked.has(product.id) || state.wishlist.has(product.id));
    $("#tracked-products").innerHTML = trackingChoices.map(product => `<div class="tracking-row"><div><strong>${escapeHtml(name(product))}</strong><small>${product.pack} · ${state.tracked.has(product.id) ? "Tracking enabled in demo" : "Saved only · not tracking"}</small></div><button class="button secondary" type="button" data-action="track" data-product="${product.id}" aria-pressed="${state.tracked.has(product.id)}">${state.tracked.has(product.id) ? "Stop tracking" : "Start tracking"}</button></div>`).join("") || '<p class="helper">Save a product to preview its separate tracking choice.</p>';
  }

  function renderComparison() {
    const product = productById(state.selected);
    const prices = validPrices(product);
    const best = prices[0].cents;
    const ordered = [...prices, ...product.prices.filter(price => price.cents === null)];
    const next = prices.find(price => price.cents > best);
    $("#comparison-content").innerHTML = `<div class="compare-layout"><div class="panel compare-product"><div class="product-art" style="--art-bg:${product.color}">${art(product)}</div><p class="product-brand" translate="no">${product.brand}</p><h2>${escapeHtml(name(product))}</h2><p class="helper">${product.pack}<br><span translate="no">${product.code}</span> · sample code</p><button class="button primary full" type="button" data-action="add" data-product="${product.id}">${icon("plus")}Add to list</button><button type="button" class="text-button" data-action="save" data-product="${product.id}" aria-pressed="${state.wishlist.has(product.id)}">${state.wishlist.has(product.id) ? "Saved to wishlist" : "Save to wishlist"}</button></div><div class="panel"><div class="section-heading"><h2>Same pack. Different retailers.</h2><span class="small-tag">Sample prices</span></div><table class="comparison-table"><caption class="sr-only">Sample retailer prices for ${escapeHtml(name(product))}, ${product.pack}</caption><thead><tr><th scope="col">Retailer</th><th scope="col">Offer conditions</th><th scope="col">Listed price</th></tr></thead><tbody>${ordered.map(price => `<tr class="${price.cents === best ? "best-row" : ""}"><th scope="row"><span class="store-name"><span class="store-monogram" aria-hidden="true">${price.store.slice(0, 2)}</span><span translate="no">${price.store}</span></span>${price.cents === best ? '<span class="small-tag">Lowest listed</span>' : ""}</th><td class="offer-note">${price.offer}</td><td class="comparison-price">${price.cents === null ? '<span class="offer-note">No listing</span>' : money(price.cents)}</td></tr>`).join("")}</tbody></table><div class="price-difference">${next ? `<strong>${money(next.cents - best)}</strong> below the next higher sample listing.` : "The available sample prices are tied."} Delivery and stock are not included.</div><p class="helper">Made-up capture date: ${date(7)} 2026. This is not a live price check. Offer amounts are shown separately from single-pack listed prices.</p></div></div>`;
    renderHistory();
  }

  function renderHistory() {
    if ($("#history-period").value === "empty") {
      $("#history-content").innerHTML = '<div class="empty-state"><h3>Not enough history yet</h3><p>Show the dates actually recorded. Do not invent missing days or predict a future price.</p></div>';
      return;
    }
    const product = productById(state.selected);
    const retailer = lowest(product).store;
    const current = lowest(product).cents;
    const values = [current + 300, current + 200, current + 200, current + 350, current + 100, current + 100, current];
    const min = Math.min(...values) - 80;
    const max = Math.max(...values) + 80;
    const points = values.map((value, index) => `${30 + index * 120},${140 - ((value - min) / (max - min)) * 110}`);
    $("#history-content").innerHTML = `<p class="helper">${escapeHtml(name(product))} · ${product.pack} · ${retailer}. Seven made-up capture dates; one exact product and retailer.</p><svg class="history-chart" viewBox="0 0 780 165" role="img" aria-labelledby="history-chart-title"><title id="history-chart-title">Sample listed prices for ${escapeHtml(name(product))} at ${retailer}; values are also in the table below.</title><path class="chart-grid" d="M30 40H750M30 90H750M30 140H750"/><polyline class="chart-line" points="${points.join(" ")}"/>${points.map(point => { const [x, y] = point.split(","); return `<circle class="chart-point" cx="${x}" cy="${y}" r="5"/>`; }).join("")}</svg><div class="chart-axis"><span>${date(1)}</span><span>${date(4)}</span><span>${date(7)}</span></div><p class="helper">The final sample price is ${money(values[0] - current)} below the first recorded value. This does not predict the next price.</p><details class="history-values"><summary>Read the sample chart values</summary><table><thead><tr><th scope="col">Sample capture date</th><th scope="col">Listed price</th></tr></thead><tbody>${values.map((value, index) => `<tr><td>${date(index + 1)} 2026</td><td>${money(value)}</td></tr>`).join("")}</tbody></table></details>`;
  }

  function renderAll() {
    renderProducts(); renderBasket(); renderSaved(); renderComparison();
  }

  function toast(message) {
    window.clearTimeout(toastTimeout);
    $("#toast").textContent = message;
    $("#toast").classList.add("visible");
    toastTimeout = window.setTimeout(() => { $("#toast").classList.remove("visible"); }, 4000);
  }

  function navigate(focusHeading = true) {
    const [requested, productId] = window.location.hash.slice(1).split("/");
    const scene = ["discover", "compare", "list", "saved", "updates", "assistant"].includes(requested) ? requested : "discover";
    if (scene === "compare") {
      if (!productById(productId)) {
        window.location.replace("#discover");
        return;
      }
      state.selected = productId;
      renderComparison();
    }
    $$(".scene").forEach(section => { section.hidden = section.id !== `scene-${scene}`; });
    $$("[data-nav]").forEach(link => {
      if (link.dataset.nav === (scene === "compare" ? "discover" : scene)) link.setAttribute("aria-current", "page");
      else link.removeAttribute("aria-current");
    });
    $("#breadcrumb-label").textContent = scene === "compare" ? "Product comparison" : translations[state.language][scene];
    document.title = `${scene === "compare" ? "Compare" : translations[state.language][scene]} · Market Desk`;
    if (focusHeading) {
      window.scrollTo({ top: 0, behavior: "instant" });
      $(`#scene-${scene} h1`).focus({ preventScroll: true });
    }
  }

  function openQuantity(id, editing) {
    const product = productById(id);
    if (!product) return;
    state.quantityProduct = id;
    $("#quantity-form").dataset.editing = String(editing);
    $("#quantity-heading").textContent = editing ? "Change quantity" : "Add an item";
    $("#quantity-form button[type=submit]").innerHTML = `${editing ? "Update sample quantity" : "Add to sample list"}${icon("plus")}`;
    $("#item-quantity").value = editing ? state.list.find(item => item.id === id).quantity : 1;
    $("#item-quantity").setCustomValidity("");
    $("#quantity-product").innerHTML = `<div class="preview-item-art" style="--art-bg:${product.color}">${art(product)}</div><div><h3>${escapeHtml(name(product))}</h3><p>${product.pack}</p><p>${money(lowest(product).cents)} per pack · sample</p></div>`;
    $("#quantity-dialog").showModal();
  }

  document.addEventListener("click", event => {
    const settings = event.target.closest("[data-open-settings]");
    if (settings) { $("#settings-dialog").showModal(); return; }
    const close = event.target.closest("[data-close-dialog]");
    if (close) { close.closest("dialog").close(); return; }
    const category = event.target.closest("[data-category]");
    if (category) { state.category = category.dataset.category; renderProducts(); return; }
    const action = event.target.closest("[data-action]");
    if (!action) return;
    const id = action.dataset.product;
    const product = productById(id);
    if (!product) return;
    if (action.dataset.action === "add" || action.dataset.action === "edit") {
      openQuantity(id, action.dataset.action === "edit");
    } else if (action.dataset.action === "save") {
      const fromSaved = Boolean(action.closest("#scene-saved"));
      if (state.wishlist.has(id)) state.wishlist.delete(id); else state.wishlist.add(id);
      const pressed = state.wishlist.has(id);
      // Keep the activating element in place so keyboard focus is not lost.
      $$(`[data-action=save][data-product=${id}]`).forEach(button => {
        button.setAttribute("aria-pressed", String(pressed));
        if (!button.classList.contains("save-button")) button.textContent = pressed ? "Saved to wishlist" : "Save to wishlist";
      });
      renderSaved();
      if (fromSaved) $("#saved-heading").focus();
      toast(pressed ? "Saved in the demo wishlist. Tracking stays separate." : "Removed from the demo wishlist.");
    } else if (action.dataset.action === "track") {
      if (state.tracked.has(id)) state.tracked.delete(id); else state.tracked.add(id);
      renderSaved();
      ($(`[data-action=track][data-product=${id}]`) || $("#saved-heading")).focus();
      toast(state.tracked.has(id) ? "Sample tracking is on. No price collection is running." : "Sample tracking is off.");
    }
  });

  $("#search-form").addEventListener("submit", event => { event.preventDefault(); state.query = $("#product-search").value; renderProducts(); });
  $("#product-search").addEventListener("input", event => { state.query = event.target.value; renderProducts(); });
  $("#clear-search").addEventListener("click", () => { state.query = ""; state.category = "all"; $("#product-search").value = ""; renderProducts(); $("#product-search").focus(); });
  $("#shopping-list").addEventListener("change", event => {
    if (!event.target.matches("[data-check-product]")) return;
    const id = event.target.dataset.checkProduct;
    state.list.find(item => item.id === id).checked = event.target.checked;
    renderBasket();
    $(`[data-check-product=${id}]`).focus();
  });
  $("#quantity-form").addEventListener("submit", event => {
    event.preventDefault();
    const quantity = Number($("#item-quantity").value);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 12) { $("#item-quantity").reportValidity(); return; }
    const existing = state.list.find(item => item.id === state.quantityProduct);
    const editing = $("#quantity-form").dataset.editing === "true";
    if (existing && !editing && existing.quantity + quantity > 12) {
      $("#item-quantity").setCustomValidity(`This demo allows 12 packs per product. You already have ${existing.quantity}; edit the list quantity to change it.`);
      $("#item-quantity").reportValidity();
      return;
    }
    if (existing) existing.quantity = editing ? quantity : existing.quantity + quantity;
    else state.list.push({ id: state.quantityProduct, quantity, checked: false });
    $("#quantity-dialog").close();
    renderBasket();
    if (editing) $(`[data-action=edit][data-product=${state.quantityProduct}]`).focus();
    toast(`${name(productById(state.quantityProduct))} updated in your sample list.`);
  });
  $("#item-quantity").addEventListener("input", event => event.target.setCustomValidity(""));
  $("#history-period").addEventListener("change", renderHistory);
  window.addEventListener("hashchange", () => navigate());
  $("#display-language").addEventListener("change", event => {
    state.language = event.target.value;
    // The language selector previews selected labels; the remaining UI is English.
    $$("[data-i18n]").forEach(element => { element.textContent = translations[state.language][element.dataset.i18n]; });
    renderAll(); navigate(false);
    toast("Navigation and product-name preview updated. Other prototype text remains English.");
  });
  $("#high-contrast").addEventListener("change", event => { document.documentElement.classList.toggle("high-contrast", event.target.checked); });
  $("#reset-demo").addEventListener("click", () => { if (window.confirm("Reset the temporary demo selections? Nothing has been saved outside this page.")) window.location.reload(); });
  $("#alert-kind").addEventListener("change", event => {
    const drop = event.target.value === "drop";
    $("#alert-value-label").firstChild.textContent = drop ? "Drop amount (HKD)" : "Target price (HKD)";
    $("#alert-value").value = drop ? "2.00" : "24.00";
    $("#alert-rule-help").textContent = drop ? "A price drop needs an earlier price for the same product, pack, and retailer. This preview uses a made-up earlier value." : "A target check needs one current price. This rule is a sample UI choice.";
    $("#alert-status").textContent = "";
  });
  $("#alert-form").addEventListener("submit", event => {
    event.preventDefault();
    const product = productById($("#alert-product").value);
    const amount = Math.round(Number($("#alert-value").value) * 100);
    const target = $("#alert-kind").value === "target";
    const met = target ? lowest(product).cents <= amount : 300 >= amount;
    $("#alert-status").textContent = `Sample ${target ? "target" : "drop"} ${met ? "met" : "not met"}. ${target ? `Current sample price: ${money(lowest(product).cents)}.` : `Made-up earlier price: ${money(lowest(product).cents + 300)}; current: ${money(lowest(product).cents)}.`} No alert was delivered.`;
  });
  $("#reminder-form").addEventListener("submit", event => { event.preventDefault(); $("#reminder-status").textContent = `Sample reminder set after ${$("#reminder-days").value} days for ${name(productById($("#reminder-product").value))}. No background reminder is running.`; });
  $("#reminder-add").addEventListener("click", () => openQuantity($("#reminder-product").value, false));
  $("#reminder-snooze").addEventListener("click", () => { $("#reminder-status").textContent = "Sample reminder snoozed for 7 days. No background reminder is running."; });

  function sendSampleQuestion(question) {
    const user = document.createElement("div");
    user.className = "chat-message user-message";
    const userLabel = document.createElement("span"); userLabel.className = "chat-label"; userLabel.textContent = "You · demo";
    const text = document.createElement("p"); text.textContent = question;
    user.append(userLabel, text); $("#chat-feed").append(user);
    const answer = document.createElement("div"); answer.className = "chat-message answer-message";
    const answerLabel = document.createElement("span"); answerLabel.className = "chat-label"; answerLabel.textContent = "Scripted demo · no AI connected";
    const body = document.createElement("p");
    const lowerQuestion = question.toLocaleLowerCase();
    const product = products.find(item => Object.values(item.names).some(label => lowerQuestion.includes(label.toLocaleLowerCase())) || lowerQuestion.includes(item.id));
    if (lowerQuestion.includes("track")) body.textContent = "Saving bookmarks an item. Tracking is a separate choice for collecting that exact product’s history in the later baseline. This demo does not collect prices.";
    else if (product) body.textContent = `${name(product)} (${product.pack}) has a lowest sample listed price of ${money(lowest(product).cents)} at ${lowest(product).store}. These are made-up demo values. No live request was made.`;
    else body.textContent = "Which sample product do you mean? Try milk, rice, eggs, oats, oil, or laundry liquid. Product and pack need to be clear before comparing prices.";
    answer.append(answerLabel, body); $("#chat-feed").append(answer);
    $("#chat-feed").scrollTop = $("#chat-feed").scrollHeight;
  }
  $("#chat-form").addEventListener("submit", event => { event.preventDefault(); const question = $("#chat-question").value.trim(); if (!question) return; sendSampleQuestion(question); $("#chat-question").value = ""; $("#chat-question").focus(); });
  $$("[data-chat-prompt]").forEach(button => button.addEventListener("click", () => sendSampleQuestion(button.dataset.chatPrompt)));
  renderAll();
  navigate(false);
})();
