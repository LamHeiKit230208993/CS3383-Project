# TC-UI-001 - Three UI languages (J12, R46)

Covers English, Traditional Chinese, and Simplified Chinese app text, the
`/lang` switch, and product labels chosen from the source JSON.

Sources:
[`LocaleUiTest`](../../../src/test/java/com/pricewatch/web/LocaleUiTest.java),
[`LocalizedFeedbackTest`](../../../src/test/java/com/pricewatch/web/LocalizedFeedbackTest.java).

## Preconditions

- The app is running with the default database and the messages bundles
  (`messages.properties`, `messages_zh_TW.properties`, `messages_zh_CN.properties`).
- No `LANG` cookie has been set yet.

## Steps and expected results

1. Open `/` with no cookie. `<html lang="en">`, the page says `Find a product`,
   and no Chinese wording appears.
2. Open **Settings** (`/my-data`) and click **繁體中文** (or `GET /lang?code=zh_TW&back=/`). The cookie `LANG=zh_TW`
   is set, the app redirects to the same page, `<html lang="zh-Hant">`, and the
   nav and search label show Traditional Chinese (`尋找產品`, `我的清單`, `已儲存`).
3. Click **简体中文** (`GET /lang?code=zh_CN&back=/`). `<html lang="zh-Hans">` and
   Simplified wording appears (`寻找产品`), with no Traditional-only glyphs.
4. A missing or invalid code (`/lang?code=xx`) falls back to English; a `back`
   value that leaves the host (`//evil.example`) redirects to `/` instead.
5. Search works in each language after one import: `100 %Fresh Milk`, `鮮牛奶`,
   `鲜牛奶` all find `P000000124`.
6. Product labels use the JSON `zh-Hant`/`zh-Hans` text where present and fall
   back to English where a translation is missing. The English UI shows only the
   English product name, with no Chinese duplicate; a Chinese UI shows the name
   in that script. The labelled **Chinese offer text** disclosure on the
   comparison page stays available because the source offer wording is Chinese.
7. Feedback is translated too: after a list change, a budget save, a reminder
   action, or a rejected form, the notice or error text appears in the chosen
   language. Page titles (browser tab and breadcrumb) follow the language as
   well.
8. A service rule that rejects input (for example adding the same product with
   a different retailer, or a quantity outside 1-999) shows its error in the
   chosen language; English stays the default when no `LANG` cookie is set.

## Limits

- Translations need a native-speaker review before release (flagged in the
  bundle headers).
- Category filter values come from the source data and are not translated.
