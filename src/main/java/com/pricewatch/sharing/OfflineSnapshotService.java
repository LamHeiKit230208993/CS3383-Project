package com.pricewatch.sharing;

import com.pricewatch.shopping.ItemView;
import com.pricewatch.shopping.ShoppingService;
import com.pricewatch.shopping.persistence.ShoppingListEntity;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

/**
 * Builds one standalone HTML file. The result has inline CSS only: no script,
 * form, link, image, refresh control, or remote URL can make the snapshot depend
 * on the running app or a network connection.
 */
@Service
public class OfflineSnapshotService {
    private static final String MAIN_LIST = "My list";
    private static final ZoneId HONG_KONG = ZoneId.of("Asia/Hong_Kong");

    private static final String STYLE = """
            :root {
              color-scheme: light;
              --ink: #1d1b18;
              --muted: #5f5a50;
              --paper: #f5f0e6;
              --card: #fffdf8;
              --line: #d9d0c0;
              --accent: #0b5d4b;
              --accent-soft: #e2efe9;
            }
            * { box-sizing: border-box; }
            body {
              margin: 0;
              background: var(--paper);
              color: var(--ink);
              font-family: "Segoe UI", system-ui, -apple-system, sans-serif;
              line-height: 1.55;
            }
            main {
              width: calc(100% - 2rem);
              max-width: 980px;
              margin: 0 auto;
              padding: 2.5rem 0 3rem;
            }
            .hero { border-bottom: 3px solid var(--ink); padding-bottom: 1.5rem; }
            .eyebrow {
              margin: 0 0 .5rem;
              color: var(--accent);
              font-size: .78rem;
              font-weight: 800;
              letter-spacing: .14em;
              text-transform: uppercase;
            }
            h1 {
              margin: 0;
              font-size: clamp(2rem, 6vw, 3.4rem);
              line-height: 1.05;
              letter-spacing: -.03em;
              overflow-wrap: anywhere;
            }
            .intro { max-width: 62ch; margin: 1rem 0 0; color: var(--muted); font-size: 1.05rem; }
            .badges { display: flex; flex-wrap: wrap; gap: .5rem; margin: 1rem 0 0; }
            .badges span {
              border: 1px solid var(--accent);
              border-radius: 999px;
              background: var(--accent-soft);
              color: var(--accent);
              padding: .2rem .7rem;
              font-size: .78rem;
              font-weight: 800;
              letter-spacing: .04em;
              text-transform: uppercase;
            }
            .panel {
              margin-top: 1.5rem;
              border: 1px solid var(--line);
              border-radius: 16px;
              background: var(--card);
              padding: 1.25rem;
            }
            .meta { display: grid; gap: 1rem; grid-template-columns: repeat(3, minmax(0, 1fr)); }
            .meta > div { min-width: 0; }
            .label {
              display: block;
              color: var(--muted);
              font-size: .72rem;
              font-weight: 800;
              letter-spacing: .1em;
              text-transform: uppercase;
            }
            .meta p { margin: .35rem 0 0; overflow-wrap: anywhere; }
            .notice {
              grid-column: 1 / -1;
              margin: 0;
              border-left: 4px solid var(--accent);
              background: var(--accent-soft);
              padding: .75rem 1rem;
            }
            .section-head {
              display: flex;
              flex-wrap: wrap;
              align-items: baseline;
              justify-content: space-between;
              gap: .5rem;
              margin-bottom: .75rem;
            }
            .section-head h2 { margin: 0; font-size: 1.2rem; }
            .section-head span { color: var(--muted); font-size: .9rem; }
            table { width: 100%; border-collapse: collapse; }
            th, td {
              border-bottom: 1px solid var(--line);
              padding: .65rem .45rem;
              text-align: left;
              vertical-align: top;
            }
            th {
              color: var(--muted);
              font-size: .72rem;
              letter-spacing: .08em;
              text-transform: uppercase;
            }
            tbody tr:last-child td { border-bottom: 0; }
            .product-cell strong { display: block; overflow-wrap: anywhere; }
            .brand { display: block; color: var(--muted); font-size: .88rem; }
            code {
              display: inline-block;
              margin-top: .3rem;
              color: var(--accent);
              font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
              font-size: .82rem;
              overflow-wrap: anywhere;
            }
            .number { white-space: nowrap; }
            .empty { margin: 0; color: var(--muted); }
            footer {
              margin-top: 2rem;
              border-top: 1px solid var(--line);
              padding-top: 1rem;
              color: var(--muted);
              font-size: .88rem;
            }
            footer p { margin: .25rem 0; }
            @media (max-width: 760px) {
              main { width: calc(100% - 1rem); padding-top: 1.25rem; }
              .meta { grid-template-columns: 1fr; }
              thead {
                position: absolute;
                width: 1px;
                height: 1px;
                overflow: hidden;
                clip: rect(0 0 0 0);
                white-space: nowrap;
              }
              table, tbody, tr, td { display: block; width: 100%; }
              tr {
                border: 1px solid var(--line);
                border-radius: 12px;
                margin-bottom: .75rem;
                padding: .5rem .75rem;
              }
              td {
                display: flex;
                justify-content: space-between;
                gap: 1rem;
                border: 0;
                padding: .35rem 0;
              }
              td::before {
                content: attr(data-label);
                color: var(--muted);
                font-size: .72rem;
                font-weight: 800;
                letter-spacing: .08em;
                text-transform: uppercase;
              }
              td.product-cell { display: block; }
              td.product-cell::before { display: block; margin-bottom: .2rem; }
              td.number { white-space: normal; }
            }
            @media print {
              body { background: #fff; }
              main { width: 100%; padding: 0; }
              .panel { break-inside: avoid; }
            }
            """;

    private final ShoppingService shopping;
    private final ShoppingListRepository listRepository;
    private final MessageSource messages;
    private final Clock clock;

    public OfflineSnapshotService(ShoppingService shopping,
                                  ShoppingListRepository listRepository,
                                  MessageSource messages,
                                  Clock clock) {
        this.shopping = shopping;
        this.listRepository = listRepository;
        this.messages = messages;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OfflineSnapshot snapshot(Long listId, Locale requestedLocale) {
        Locale locale = requestedLocale == null ? Locale.ENGLISH : requestedLocale;
        String name;
        List<ItemView> items;
        if (listId == null) {
            name = MAIN_LIST;
            items = shopping.items();
        } else {
            ShoppingListEntity list = listRepository.findById(listId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            text(locale, "error.share.listNotFound")));
            name = list.getName();
            items = shopping.items(listId);
        }
        String html = render(name, items, locale, clock.instant());
        return new OfflineSnapshot(name, html.getBytes(StandardCharsets.UTF_8));
    }

    private String render(String name, List<ItemView> items, Locale locale, Instant savedAt) {
        String htmlLang = htmlLang(locale);
        String savedOn = DateTimeFormatter.ofPattern(text(locale, "format.datetime"), locale)
                .withZone(HONG_KONG)
                .format(savedAt);

        StringBuilder out = new StringBuilder(12_000);
        out.append("<!DOCTYPE html>\n<html lang=\"").append(escape(htmlLang)).append("\">\n<head>\n")
                .append("<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<meta name=\"referrer\" content=\"no-referrer\">\n")
                .append("<title>").append(escape(text(locale, "offline.documentTitle", name)))
                .append("</title>\n<style>").append(STYLE).append("</style>\n</head>\n<body>\n<main>\n");

        out.append("<header class=\"hero\">\n")
                .append("<p class=\"eyebrow\">").append(escape(text(locale, "offline.eyebrow")))
                .append("</p>\n<h1>").append(escape(name)).append("</h1>\n")
                .append("<p class=\"intro\">").append(escape(text(locale, "offline.intro")))
                .append("</p>\n<p class=\"badges\"><span>")
                .append(escape(text(locale, "offline.readOnlyBadge"))).append("</span><span>")
                .append(escape(text(locale, "offline.noConnectionBadge"))).append("</span></p>\n</header>\n");

        out.append("<section class=\"panel meta\" aria-labelledby=\"saved-on-label\">\n")
                .append("<div><span class=\"label\" id=\"saved-on-label\">")
                .append(escape(text(locale, "offline.savedOn"))).append("</span><p><time datetime=\"")
                .append(escape(savedAt.toString())).append("\">").append(escape(savedOn))
                .append("</time></p></div>\n")
                .append("<div><span class=\"label\">").append(escape(text(locale, "offline.dataAge")))
                .append("</span><p>").append(escape(text(locale, "offline.dataAgeValue")))
                .append("</p></div>\n")
                .append("<div><span class=\"label\">").append(escape(text(locale, "offline.sourceDate")))
                .append("</span><p><strong>").append(escape(text(locale, "offline.sourceDateUnknown")))
                .append("</strong></p></div>\n")
                .append("<p class=\"notice\">").append(escape(text(locale, "offline.priceNotice")))
                .append("</p>\n</section>\n");

        out.append("<section class=\"panel\" aria-labelledby=\"items-heading\">\n")
                .append("<div class=\"section-head\"><h2 id=\"items-heading\">")
                .append(escape(text(locale, "offline.itemsHeading"))).append("</h2><span>")
                .append(escape(text(locale, "list.productCount", items.size()))).append("</span></div>\n");

        if (items.isEmpty()) {
            out.append("<p class=\"empty\">").append(escape(text(locale, "offline.empty")))
                    .append("</p>\n");
        } else {
            appendTable(out, items, locale);
        }
        out.append("</section>\n");

        out.append("<footer><p>").append(escape(text(locale, "offline.footer"))).append("</p><p>")
                .append(escape(text(locale, "offline.sourceNote"))).append("</p></footer>\n")
                .append("</main>\n</body>\n</html>\n");
        return out.toString();
    }

    private void appendTable(StringBuilder out, List<ItemView> items, Locale locale) {
        out.append("<table>\n<thead><tr>")
                .append("<th scope=\"col\">").append(escape(text(locale, "offline.product"))).append("</th>")
                .append("<th scope=\"col\">").append(escape(text(locale, "offline.packs"))).append("</th>")
                .append("<th scope=\"col\">").append(escape(text(locale, "offline.checked"))).append("</th>")
                .append("<th scope=\"col\">").append(escape(text(locale, "offline.retailer"))).append("</th>")
                .append("<th scope=\"col\">").append(escape(text(locale, "offline.unitPrice"))).append("</th>")
                .append("<th scope=\"col\">").append(escape(text(locale, "offline.lineAmount"))).append("</th>")
                .append("</tr></thead>\n<tbody>\n");

        for (ItemView item : items) {
            String productName = item.product().name().textFor(locale);
            if (productName == null || productName.isBlank()) productName = item.product().code();
            String brand = item.product().brand().textFor(locale);
            String retailer = item.selectedRetailerCode() != null
                    ? item.selectedRetailerCode()
                    : item.retailerCode() == null ? text(locale, "offline.automatic")
                    : text(locale, "offline.automaticRetailer", item.retailerCode());

            out.append("<tr>")
                    .append("<td class=\"product-cell\" data-label=\"")
                    .append(escape(text(locale, "offline.product"))).append("\"><strong lang=\"")
                    .append(escape(item.product().name().languageFor(locale))).append("\">")
                    .append(escape(productName)).append("</strong>");
            if (brand != null && !brand.isBlank()) {
                out.append("<span class=\"brand\" lang=\"")
                        .append(escape(item.product().brand().languageFor(locale))).append("\">")
                        .append(escape(brand)).append("</span>");
            }
            out.append("<code>").append(escape(item.product().code())).append("</code></td>")
                    .append("<td class=\"number\" data-label=\"")
                    .append(escape(text(locale, "offline.packs"))).append("\">")
                    .append(item.quantity()).append("</td>")
                    .append("<td data-label=\"").append(escape(text(locale, "offline.checked"))).append("\">")
                    .append(escape(item.checked()
                            ? text(locale, "offline.checkedYes")
                            : text(locale, "offline.checkedNo"))).append("</td>")
                    .append("<td data-label=\"").append(escape(text(locale, "offline.retailer"))).append("\">")
                    .append(escape(retailer)).append("</td>")
                    .append("<td class=\"number\" data-label=\"")
                    .append(escape(text(locale, "offline.unitPrice"))).append("\">")
                    .append(escape(money(item.unitPrice(), locale))).append("</td>")
                    .append("<td class=\"number\" data-label=\"")
                    .append(escape(text(locale, "offline.lineAmount"))).append("\">")
                    .append(escape(money(item.lineTotal(), locale))).append("</td>")
                    .append("</tr>\n");
        }
        out.append("</tbody>\n</table>\n");
    }

    private String money(BigDecimal value, Locale locale) {
        if (value == null) return text(locale, "offline.unknown");
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-HK"));
        format.setCurrency(Currency.getInstance("HKD"));
        format.setMaximumFractionDigits(Math.max(2, value.stripTrailingZeros().scale()));
        return format.format(value);
    }

    private static String htmlLang(Locale locale) {
        if (locale != null && "zh".equals(locale.getLanguage())) {
            boolean traditional = "HK".equalsIgnoreCase(locale.getCountry())
                    || "Hant".equalsIgnoreCase(locale.getScript());
            return traditional ? "zh-Hant" : "zh-Hans";
        }
        return "en";
    }

    private String text(Locale locale, String key, Object... arguments) {
        return messages.getMessage(key, arguments, key, locale);
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
    }
}
