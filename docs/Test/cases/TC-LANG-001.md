# TC-LANG-001 — Saved language, contrast, and product-label fallback

This case covers the three-UI-language foundation: reviewed message bundles,
the one local user's saved language and contrast preference, application of
those preferences to every page, and the product-label fallback order. Language
is saved through a normal form, so it works without JavaScript or cookies.

Sources: [`LocalizationTest`](../../../src/test/java/com/pricewatch/preferences/LocalizationTest.java),
[`PreferenceService`](../../../src/main/java/com/pricewatch/preferences/PreferenceService.java),
[`PreferenceLocaleResolver`](../../../src/main/java/com/pricewatch/preferences/PreferenceLocaleResolver.java),
and [`LocalizedText`](../../../src/main/java/com/pricewatch/catalog/LocalizedText.java).

1. Open `/budget` with no saved preference. Expect `<html lang="en">`, the English
   heading `Your planned limit`, and English scene text such as `PLAN YOUR SHOP`.
   The host operating system's locale must not change this result.
2. Save `locale=zh-HK` and `contrast=normal` through `POST /preferences`. Expect a
   redirect to `/preferences`. Reopen `/budget` and expect `<html lang="zh-Hant">`,
   `預算`, and `你的計劃上限`. The same saved language must apply to every page.
3. Save `locale=zh-CN` and `contrast=high`. Reopen `/list` and expect
   `<html lang="zh-Hans">`, `data-contrast="high"` on the body, and `我的清单`.
4. Compare the key sets of `messages.properties`, `messages_zh_HK.properties`, and
   `messages_zh_CN.properties`. All three bundles must contain the same keys so a
   saved language cannot leak a raw key or fall back to another language for a
   missing app message.
5. Build a product label with only English and Simplified text
   (`new LocalizedText("Milk", null, "牛奶")`). For English expect `Milk`; for
   `zh-HK` expect the existing fallback order to supply English `Milk`; for
   `zh-CN` expect `牛奶`.
6. Reject an unsupported locale or contrast value without changing the stored
   preference. The saved preference is a local database row for the single local
   user; it is not an account, cookie, or cloud setting.

Chinese bundle text is reviewed source text, not machine translation output.
The preference changes app language only; it does not translate source product
names, brand names, retailer codes, or prices. Layout checks for the longer
Chinese text at 375, 768, 1024, and 1440 px are recorded separately in the test
history and are not claimed by these automated tests.
