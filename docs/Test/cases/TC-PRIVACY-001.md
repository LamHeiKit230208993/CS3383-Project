# TC-PRIVACY-001 — View, export and delete personal records

This case covers decision D13. Dated execution results are recorded separately
in the test history.

Sources:
[`PrivacyControllerTest`](../../../src/test/java/com/pricewatch/web/PrivacyControllerTest.java),
[`PersonalDataService`](../../../src/main/java/com/pricewatch/privacy/PersonalDataService.java),
and the privacy scenario in `src/test/browser/milestone-check.cjs`.

## Stored records

`GET /privacy` shows counts for lists, list items, wishlist entries, tracking
periods, price observations, alert events, and reminders. It also shows whether
language/contrast preferences, budget settings, and alert timing are set. The
page offers a JSON export link and a delete form whose required checkbox is
named `confirmDelete`.

## Export

`GET /privacy/export` returns an `attachment` named
`pricewatch-personal-data.json` with `application/json`. The version-1 document
has `format` = `pricewatch-personal-data`, `version` = 1, an `exportedAt`
instant, and these personal sections:

- `lists` with item product code, quantity, checked state, and retailer choice;
- `wishlist` entries with product code and saved time;
- `tracking` periods with retailer, enabled/stopped times, target, drop rule,
  and their observations;
- `alerts`, `budget`, `reminders`, `preferences`, and `notificationSettings`.

The accepted catalogue is not personal data. The export has no `products`,
`catalogue`, or `acceptedImport` section. Two copies can be exported without
changing stored state.

## Delete

`POST /privacy/delete` without confirmation redirects to `/privacy` with an
error flash and changes nothing. With `confirmDelete=true` it deletes the user's
lists, list items, wishlist entries, tracking periods, observations, alert
events, reminders, budget, preferences, and alert timing.

The accepted catalogue and its accepted-import metadata remain. Products and
their listings are still readable after deletion, and product-wide tracking and
target choices are reset. Exported or shared copies cannot be recalled. The
export contains one local user's records; sharing a list remains the separate
`pricewatch-list` format.

## Browser evidence

The milestone scenario opens `/privacy`, downloads the JSON, checks the personal
sections and the absence of a catalogue section, submits the unconfirmed delete,
and confirms that nothing changed. It then confirms deletion, checks the
personal counts are zero and every setting is `Not set`, and checks that an
accepted product page still opens. Finally it re-adds two products to My list so
the later offline-snapshot scenario still has saved data.

## Limits

This is one device-local user. Deletion does not perform a full catalogue reset,
does not erase browser downloads, and cannot revoke copies already sent to
another person. Deleting preferences returns the UI to the English/normal
default. The browser check uses Chromium and a synthetic local catalogue.
