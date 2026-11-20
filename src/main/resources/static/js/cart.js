"use strict";

for (const form of document.querySelectorAll("[data-cart-edit]")) {
  const quantity = form.elements.quantity;
  const retailer = form.elements.retailerCode;
  const steps = [...form.querySelectorAll("[data-cart-step]")];
  const save = form.querySelector("button[type=submit]");
  save.hidden = true;

  function syncSteps() {
    for (const step of steps) {
      const next = quantity.valueAsNumber + Number(step.dataset.cartStep);
      step.disabled = !quantity.validity.valid || next < Number(quantity.min) || next > Number(quantity.max);
    }
  }

  function saveEdit(field) {
    const target = new URL(form.action);
    target.hash = field.id;
    form.action = target.href;
    form.requestSubmit();
  }

  for (const step of steps) {
    step.hidden = false;
    step.addEventListener("click", () => {
      quantity.valueAsNumber += Number(step.dataset.cartStep);
      saveEdit(quantity);
    });
  }
  quantity.addEventListener("input", syncSteps);
  quantity.addEventListener("change", () => saveEdit(quantity));
  retailer.addEventListener("change", () => saveEdit(retailer));
  form.addEventListener("submit", event => {
    if (form.hasAttribute("aria-busy")) event.preventDefault();
    else form.setAttribute("aria-busy", "true");
  });
}

for (const panel of document.querySelectorAll(".cart-files, .cart-remove")) {
  panel.addEventListener("keydown", event => {
    if (event.key !== "Escape" || !panel.open) return;
    panel.open = false;
    panel.querySelector("summary").focus();
    event.preventDefault();
  });
}

const cartStatus = document.querySelector("[data-cart-status]");
for (const form of document.querySelectorAll("[data-cart-check]")) {
  const checkbox = form.elements.checked;
  const row = form.closest(".list-item");
  const locked = row.querySelectorAll("[data-cart-lock]");
  function showDone(done) {
    row.classList.toggle("is-checked", done);
    for (const control of locked) {
      control.inert = done;
      if (done && control instanceof HTMLDetailsElement) control.open = false;
    }
  }
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
    showDone(nextState);
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
      showDone(checkbox.checked);
      cartStatus.textContent = cartStatus.dataset.failed;
      cartStatus.classList.remove("sr-only");
    } finally {
      checkbox.removeAttribute("aria-disabled");
    }
  });
}
