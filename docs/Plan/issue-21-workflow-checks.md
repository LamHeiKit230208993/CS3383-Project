# Issue #21 shopper workflow checks

## Local validation · 18 November 2026

The combined shopper suite used the same 236 mL milk pack across Ask,
list export and import, offline review, language switching, and price history.
It also checked personal-data export, cancellation, and confirmed reset.
The J01–J08 build from commit `219cc5b` created the database used to verify
upgrade compatibility with J12. See the [dated test results](../Test/history.md).

The [setup guide](../SETUP.md) documents how to use these features. The
[shopper sequence](../diagrams/j10-j12/shopper-sequence.puml) shows current
list transfer, offline checklist, and personal-data flows. The
[Ask sequence](../diagrams/j10-j12/ask-sequence.puml) shows checked answers
and shopping actions.

Use the packaged app with an isolated database and controlled price fixtures.
After each small feature, run its focused check and walk through the matching
browser flow. Record the result in [test history](../Test/history.md). Run the
full Maven and Chrome suites before calling the combined milestone ready.

| Work | What the shopper does | Pass criteria |
|---|---|---|
| J09 · Checked chat | Ask in English for a Traditional Chinese reply. Ask “I want the cheapest milk”. In a second chat, ask for milk options, choose the 236 mL pack, then ask about Wellcome. Reopen the first chat. | DeepSeek writes natural replies and can ask for details or call the catalogue tools. The lowest-price search ranks matching packs by listed pack price. The pack chooser and price cards use catalogue records. A follow-up uses the same pack. Each chat keeps its own context. A provider failure keeps the question available for retry. |
| J09 · Shopping tools | Show a named Cart, ask for a calculation, remove an item, and set an alert over two chat turns. Open an item card and return to Ask. | The app renders checked data, calculates using decimal arithmetic, and confirms an exact removal or alert before saving. Retrying a reply or confirmation applies changes once. Returning opens the original conversation and card. |
| J10 · List management and transfer | Create and rename a list, add an exact pack to it from comparison, switch lists, and delete a named list with confirmation. Export and import, then try a damaged file. | The default Cart stays available. Imported lists remain separate. Each Cart estimate uses the active list and the same saved budget. Missing prices stay unknown, and damaged imports preserve existing data. |
| J11 · Offline checklist | Download the selected Cart as HTML, stop the app, disconnect the network, and open the file. Check an item and edit the quantity, retailer, or budget. | The file shows saved prices and retrieval time. It recalculates locally and keeps changes in the open copy. |
| J12 · Language and accessibility | Switch through English, Traditional Chinese and Simplified Chinese. Search a known product in each language. Use keyboard-only navigation and a narrow viewport with high contrast enabled. | Core labels and feedback follow the chosen language. The same product remains findable, focus is visible, status has text, and the controls fit and work at mobile width. |
| J12 · Personal data and history | View and export local personal records, cancel a reset, then confirm it in an isolated database. Open history with recorded prices, a gap, and no observations. | Export includes the agreed records and the current session's chats. Cancel preserves them. Confirm clears personal data and preserves the accepted catalogue and import details. History shows retrieval times, retailer labels, gaps and an empty state. |
| Product alerts | Save one exact pack, set a target, and refresh with several qualifying retailers. Repeat the refresh, then raise prices and cross the target again. Add a time reminder for an unwatched product. | One alert lists all qualifying retailers. Identical refreshes do not duplicate it. The rearm rule allows a later alert. A time reminder can be edited from the item panel and stays in Alerts history after dismissal. |

For the combined check, use the same fixture product across Ask, a shared list,
offline review and the translated history view. Verify the retailer and pack
stay consistent, and check the alert and reminder flow in that same run.

These are acceptance criteria. An unfinished job has no pass result yet.
