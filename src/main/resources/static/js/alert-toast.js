"use strict";

(() => {
  const toast = document.querySelector("[data-alert-toast]");
  if (!toast) return;

  const storageKey = "market-desk:shown-alerts";
  const displayed = new Set();
  const title = toast.querySelector("[data-toast-title]");
  const product = toast.querySelector("[data-toast-product]");
  const detail = toast.querySelector("[data-toast-detail]");
  const status = document.querySelector("[data-toast-status]");
  let pending = [];
  let timer;
  let requesting = false;
  try {
    JSON.parse(sessionStorage.getItem(storageKey) || "[]").forEach(id => displayed.add(id));
  } catch {
    // The current page can still deduplicate when browser storage is unavailable.
  }

  function hide() {
    const focused = toast.contains(document.activeElement);
    toast.hidden = true;
    clearTimeout(timer);
    if (focused) document.querySelector("#main-content")?.focus();
    showNext();
  }

  function scheduleDismissal() {
    timer = setTimeout(() => {
      if (document.hidden || toast.matches(":hover, :focus-within")) {
        scheduleDismissal();
      } else {
        hide();
      }
    }, 10000);
  }

  function showNext() {
    if (!toast.hidden || document.hidden || document.querySelector("dialog[open]")) return;
    const event = pending.find(item => !displayed.has(item.id));
    if (!event) return;

    displayed.add(event.id);
    try {
      sessionStorage.setItem(storageKey, JSON.stringify([...displayed].slice(-100)));
    } catch {
      // Leave the page-local record in place.
    }
    toast.dataset.kind = event.reminder ? "reminder" : "price";
    toast.hidden = false;
    title.textContent = event.title;
    product.textContent = event.product;
    detail.textContent = event.detail;
    status.textContent = [event.title, event.product, event.detail].filter(Boolean).join(". ");
    scheduleDismissal();
  }

  async function refresh() {
    if (document.hidden || requesting) return;
    requesting = true;
    try {
      const response = await fetch(toast.dataset.endpoint, { cache: "no-store" });
      if (!response.ok) return;
      pending = await response.json();
      showNext();
    } catch {
      // A temporary connection failure can recover on the next check.
    } finally {
      requesting = false;
    }
  }

  toast.querySelector("[data-toast-dismiss]").addEventListener("click", hide);
  document.addEventListener("visibilitychange", refresh);
  window.addEventListener("pageshow", refresh);
  window.setInterval(refresh, 15000);
  refresh();
})();
