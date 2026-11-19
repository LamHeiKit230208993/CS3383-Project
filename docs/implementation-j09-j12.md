# J09–J12 implementation contract

Updated: 19 November 2026.

This document records what the J09–J12 branch implements and how it is checked.
It is not acceptance evidence on its own. The [J09–J12 plan](Plan/j09-j12.md)
holds the agreed decisions D08–D13; the [project scope](project-scope.md) remains
the main feature record. A missing section means that workstream is still
planned, not delivered.

## Status

| Workstream | Status | Evidence |
|---|---|---|
| 1. Product-wide tracking | Implemented | Focused tracking/alert/reminder tests, browser milestone checks, file-database restart and old-to-new H2 checks |
| 2. Languages foundation | Implemented | `LocalizationTest`; full Maven suite; responsive layout checks recorded in [test history](Test/history.md) |
| 3. J09 Ask | Implemented; focused and full tests pass; browser fallback/chooser passes | `AskServiceTest`, `AskControllerTest`, `HttpAiAnswerClientTest` |
| 4. J10 list sharing | Implemented; focused and full tests pass; browser transfer scenario passes | `ListTransferServiceTest`, `ListTransferControllerTest` |
| 5. J11 offline snapshot | Implemented; focused and full tests pass; app-stopped file check passes | `OfflineSnapshotServiceTest`, `ListTransferControllerTest` |
| 6a. Contrast and accessibility | Implemented; automated contrast/focus/non-colour checks pass | `LocalizationTest`, CSS overrides, `milestone-check.cjs`, `market-desk.cjs` |
| 6b. Personal data | Implemented; focused and full tests pass; browser export/delete passes | `PrivacyControllerTest`, `PersonalDataService` |
| 6c. History views | Implemented; focused and full tests pass; browser history passes | `ProductHistoryControllerTest`, `ProductHistoryService` |
| 7. Integration | Implemented; full Maven and browser suites, persistence restart, and UML exports pass; human diagram review recommended | [test history](Test/history.md), `docs/diagrams/j09-j12/` |

## 1. Product-wide tracking

The shopper-facing control is per product. `TrackingService.startAll(code)`
sets `ProductEntity.trackAllRetailers` and opens period rows for every current
listing; `stopAll(code)` clears the flag and ends every enabled period at one
instant. Both actions are idempotent. Existing per-retailer rows remain the
history unit so drop evidence still follows one product/retailer series.

- A later accepted LIVE import enrolls a product-wide track in a retailer that
  first appears in that import, and seeds its observation from the same import.
- `configureTargetForProduct(code, price)` stores the target on the product and
  applies it to every active period. A retailer enrolled later inherits it.
  Setting a target before tracking fails with `error.tracking.targetBeforeTrack`.
- A product-wide stop disables the per-period stop action; direct calls return
  `error.tracking.productWide`.
- Drop rules remain per retailer because their evidence is per retailer.
- `saved.html` shows one product-level track/stop form and a product target
  form. The route and service contracts are unchanged for existing per-retailer
  records.
- A reminder whose pinned retailer has no current price no longer offers an Add
  action that cannot succeed. It explains the block, keeps the saved retailer
  choice, and offers priced alternatives.

No new table is added. Upgrade compatibility uses the nullable
`track_all_retailers` column and `product_target_price` column added to
`products`; an H2 file created by the pre-J05 build upgrades without losing
catalogue or list state.

## 2. Languages and preferences

Supported languages are English (`en`), Traditional Chinese (`zh-HK`, rendered
as `lang="zh-Hant"`), and Simplified Chinese (`zh-CN`, rendered as
`lang="zh-Hans"`). The default is English regardless of the host operating
system locale.

### Message lookup

- App text lives in `messages.properties`, `messages_zh_HK.properties`, and
  `messages_zh_CN.properties`. All three bundles contain the same keys.
- `MessageConfiguration` supplies Spring's `MessageSource` with
  `fallbackToSystemLocale=false` and English as the default locale.
- `AppMessages` is used by controllers and services that run outside a
  Thymeleaf template. It selects the bundle explicitly from the request locale
  and disables the JVM's system-locale fallback. It never uses the host OS
  locale and returns the key only when a key is genuinely missing.
- Templates use `#{key}` and `#messages.msg(...)`. Dynamic values use message
  parameters rather than concatenated sentences.

### Saved preference

- `PreferenceEntity` holds the single local user's row in `user_preferences`:
  locale and contrast. `PreferenceService` validates and normalizes aliases
  (`zh-Hant`/`zh-TW` to `zh-HK`, `zh-Hans`/`zh` to `zh-CN`, `standard` to
  `normal`). Invalid values raise a localized error and do not write.
- `PreferenceLocaleResolver` is the `localeResolver` bean. On every request it
  reads the saved locale and exposes it both to Spring and as the request
  attribute `pricewatch.locale`, which `AppMessages` reads.
- `PreferenceModelAdvice` adds `preferences`, `htmlLang`, and `contrast` to every
  page model. `market-desk.html` applies them as `<html lang>` and
  `<body data-contrast>`.
- `GET /preferences` renders the form; `POST /preferences` saves it and
  redirects. The sidebar also offers a no-JavaScript language form. No cookie
  or account is used.

### Product and offer labels

`LocalizedText` keeps source-language labels in their own language. Fallback is:

- English UI: English, then Traditional, then Simplified.
- Traditional Chinese UI: Traditional, then English, then Simplified.
- Simplified Chinese UI: Simplified, then English, then Traditional.

Templates emit the selected label's `lang` attribute (`textFor` and
`languageFor`). Product, brand, offer, and reminder names use the same rule in
services. Prices, retailer codes, and product codes are not translated.

### High contrast

The saved `high` contrast preference sets `data-contrast="high"` on every page.
CSS overrides raise text/background contrast, strengthen link underlines, and
enlarge the focus outline. This is a visual preference, not a claim that a full
WCAG contrast or keyboard audit has passed.

## 3. J09 Ask

`GET /ask` shows the question form. `POST /ask` accepts `question` and an
optional `productCode` from the chooser. The flow is fixed:

1. Search runs over the accepted active catalogue, never the raw question
   against the model. A product code in the question wins. Otherwise product
   name, brand, and category labels are matched deterministically.
2. Zero matches return an explicit no-match page. Several equally strong matches
   return a product chooser. Both paths skip the model.
3. One product builds `AskFacts`: code, displayed name, brand, category, each
   retailer and its decimal price or explicit null, lowest price and tied
   retailers, import retrieval time, unknown source price date, and question.
4. `AiAnswerClient` receives only that map. `HttpAiAnswerClient` sends an
   OpenAI-compatible chat-completions request to
   `https://llm.cs.cityu.edu.hk/v1` with model
   `CS3383/DeepSeek-V4-Flash` by default. Base URL, model, timeout, and key are
   server-side configuration.
5. The answer is shown only if every currency amount it contains equals a known
   checked price. A missing key, provider error, timeout, empty/oversize answer,
   or mismatched amount discards the model answer and shows the deterministic
   checked answer. The facts table is rendered in every answered case.

The checked facts and the normal product comparison remain usable when the
model is unavailable. A null price is shown as unknown; it never means out of
stock. The key is read only from `PRICEWATCH_AI_API_KEY` or an untracked local
properties file and is never logged or committed.

## 4. J10 list sharing

`GET /list/import` renders the transfer page. It lists every saved list with its
item count, export link, and preview link. `GET /list/export` exports My list;
`GET /list/export?listId=...` exports a chosen saved list. The response is an
`application/json` attachment named `<list name>.pricewatch.json`. An unknown
list id returns 404.

The file is versioned JSON, not a live link:

```json
{
  "format": "pricewatch-list",
  "version": 1,
  "exportedAt": "2026-11-19T00:00:00Z",
  "name": "My list",
  "items": [
    {"productCode": "P000000124", "quantity": 2, "checked": false, "retailerCode": "JASONS"}
  ]
}
```

Prices are deliberately absent. An importing copy uses its own current accepted
catalogue prices. Export is read-only and preserves product code, quantity,
checked state, and the saved retailer choice.

`POST /list/import` is multipart and takes one `file`. The service checks the
whole file before the first write: a 2 MB byte limit, 999 items, a version-1
object, a non-empty item array, distinct product codes, whole-number quantities
from 1 to 999, a boolean `checked`, active accepted products, and a retailer
code listed for that product. A listed retailer whose current price is null is
accepted and keeps its saved choice; its price stays unknown. An unlisted
retailer is rejected. A rejected file therefore creates no list and changes no
existing list. The successful import runs in one transaction and creates one
separate list; it never overwrites or merges into My list.

Names are control-character-stripped, whitespace-collapsed, and limited to 80
characters without splitting a surrogate pair. A blank name becomes `share.defaultName`. A collision appends
`(imported)` and then a number. The redirect previews the new list with its own
id. `/list` continues to show My list only; imported lists can be previewed and
exported, preserving the existing single-main-list editing behaviour.

Upload values are treated as data. Thymeleaf escapes names and product labels,
the JSON parser does not execute content, and the download filename strips path
and control characters. The servlet multipart limit is 3 MB so files between the
2 MB service limit and 3 MB get the localized too-large message; larger uploads
are caught and redirected with the same message.

## 5. J11 offline snapshot

`GET /list/offline` builds a snapshot of My list; `GET /list/offline?listId=...`
builds one for a chosen saved list. The response is a UTF-8 `text/html`
attachment named `<list name>.pricewatch.html`. An unknown list id returns 404.
The same download is linked from every row of the transfer page and from the My
list heading.

The file is one self-contained document with inline CSS only. It contains:

- the list name and item count;
- product code, displayed name, brand label with its own `lang` attribute,
  quantity, checked state, and retailer;
- the saved unit and line amounts, or `Unknown` when the item has no known
  price; an automatic choice names the retailer whose lowest price supplied the
  amount;
- the saved-on time in Hong Kong time and an ISO `<time datetime>` value;
- a data-age label stating that the amounts are frozen at the saved-on time and
  the file never updates, so the age is the elapsed time since that timestamp;
- `Source price date: Unknown` and a note that a saved amount does not prove a
  current price or stock state.

Some CSS transforms headings and labels to uppercase for display; the text
itself remains in the saved UI language. Currency amounts use the existing
Hong Kong dollar formatting. List names and product labels are escaped, so
imported names such as `<b>Trip</b>` cannot create markup. An empty My list
produces a readable empty-state snapshot instead of a file with no content.

The HTML has no `<script>`, `<form>`, `<a>`, `href`, `src`, image, iframe,
remote URL, refresh control, chat control, or editable field. The milestone
check downloads the file, stops the app, opens the file through a `file://` URL
in an offline browser context, and verifies the saved values and labels remain
visible. It also checks 375 px and 1440 px without horizontal overflow.

## 6a. Contrast and accessibility

The saved contrast preference is applied as `data-contrast="high"` on every
page. High contrast sets white surfaces with black ink, strengthens link
underlines, enlarges the focus outline to 4 px, and gives status pills a
current-colour border so status does not depend on colour alone.

The milestone browser scenario switches to Traditional Chinese and high
contrast, then:

- computes the WCAG relative-luminance ratio of body text against the body
  background and requires at least 7:1;
- requires a non-zero status-pill border;
- presses Tab until focus is in `main` and requires the computed 4 px outline.

The baseline suite separately checks the skip link and Enter-to-main-content
behaviour. These are automated checks in Chromium, not a full WCAG or
screen-reader audit.

## 6b. Personal data

`GET /privacy` shows the single local user's counts and settings, plus export
and delete controls. `PersonalDataService.summary()` counts lists, list items,
wishlist entries, tracking periods, observations, alert events, and reminders,
and checks for preferences, budget, and notification timing.

`GET /privacy/export` returns an `attachment` named
`pricewatch-personal-data.json`. The version-1 JSON uses
`format` = `pricewatch-personal-data` and contains `exportedAt`, `lists` (with
items), `wishlist`, `tracking` (with observations), `alerts`, `budget`,
`reminders`, `preferences`, and `notificationSettings`. It contains no product,
catalogue, or accepted-import section.

`POST /privacy/delete` requires `confirmDelete=true`; without it the controller
redirects with an error and writes nothing. Confirmed deletion removes the
personal rows and calls `ProductRepository.clearTrackingChoices()` to reset
product-wide tracking and target choices. Products, listings, offers, and
accepted-import metadata remain. Exported or shared copies cannot be recalled.
This is not a catalogue reset and not a cross-device account deletion.

## 6c. History views

`GET /history/{code}` is served by `ProductHistoryService`. An unknown or
untracked product returns 404. The view lists one section per tracking period
with retailer, tracking/stopped times, target, drop state, and status.

Observations use retrieval timestamps in Hong Kong time and the stored price or
`Unknown`. The service filters out observations older than 90 days or after the
current instant before building the sequence. When adjacent retained
observations are at least seven days apart, the view adds an explicit
`HistoryEntryView.gap(days)` row. It never fills the interval with invented
daily values, and the page states that the source price date is unknown.
Page-level and per-retailer empty states cover tracked products with no retained
observations. `/saved` links each tracked product to its history.

## 7. Integration

The integrated evidence is the full Maven suite, the baseline browser suite, the
milestone browser flow (including the app-stopped offline file), the
restart check, and the J09–J12 PlantUML exports. The milestone
flow now also performs the privacy export/delete round trip and the tracked
history navigation. `docs/diagrams/j09-j12/` holds editable sources and official
PlantUML 1.2026.9 SVG/PNG exports; see its README for the remaining human
visual-review note.

## Verification

Automated coverage in this branch:

- `LocalizationTest` — English default; saved Traditional and Simplified text on
  pages; invalid preference rejection; bundle key parity; product-label
  fallback order.
- `AskServiceTest`, `AskControllerTest`, and `HttpAiAnswerClientTest` — retrieval,
  chooser/no-match, checked facts, amount rejection, fallback, and the provider
  request boundary with a fake or local test client.
- `ListTransferServiceTest` — round trip, separate-list import, unique names,
  listed-but-unpriced retailer retention, every rejection path without writes,
  and unchanged existing state after a failed import.
- `ListTransferControllerTest` — page, download headers, multipart import and
  preview, empty/oversized/invalid upload, unknown list, preview of another
  saved list, offline download headers and body, chosen-list snapshot, escaping,
  and unknown-list 404.
- `OfflineSnapshotServiceTest` — self-contained source with saved amounts,
  retailer and checked state; saved-on/data-age/source-date labels; no
  script/form/link/remote URL; localized scene text and source labels;
  automatic-retailer naming; explicit unknown amounts; HTML escaping; empty
  list; and unknown list.
- `PrivacyControllerTest` — privacy counts and controls; versioned
  personal-only export with no catalogue section; confirmation-gated deletion;
  catalogue and accepted import retained; product tracking choices reset.
- `ProductHistoryControllerTest` — 404 for unknown and untracked products; real
  observation dates; seven-day gap row; 90-day filtering; empty state; link
  from `/saved`; responsive browser navigation.
- The milestone browser suite measures the high-contrast body ratio (7:1),
  focus outline, status-pill border, privacy export/delete, and history
  navigation. The baseline suite continues to check the skip link.
- Existing J01–J11 service, controller, persistence, and browser suites continue
  to pass. Test counts and dated commands are recorded in
  [docs/Test/history.md](Test/history.md).
- `TC-LANG-001`, `TC-ASK-001`, `TC-SHARE-001`, `TC-OFFLINE-001`,
  `TC-HISTORY-001`, `TC-PRIVACY-001`, and `TC-A11Y-001` describe the maintained
  cases. `TC-TRACK-001`, `TC-ALERT-001`, `TC-REMINDER-001`, and
  `TC-MILESTONE-001` cover product-wide tracking and the integrated browser
  flow.

Known limits:

- Chinese text is reviewed source text, not machine translation evidence.
- Layout checks cover 375, 768, 1024, and 1440 px, but not every browser engine.
- Preferences belong to one local user; there is no per-account language or
  cross-device sync.
- Imported lists can be previewed and exported but are not edited in `/list`,
  which keeps the existing single-main-list behaviour.
- Personal-data deletion is local-row deletion, not a catalogue reset or account
  deletion. It cannot recall exported or shared files.
- History is read-only and limited to 90 days of accepted LIVE observations.
  Gap rows describe missing observations; they are not price estimates.
- The automated contrast check covers computed colours and focus outline in
  Chromium, not a full WCAG audit or screen-reader pass.
- The PlantUML sources were checked and rendered with the official 1.2026.9
  renderer, but a human visual pass over the four exports is still recommended.
- Sharing is a file copy. Later edits on either copy do not sync, and both local
  apps need a compatible accepted catalogue.
- The offline snapshot is a static file. Its displayed data age is the elapsed
  time since the visible saved-on timestamp; nothing inside the file updates.
  It is read-only, so checking an item or changing a quantity offline is not
  supported.
