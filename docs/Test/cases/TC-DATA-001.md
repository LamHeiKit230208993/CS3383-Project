# TC-DATA-001 — File database reopen

Source: [`CatalogPersistenceTest`](../../../src/test/java/com/pricewatch/catalog/CatalogPersistenceTest.java).

The test creates an H2 file under a temporary directory. It starts the application
without a web server, saves a product with multilingual labels and a list item
with quantity 3 and checked state true, and closes the application context.
It opens a second context against the same file and reads those records.

Expected: the same source code, Traditional Chinese name, quantity, and checked
state are present. This is an actual file-database reopen, not two reads from a
shared memory database. It does not by itself prove the browser restart flow.
