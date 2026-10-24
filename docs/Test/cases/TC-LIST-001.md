# TC-LIST-001 â€” List edits persist and reject bad input

| Field | Details |
|---|---|
| Test class | `ListFlowTest` |
| Test methods | `addItemIsStoredAndSurvivesReload()`, `addingTheSameProductTwiceAddsQuantityNotRows()`, `invalidQuantityDoesNotChangeTheList()`, `unknownProductIsRejected()`, `togglePersistsCheckedState()`, `changingQuantityPersists()`, `togglingAMissingItemLeavesTheListAlone()`, `removeDeletesTheRow()`, `removingAMissingItemLeavesTheListAlone()`, `removingTheLastItemLeavesAnEmptyList()` |
| Type | Full context test driving the real POST routes through `MockMvc` |
| Routes | `POST /list/items`, `/list/items/{code}/quantity`, `/list/items/{code}/toggle`, `/list/items/{code}/remove` |

## Purpose

Verify the J04 list rules: quantities, checked state, and safe rejection of
bad input.

## Steps

1. POST an item with quantity 2, then read the list store.
2. POST the same product again with quantity 3.
3. POST quantity `0` and an unknown product code.
4. Tick an item, then untick it.
5. Change a quantity to 5.
6. Remove one product, then remove an unknown code, then remove the last item.

## Expected result

- The item is stored with its code, quantity 2, and `checked = false`, and
  redirects to `/list`.
- The repeat add produces one row with quantity 4, not two rows.
- The zero quantity redirects to `/list` and leaves the list unchanged; an
  unknown code redirects to `/` and also changes nothing.
- Ticking and unticking both persist across a re-read.
- The quantity change persists as 5.
- Toggling an item that is not on the list leaves the list unchanged.
- Remove deletes only the named row; an unknown code changes nothing; removing the last row leaves an empty list.

## Limits

Uses the in-memory test database. The restart case is covered by the manual
browser check recorded in the catalog, not by this class.

Source: [`ListFlowTest.java`](../../../src/test/java/com/pricewatch/web/ListFlowTest.java)
