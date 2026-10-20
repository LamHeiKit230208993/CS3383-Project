(function () {
    "use strict";

    const $ = (selector, root) => (root || document).querySelector(selector);
    const $$ = (selector, root) => Array.from((root || document).querySelectorAll(selector));

    let toastTimer = null;

    function showToast(message) {
        const toast = $("#toast");
        if (!toast) {
            return;
        }
        toast.textContent = message;
        toast.classList.add("is-visible");
        window.clearTimeout(toastTimer);
        toastTimer = window.setTimeout(() => toast.classList.remove("is-visible"), 2600);
    }

    /* ---------- Search filter ---------- */

    const searchInput = $("#product-search");
    const resultCount = $("#result-count");
    const noResults = $("#no-results");
    const categoryButtons = $$(".filter[data-filter]");
    let activeCategory = "All";

    function applySearch() {
        const term = (searchInput && searchInput.value ? searchInput.value : "").trim().toLowerCase();
        let visible = 0;

        $$("#product-results .product-card").forEach((card) => {
            const name = (card.getAttribute("data-name") || "").toLowerCase();
            const category = card.getAttribute("data-category") || "";
            const matchesTerm = !term || name.indexOf(term) !== -1;
            const matchesCategory = activeCategory === "All" || category === activeCategory;
            const show = matchesTerm && matchesCategory;
            card.hidden = !show;
            if (show) {
                visible += 1;
            }
        });

        if (noResults) {
            noResults.hidden = visible !== 0;
        }
        if (resultCount) {
            resultCount.textContent = String(visible);
        }
    }

    if (searchInput) {
        searchInput.addEventListener("input", applySearch);
    }

    categoryButtons.forEach((button) => {
        button.addEventListener("click", () => {
            activeCategory = button.getAttribute("data-filter") || "All";
            categoryButtons.forEach((item) => item.classList.toggle("is-active", item === button));
            applySearch();
        });
    });

    const clearSearch = $("#clear-search");
    if (clearSearch) {
        clearSearch.addEventListener("click", () => {
            if (searchInput) {
                searchInput.value = "";
                searchInput.focus();
            }
            activeCategory = "All";
            categoryButtons.forEach((item) => {
                item.classList.toggle("is-active", item.getAttribute("data-filter") === "All");
            });
            applySearch();
        });
    }

    /* ---------- Quantity dialog ---------- */

    const quantityDialog = $("#quantity-dialog");
    const openQuantity = $("#open-quantity");

    if (openQuantity && quantityDialog && typeof quantityDialog.showModal === "function") {
        openQuantity.addEventListener("click", () => {
            quantityDialog.showModal();
            const quantity = $("#item-quantity");
            if (quantity) {
                quantity.focus();
                quantity.select();
            }
        });
    }

    const quantityForm = $("#quantity-form");
    if (quantityForm) {
        quantityForm.addEventListener("submit", (event) => {
            const submitter = event.submitter;
            if (submitter && submitter.value === "cancel") {
                return;
            }
            const quantity = $("#item-quantity");
            const value = quantity ? quantity.value : "1";
            showToast("Added " + value + " sample item(s) to the demo list.");
        });
    }

    /* ---------- Shopping list totals ---------- */

    const listTotal = $("#list-total");
    const summaryTotal = $("#summary-total");
    const budgetFill = $("#budget-fill");
    const budgetCaption = $(".budget-caption span");

    function recalculateList() {
        const rows = $$(".list-item");
        if (!rows.length) {
            return;
        }

        let total = 0;
        rows.forEach((row) => {
            const price = Number.parseFloat(row.getAttribute("data-price") || "0");
            const quantity = Number.parseInt(row.getAttribute("data-quantity") || "1", 10);
            total += price * quantity;

            const checkbox = row.querySelector('input[type="checkbox"]');
            row.classList.toggle("is-checked", Boolean(checkbox && checkbox.checked));
        });

        const formatted = "$" + total.toFixed(2);
        if (listTotal) {
            listTotal.textContent = formatted;
        }
        if (summaryTotal) {
            summaryTotal.textContent = formatted;
        }

        const percent = Math.min(100, Math.round((total / 600) * 100));
        if (budgetFill) {
            budgetFill.style.width = percent + "%";
        }
        if (budgetCaption) {
            budgetCaption.textContent = percent + "% of planned budget";
        }
    }

    $$(".list-item input[type='checkbox']").forEach((checkbox) => {
        checkbox.addEventListener("change", recalculateList);
    });

    /* ---------- High contrast ---------- */

    const contrastToggle = $("#contrast-toggle");
    if (contrastToggle) {
        contrastToggle.addEventListener("change", () => {
            document.documentElement.classList.toggle("high-contrast", contrastToggle.checked);
            showToast(contrastToggle.checked ? "High-contrast preview on." : "High-contrast preview off.");
        });
    }

    /* ---------- Language preview ---------- */

    const translations = {
        en: {
            name: "English",
            searchLabel: "Search products",
            listLabel: "Shopping list",
            bestPriceLabel: "Best listed price"
        },
        "zh-Hant": {
            name: "繁體中文",
            searchLabel: "搜尋產品",
            listLabel: "購物清單",
            bestPriceLabel: "最低標價"
        },
        "zh-Hans": {
            name: "简体中文",
            searchLabel: "搜索产品",
            listLabel: "购物清单",
            bestPriceLabel: "最低标价"
        }
    };

    const languageButtons = $$(".filter[data-lang]");
    languageButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const language = button.getAttribute("data-lang") || "en";
            const dictionary = translations[language] || translations.en;

            $$("[data-i18n]").forEach((element) => {
                const key = element.getAttribute("data-i18n");
                if (key && dictionary[key]) {
                    element.textContent = dictionary[key];
                }
            });

            const languageName = $("#language-name");
            if (languageName) {
                languageName.textContent = dictionary.name;
            }

            languageButtons.forEach((item) => item.classList.toggle("is-active", item === button));
            showToast("Language preview: " + dictionary.name);
        });
    });

    /* ---------- Assistant demo ---------- */

    const chatForm = $("#chat-form");
    const chatInput = $("#chat-input");
    const chatFeed = $("#chat-feed");

    function appendChat(role, text) {
        if (!chatFeed) {
            return;
        }

        const row = document.createElement("div");
        row.className = "chat-row " + (role === "user" ? "chat-user" : "chat-assistant");

        if (role !== "user") {
            const avatar = document.createElement("div");
            avatar.className = "chat-avatar";
            avatar.setAttribute("aria-hidden", "true");
            avatar.textContent = "pw";
            row.appendChild(avatar);
        }

        const bubble = document.createElement("div");
        bubble.className = "chat-bubble";
        bubble.textContent = text;
        row.appendChild(bubble);

        chatFeed.appendChild(row);
        chatFeed.scrollTop = chatFeed.scrollHeight;
    }

    if (chatForm && chatInput) {
        chatForm.addEventListener("submit", (event) => {
            event.preventDefault();
            const question = chatInput.value.trim();
            if (!question) {
                return;
            }

            appendChat("user", question);
            chatInput.value = "";

            window.setTimeout(() => {
                appendChat("assistant",
                    "Sample reply: the checked sample comparison is Garden Fresh Milk 946ml at $40.00 at AEON. " +
                    "No live AI service is called, and this answer is not a price guarantee.");
            }, 220);
        });
    }

    $$("[data-chat-suggestion]").forEach((button) => {
        button.addEventListener("click", () => {
            if (chatInput) {
                chatInput.value = button.getAttribute("data-chat-suggestion") || "";
            }
            if (chatForm) {
                chatForm.requestSubmit();
            }
        });
    });

    /* ---------- Snooze demo ---------- */

    $$("[data-snooze]").forEach((button) => {
        button.addEventListener("click", () => {
            button.textContent = "Snoozed for 3 months";
            button.disabled = true;
            showToast("Sample alert snoozed.");
        });
    });

    /* ---------- History empty state ---------- */

    const emptyHistoryToggle = $("#toggle-empty-history");
    if (emptyHistoryToggle) {
        emptyHistoryToggle.addEventListener("click", () => {
            const example = $("#empty-history-example");
            if (!example) {
                return;
            }
            example.hidden = !example.hidden;
            emptyHistoryToggle.textContent = example.hidden ? "Show an empty example" : "Hide the empty example";
        });
    }
})();
