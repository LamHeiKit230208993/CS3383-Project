# Issue #21 shopper workflow checks

## Local validation · 18 November 2026

The combined shopper suite used the same 236 mL milk pack across Ask,
list export and import, offline review, language switching, and price history.
It also checked personal-data export, cancellation, and confirmed reset.
The J01–J08 build from commit `219cc5b` created the database used to verify
upgrade compatibility with J12. See the [dated test results](../Test/history.md).

The [setup guide](../SETUP.md) documents how to use these features. The
[shopper sequence](../diagrams/j10-j12/shopper-sequence.puml) shows the
local storage and browser session steps. The combined retailer chart and
the product-wide alert rule change remain follow-up work.

Use the packaged app with an isolated database and controlled price fixtures.
After each small feature, run its focused check and walk through the matching
browser flow. Record the result in [test history](../Test/history.md). Run the
full Maven and Chrome suites before calling the combined milestone ready.

| Work | What the shopper does | Pass criteria |
|---|---|---|
| J09 · Checked chat | Ask “How much is it?”, name an ambiguous product, choose the 236 mL pack, then ask about Wellcome. Start a second chat and reopen the first. | The app asks for missing details, makes the shopper choose among matching packs, and returns only checked prices. The follow-up uses the original pack, and history restores the correct conversation. Price search still works when AI is unavailable. |
| J10 · List transfer | Add two products, pin one retailer, mark one item checked, export, and import the file into a second local app. Try a damaged file next. | Import creates a separate named list with the same products, quantities, checked states and retailer choices. A missing listing stays unknown. A damaged file gives an actionable error and leaves existing lists unchanged. |
| J11 · Offline review | Save a list, disconnect internet while keeping the local app available, and reopen the saved view. Try refresh, chat and editing. | The view shows the same saved items, estimates and retrieval time. It is read-only, and live refresh and remote chat are unavailable. |
| J12 · Language and accessibility | Switch through English, Traditional Chinese and Simplified Chinese. Search a known product in each language. Use keyboard-only navigation and a narrow viewport with high contrast enabled. | Core labels and feedback follow the chosen language. The same product remains findable, focus is visible, status has text, and the controls fit and work at mobile width. |
| J12 · Personal data and history | View and export local personal records, cancel a reset, then confirm it in an isolated database. Open history with recorded prices, a gap, and no observations. | Export includes the agreed records and the current session's chats. Cancel preserves them. Confirm clears personal data and preserves the accepted catalogue and import details. History shows retrieval times, retailer labels, gaps and an empty state. |
| Product alerts · follow-up | Track one exact product, set one target, and refresh prices with zero, one, then several qualifying retailers. Repeat the refresh, then raise prices and cross the target again. Add a reminder for an untracked product. | One alert lists all qualifying retailers, identical refreshes do not duplicate it, and the agreed rearm rule allows a new alert. History remains per retailer. Reminder timing and list actions work independently. |

For the combined check, use the same fixture product across Ask, a shared list,
offline review and the translated history view. Verify the retailer and pack
stay consistent, and check the alert and reminder flow in that same run.

These are acceptance criteria. An unfinished job has no pass result yet.
