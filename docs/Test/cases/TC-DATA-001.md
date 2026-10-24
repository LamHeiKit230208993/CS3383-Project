# TC-DATA-001 — Stored data is read back

| Field | Details |
|---|---|
| Test class | `CataloguePersistenceTest` |
| Test methods | `savedCatalogueCanBeReadBack()`, `missingPriceStaysNullInStorage()`, `savedListSurvivesReload()` |
| Type | Full application context test over an in-memory H2 database |
| Setup | `@SpringBootTest`; `src/test/resources/application.properties` points at `jdbc:h2:mem:pricewatch-test` |

## Purpose

Verify that the records agreed in J01 survive a write and a re-read, and that
an unknown price is never turned into zero by storage.

## Steps

1. Save a catalogue with one product and one retailer listing.
2. Read the catalogue back through `CatalogueStore.get()`.
3. Repeat for a listing whose price is `null`.
4. Add an item to the shopping list, tick it, save, and read the list back.

## Expected result

- The catalogue reads back with the same product code, price, and `importedAt`.
- `sourceDate` is `null` when the source provides no date.
- A `null` price reads back as `null`, and `hasPrice()` is `false`.
- The list reads back with the same product code, quantity, and checked state.

## Limits

Uses an in-memory database. It proves the mapping and the read path, not
durability of the file database across a real process restart; that was
checked by hand in the browser run recorded in the catalog.

Source: [`CataloguePersistenceTest.java`](../../../src/test/java/com/pricewatch/domain/CataloguePersistenceTest.java)
