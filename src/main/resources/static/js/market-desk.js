"use strict";
// Forms work without JavaScript; this only prevents repeated import clicks.
document.querySelectorAll("[data-import-form]").forEach(form => {
  form.addEventListener("submit", () => {
    const button = form.querySelector("button[type=submit]");
    button.dataset.label = button.textContent;
    button.disabled = true;
    button.textContent = "Importing…";
    form.setAttribute("aria-busy", "true");
  });
});
window.addEventListener("pageshow", () => {
  document.querySelectorAll("[data-import-form]").forEach(form => {
    form.removeAttribute("aria-busy");
    const button = form.querySelector("button[type=submit]");
    button.disabled = false;
    if (button.dataset.label) button.textContent = button.dataset.label;
  });
});


const estimateForm = document.querySelector("[data-estimate-form]");
if (estimateForm) {
  const quantity = estimateForm.querySelector("[name=quantity]");
  const radios = document.querySelectorAll('input[form="add-item-form"][name=retailerCode]');
  // Integer ten-thousandths keep the preview exact, including large supported prices.
  function refreshEstimate() {
    const selected = [...radios].find(radio => radio.checked && !radio.disabled);
    const store = estimateForm.querySelector("[data-selected-store]");
    const amount = estimateForm.querySelector("[data-listed-estimate]");
    if (![...radios].some(radio => !radio.disabled)) {
      store.textContent = "No priced retailer listing. You can keep this item with an unknown estimate.";
      amount.textContent = "Unknown";
      return;
    }
    if (!selected || !quantity.validity.valid || !/^\d+$/.test(quantity.value)) {
      store.textContent = selected ? "Enter a whole quantity from 1 to 999." : "Choose a retailer in the comparison.";
      amount.textContent = "—";
      return;
    }
    const [whole, fractional = ""] = selected.dataset.price.split(".");
    const unit = BigInt(whole) * 10000n + BigInt(fractional.padEnd(4, "0"));
    const total = unit * BigInt(quantity.value);
    const fraction = String(total % 10000n).padStart(4, "0").replace(/0+$/, "").padEnd(2, "0");
    const integer = new Intl.NumberFormat("en-HK", { style: "currency", currency: "HKD", maximumFractionDigits: 0 }).format(total / 10000n);
    store.textContent = selected.value + " · " + quantity.value + " packs";
    amount.textContent = integer + "." + fraction;
  }
  quantity.addEventListener("input", refreshEstimate);
  radios.forEach(radio => radio.addEventListener("change", refreshEstimate));
  refreshEstimate();
}
const categoryFilter = document.querySelector("#category-filter");
if (categoryFilter) categoryFilter.addEventListener("change", () => categoryFilter.form.requestSubmit());
