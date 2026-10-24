# Baseline Decisions

Updated: 17 October 2026. This records the agreed build approach, not completed work.

The [project scope](../project-scope.md) remains the main record of all 48 feature decisions. The [feature plan](features.md) turns those decisions into jobs. The [JSON sample check](data-sample-review.md) records what was checked.

## 1. Confirmed direction

| Area | First baseline |
|---|---|
| App | One local Spring Boot app for one user. No account, sign-in, or cloud saving. |
| Screens | Start with simple pages in the existing app. Build a working search-to-list flow before full visual design. |
| Saved data | Use an H2 file database for normal use. Memory-only storage is for tests. Lists, settings, and tracked history must survive restart. |
| Price source | Start from the checked JSON structure. It contains names in three languages within each product. Import code still needs implementation and tests. |
| Search | Search stored language names under the same source product code. Confirm exact product and pack before comparison. |
| History | Keep the latest catalogue for search. Save older price records only for products selected for tracking in the first pass. |
| Offline use | Read a previously saved shopping list on the device where it was saved. No live price fetch or remote AI while offline. |
| Reminders and alerts | First support messages inside the active app. Background device messages and cloud notifications are future work. |
| Chat | Answer from checked price results. Explain historical comparisons only where records support them. Do not predict a future price. |
| List sharing | Export a chosen list by link or file; import it as a separate local copy. Implement later in the baseline. |
| Other tools | Jetty, PostgreSQL, and a separate cache are backup options. Do not add them to the baseline without a reason and team review. |

Spring Boot is chosen. Its current version is recorded in the build file and setup guide. This plan does not request a framework upgrade.

## 2. Meaning of the scope marks

Use the meanings in [project scope](../project-scope.md#1-how-to-read-the-decisions). Do not define a second legend here.

In particular, `!` limits a feature to explicitly tracked items. It does not mean every such feature needs a long history. A target-price check and source label can work with one current record; a price-drop check needs an earlier record.

Unmarked conditional rows stay conditional. They are not automatically excluded or approved. Voice remains future work under R19, not promised work in a later baseline phase.

## 3. Latest catalogue versus history

These are two separate jobs:

- The latest catalogue supports search and current listed-price comparison.
- Tracked history keeps selected products' older prices, separated by exact product, pack, retailer, and recorded date.
- Selecting a category may later select several products, but must not turn different packs into one price series.
- Saving a wishlist item is not proof of purchase. Tracking must be an explicit choice.
- Removing a tracked item stops future collection. Data retention and re-addition rules must be set before history work is assigned.

The checked file was about 2.74 MB with 2,736 products and 7,818 price entries. This does not prove that all-product history is impossible. It also does not measure database growth over time. Selected tracking is a first-baseline choice to keep the work small.

The checked endpoint returns the full file. Do not assume the provider supports downloading only selected products. Download the current catalogue, then select history records locally.

## 4. Local and offline behaviour

The local Java app must run for price imports, alert checks, and reminders. If it is closed or the computer is asleep, those jobs do not run. At the next start, check whether an update is needed. Keep the last good data if the update fails.

The first offline feature is read-only shopping-list review. It must work on the device that saved the list. H2 on a home computer alone does not supply a phone's offline list; that use needs a saved page and list on the phone, or a readable exported copy.

Do not show cached amounts as fresh prices. An offline list may show saved amounts only with their saved date. Price refresh and remote chat are unavailable offline. Offline editing and background notification delivery are later work.

## 5. Work that remains conditional

Do not use a task list to silently approve R04, R06, R11, R16, R20, R24, R29, R35, R38, R40, R43, or R44. Their scope conditions still apply.

A required feature can be scheduled later without becoming optional. Lists, budgets, sharing, languages, easy-to-read display, privacy controls, and saved-list viewing remain in the scope.

List-transfer format is not a decision needed for the first search-to-list flow. Choose it when the sharing job starts.

## 6. Current evidence

J01-J04 are implemented: H2 file storage, sample and daily import, search, exact-product comparison, and a saved list with quantities and checked state. See [the implementation record](../implementation-baseline-j01-j04.md). The app still does not call an AI service or deliver alerts.

The live JSON download and basic field counts were checked on 17 October 2026. This is source inspection, not a completed app connection or a performance result. See the [sample check](data-sample-review.md).
