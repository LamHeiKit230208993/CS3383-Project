package com.pricewatch.shopping;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** J11 read-only offline list: renders My list as one self-contained HTML file. */
@Service
public class OfflineListExporter {
    private static final String STYLE = """
            :root{color-scheme:light;--page:#f5f7fb;--surface:#fff;--ink:#16243b;--muted:#53647c;--line:#dce3ed;--blue:#2854ce;--blue-soft:#edf2ff;--green:#146b48;font-family:"Segoe UI",-apple-system,BlinkMacSystemFont,Arial,sans-serif;font-size:16px}
            *{box-sizing:border-box}
            body{margin:0;background:var(--page);color:var(--ink);line-height:1.6}
            main{max-width:780px;margin:auto;padding:34px 22px 44px}
            h1{font-size:clamp(26px,4vw,36px);letter-spacing:-1px;margin:0}
            h2{font-size:22px;letter-spacing:-.5px;margin:0}
            h3{font-size:17px;margin:0}
            p{margin:0}
            .eyebrow{color:var(--muted);font-size:10px;letter-spacing:1.8px;font-weight:650;margin-bottom:14px}
            .intro{color:var(--muted);font-size:14px;margin-top:12px}
            .panel{background:var(--surface);border:1px solid var(--line);border-radius:18px;padding:26px;margin-top:22px}
            .section-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;margin:0 0 18px}
            .section-heading>span{color:var(--muted);font-size:12px}
            .helper{font-size:13px;color:var(--muted);line-height:1.7;margin-top:12px}
            .source-note{font-size:12px;color:var(--muted);border-top:1px solid var(--line);padding-top:18px;margin-top:26px}
            .warning{padding:12px;background:#fff4dd;color:#69470e;border-radius:8px;font-size:13px;margin-top:16px}
            .list-item{padding:22px 0;border-top:1px solid var(--line)}
            .list-item:first-of-type{border-top:0}
            .list-item-heading{display:flex;align-items:start;justify-content:space-between;gap:20px}
            .list-item-heading>div{min-width:0}
            .line-total{font-size:22px;font-variant-numeric:tabular-nums;white-space:nowrap}
            .is-checked h3{text-decoration:line-through;color:var(--muted)}
            .selection-mode{color:var(--blue);font-size:12px;margin-top:8px}
            .check-label{display:flex;align-items:center;gap:10px;font-size:14px;margin-top:14px;cursor:pointer}
            .check-label input{width:20px;height:20px;accent-color:var(--blue)}
            .summary-total{border-top:1px solid var(--line);padding-top:20px;margin-top:20px}
            .summary-total span{font-size:13px;color:var(--muted);display:block}
            .summary-total strong{font-size:30px;letter-spacing:-.8px;font-variant-numeric:tabular-nums;display:block}
            """;
    private static final String SCRIPT = """
            <script>
            (function(){
              var items=[].slice.call(document.querySelectorAll('.list-item'));
              var count=document.getElementById('done-count');
              items.forEach(function(item){
                var box=item.querySelector('input[type=checkbox]');
                var sync=function(){item.classList.toggle('is-checked',box.checked);};
                box.addEventListener('change',function(){sync();render();});
                sync();
              });
              function render(){
                if(!count)return;
                var done=items.filter(function(i){var b=i.querySelector('input');return b&&b.checked;}).length;
                count.textContent=done+' of '+items.length+' done';
              }
              render();
            })();
            </script>
            """;

    private final ShoppingService shopping;
    private final Supplier<Instant> clock;

    public OfflineListExporter(ShoppingService shopping, Clock clock) {
        this.shopping = shopping;
        this.clock = clock::instant;
    }

    public byte[] export() {
        List<ItemView> items = shopping.items();
        Summary summary = Summary.from(items);
        StringBuilder html = new StringBuilder(4096);
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(escape("Market Desk \u2014 My list (saved copy)")).append("</title>")
                .append("<style>").append(STYLE).append("</style></head><body><main>")
                .append("<header><p class=\"eyebrow\">").append(escape("SAVED COPY \u00b7 WORKS OFFLINE")).append("</p>")
                .append("<h1>").append(escape("Your next shop, saved.")).append("</h1>")
                .append("<p class=\"intro\">").append(escape("Prices saved ")).append(escape(time(clock.get())))
                .append(escape(". Amounts may be out of date; tick items while you shop. Ticks are not saved."))
                .append("</p></header>");

        html.append("<section class=\"panel\">")
                .append("<div class=\"section-heading\"><h2>").append(escape("My list"))
                .append("</h2><span id=\"done-count\">").append(escape(items.size() + " products"))
                .append("</span></div>");
        if (items.isEmpty()) {
            html.append("<p class=\"helper\">").append(escape("This list was empty when it was saved.")).append("</p>");
        } else {
            for (ItemView item : items) {
                html.append(renderItem(item));
            }
            html.append(summaryBlock(summary));
        }
        html.append("<p class=\"source-note\">").append(escape(
                        "A read-only copy. No new prices are fetched and no chat is available offline. "
                                + "Delivery, stock, and multi-buy offers are not included. This is not actual spending."))
                .append("</p></section>");
        html.append("</main>").append(SCRIPT).append("</body></html>");
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String renderItem(ItemView item) {
        var product = item.product();
        String name = product.name().en();
        String checkedClass = item.checked() ? " is-checked" : "";
        String retailerLabel = item.selectedRetailerCode() == null
                ? "Automatic lowest listed price"
                : "Your retailer: " + item.selectedRetailerCode();
        String priceLine = item.unitPrice() == null
                ? "No current price. Kept in your list."
                : money(item.unitPrice()) + " per pack \u00b7 " + item.quantity() + " packs"
                        + (item.retailerCode() == null ? "" : " \u00b7 " + item.retailerCode());
        return new StringBuilder()
                .append("<article class=\"list-item").append(checkedClass).append("\">")
                .append("<div class=\"list-item-heading\"><div>")
                .append("<h3>").append(escape(name)).append("</h3>")
                .append("<p class=\"helper\">").append(escape(product.brand().en() + " \u00b7 " + product.code()))
                .append("</p></div><strong class=\"line-total\">").append(escape(money(item.lineTotal())))
                .append("</strong></div>")
                .append("<p class=\"selection-mode\">").append(escape(retailerLabel)).append("</p>")
                .append("<p class=\"helper\">").append(escape(priceLine)).append("</p>")
                .append("<label class=\"check-label\"><input type=\"checkbox\"").append(item.checked() ? " checked" : "")
                .append("> ").append(escape("Got it")).append("</label>")
                .append("</article>")
                .toString();
    }

    private String summaryBlock(Summary summary) {
        String heading = summary.unknownItemCount() > 0 ? "Known subtotal" : "Estimated list cost";
        StringBuilder block = new StringBuilder()
                .append("<div class=\"summary-total\"><span>").append(escape(heading)).append("</span>")
                .append("<strong>").append(escape(money(summary.knownSubtotal()))).append("</strong></div>");
        if (summary.unknownItemCount() > 0) {
            block.append("<p class=\"warning\">")
                    .append(escape(summary.unknownItemCount() + " items have no current price. Total is incomplete."))
                    .append("</p>");
        }
        block.append("<p class=\"helper\">").append(escape(
                "Uses saved retailer choices. A missing selected price stays unknown; "
                        + "we do not switch your retailer.")).append("</p>");
        return block.toString();
    }

    private String money(BigDecimal value) {
        if (value == null) return "Not listed";
        var format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-HK"));
        format.setCurrency(Currency.getInstance("HKD"));
        format.setMaximumFractionDigits(Math.max(2, value.stripTrailingZeros().scale()));
        return format.format(value);
    }

    private String time(Instant value) {
        return DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm 'HKT'", Locale.ENGLISH)
                .withZone(ZoneId.of("Asia/Hong_Kong")).format(value);
    }

    private String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
