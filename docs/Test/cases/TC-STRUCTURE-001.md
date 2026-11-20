# TC-STRUCTURE-001 — One list read and shared totals

The list page must use one set of saved items for both rows and totals. Reading
items must fetch their products, listings, and offers without per-item queries.

Automated checks:

- `ShoppingServiceTest.listLoadsProductsAndOffersInOneQuery` saves two products,
  reads them outside an enclosing test transaction, and checks one prepared SQL
  statement. It checks that the returned listings and offers are populated.
- `HelloControllerModelTest.listUsesOneReadForRowsAndSummary` verifies one list
  read and checks that the budget uses the same known subtotal, unknown item
  count, and total quantity.
- `HelloControllerModelTest.discoverSharesOneCartReadAcrossPreviewAndBudget`
  verifies that Discover reads the Cart once and uses that same data for the
  preview, full amount and budget. It shows three of four Cart lines.
- The other model tests verify that Data, Planned, and list-add requests do not
  read an unused shopping summary.

```powershell
.\mvnw.cmd '-Dtest=ShoppingServiceTest,HelloControllerModelTest' test
```

This checks the query contract; it is not a latency benchmark or a load test.
