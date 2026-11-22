(() => {
  const editor = ".item-rules";

  function showField(form, selector, visible) {
    const label = form.querySelector(selector);
    if (!label) return;
    const input = label.querySelector("input");
    label.hidden = !visible;
    input.disabled = !visible;
    input.required = visible;
  }

  function adjustCondition(form) {
    const condition = form.elements.condition.value;
    showField(form, "[data-rule-price]", condition !== "time");
    showField(form, "[data-rule-schedule]", condition === "time" || condition === "combined");
  }

  function prepareForms(root) {
    root.querySelectorAll("[data-watch-form]").forEach(adjustCondition);
  }

  prepareForms(document);
  document.addEventListener("change", (event) => {
    if (event.target.matches("[data-rule-condition]")) adjustCondition(event.target.form);
  });

  async function renderEditor(dialog, url, options) {
    const response = await fetch(url, options);
    if (!response.ok) throw new Error("Unable to open alert rules");
    const page = new DOMParser().parseFromString(await response.text(), "text/html");
    const content = page.querySelector(editor);
    if (!content) throw new Error("Alert rules were not present");

    const close = document.createElement("button");
    close.type = "button";
    close.className = "button secondary item-rules-close";
    close.textContent = content.dataset.close;
    close.addEventListener("click", () => dialog.close());

    dialog.replaceChildren(close, ...page.querySelectorAll("#main-content > .notice"), content);
    prepareForms(dialog);
    if (dialog.open) {
      const feedback = dialog.querySelector(".notice");
      if (feedback) feedback.tabIndex = -1;
      (feedback || close).focus();
    }
    return Boolean(dialog.querySelector(".notice:not(.error)"));
  }

  let opening = false;
  document.addEventListener("click", async (event) => {
    const link = event.target.closest("a[data-open-alert-rules]");
    if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey ||
        event.altKey || event.shiftKey || typeof HTMLDialogElement === "undefined" ||
        new URL(link.href).origin !== location.origin) return;
    event.preventDefault();
    if (opening) return;
    opening = true;
    link.setAttribute("aria-busy", "true");

    const dialog = document.createElement("dialog");
    dialog.className = "item-rules-dialog";
    dialog.setAttribute("aria-labelledby", "rules-heading");
    let changed = false;
    let saving = false;
    dialog.addEventListener("close", () => {
      dialog.remove();
      link.focus();
      if (changed && new URLSearchParams(location.search).get("tab") === "rules") location.reload();
    });

    dialog.addEventListener("cancel", (cancel) => {
      if (saving) cancel.preventDefault();
    });
    dialog.addEventListener("submit", async (submit) => {
      const form = submit.target;
      if (form.method.toLowerCase() !== "post" || new URL(form.action).origin !== location.origin) return;
      submit.preventDefault();
      if (saving) return;
      saving = true;
      dialog.setAttribute("aria-busy", "true");
      dialog.querySelector(".item-rules-close").disabled = true;
      try {
        const data = new FormData(form, submit.submitter);
        const updated = await renderEditor(dialog, form.action, {method: "POST", body: new URLSearchParams(data)});
        changed = changed || updated;
      } catch {
        location.assign(link.href);
      } finally {
        saving = false;
        dialog.removeAttribute("aria-busy");
        dialog.querySelector(".item-rules-close").disabled = false;
      }
    });

    try {
      await renderEditor(dialog, link.href);
      document.body.append(dialog);
      dialog.showModal();
    } catch {
      dialog.remove();
      location.assign(link.href);
    } finally {
      opening = false;
      link.removeAttribute("aria-busy");
    }
  });
})();
