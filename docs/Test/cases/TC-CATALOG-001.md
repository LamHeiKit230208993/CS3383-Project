# TC-CATALOG-001 — Import, multilingual search, and exact comparison

Source: [`CatalogServiceIntegrationTest`](../../../src/test/java/com/pricewatch/catalog/CatalogServiceIntegrationTest.java).

Import the checked sample twice. Expect 2 products, 8 listings, and 4 offers;
repeated import does not append copies. Source date stays null. Retrieval time
and checksum are accepted-import metadata.

Search the English, Traditional Chinese, and Simplified Chinese full milk names.
Each must return `P000000124`. Compare constructed records for two different pack
codes. They must remain separate, both tied lowest retailers must be visible,
and a null price must never win the comparison.

After a valid import, attempt an empty catalogue. Expected: an import error,
the same accepted checksum, and the same readable catalogue. A separate test
supplies successful live-source bytes, then injects a fetch failure. The accepted
checksum and catalogue must remain unchanged after failure. HTTP transport is
covered in TC-DATA-003. A parser rejection alone does not prove database rollback.


`databaseWriteFailureRollsBackDeletedCatalogueAndKeepsMetadataAndList` checks
recovery from a real database write failure. It first commits the checked sample
and a shopping item with JASONS selected, quantity 2, and checked state true.
It snapshots the persisted catalogue, metadata, and shopping list.

A temporary H2 CHECK constraint rejects a designated test retailer. Valid source
JSON then changes an existing product and adds a new one. Replacement deletes
old offers/listings and updates product records before inserting the rejected
listing. The test requires a `DataIntegrityViolationException` from that exact
constraint, rather than an input-validation error or a mocked repository failure.

After the failed service transaction, fresh reads must match the complete prior
catalogue, metadata, and list. The test also checks the original row counts and
that the new product was rolled back. There is no enclosing test transaction.
The temporary constraint is removed in a `finally` block. Normal app constraints
and production code are unchanged.
