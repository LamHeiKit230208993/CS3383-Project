"use strict";

const picker = document.querySelector("[data-cart-picker]");
picker.querySelector("button[type=submit]").hidden = true;
picker.elements.listId.addEventListener("change", () => picker.requestSubmit());

const cartStatus = document.querySelector("[data-cart-status]");
for (const form of document.querySelectorAll("[data-cart-check]")) {
  const checkbox = form.elements.checked;
  const row = form.closest(".list-item");
  form.querySelector("button[type=submit]").hidden = true;
  form.addEventListener("submit", event => event.preventDefault());
  checkbox.addEventListener("click", event => {
    if (checkbox.getAttribute("aria-disabled") === "true") event.preventDefault();
  });
  checkbox.addEventListener("change", async () => {
    const nextState = checkbox.checked;
    const url = new URL(form.action);
    url.searchParams.set("inline", "true");
    checkbox.setAttribute("aria-disabled", "true");
    row.classList.toggle("is-checked", nextState);
    try {
      const response = await fetch(url, {
        method: "POST",
        body: new URLSearchParams({ code: form.elements.code.value, checked: String(nextState) }),
        signal: AbortSignal.timeout(8_000)
      });
      if (response.status !== 204) throw new Error("Cart update failed");
      cartStatus.textContent = cartStatus.dataset.saved;
      cartStatus.classList.add("sr-only");
    } catch {
      checkbox.checked = !nextState;
      row.classList.toggle("is-checked", checkbox.checked);
      cartStatus.textContent = cartStatus.dataset.failed;
      cartStatus.classList.remove("sr-only");
    } finally {
      checkbox.removeAttribute("aria-disabled");
    }
  });
}
