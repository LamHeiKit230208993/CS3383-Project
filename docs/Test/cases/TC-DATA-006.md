# TC-DATA-006 Daily live price refresh

## Purpose

Verify that Market Desk shares one feed download between daily and manual
refreshes and preserves a usable catalogue after a source failure.

## Setup

Run the [DailyCatalogueRefreshTest](../../../src/test/java/com/pricewatch/catalog/DailyCatalogueRefreshTest.java)
with isolated H2, a controllable clock, and a stubbed Consumer Council source.
The test calls the production coordinator and transactional catalogue import.
It uses a fresh coordinator against the same H2 database to model application
restart. The normal test profiles disable background jobs with
`pricewatch.jobs.enabled=false` and exercise the daily queue explicitly.

## Steps and expected results

| Step | Expected result |
| --- | --- |
| Open a workspace at 23:59 Hong Kong time and request the daily check repeatedly, including from a fresh coordinator. Check again at 00:01 the next day. | One live fetch before midnight and one after midnight. |
| Keep yesterday’s accepted catalogue and make today’s feed unavailable. Reopen and then manually retry with a working feed. | Automatic failure keeps the catalogue and persists across restart. A manual success clears the failure and satisfies the daily check. |
| Reopen after an interrupted attempt. Repeat with an accepted live import saved for that day. | The interrupted attempt does not run again. A committed live import clears the warning even when the attempt state still says started. |
| Load a sample and then a demo catalogue. Open each while a live feed is available. | No automatic fetch overwrites either catalogue. Manual live refresh resumes automatic scheduling. |
| Block the automatic feed, issue a manual live refresh and request a local sample import. Release the feed. | Both live callers share the result of one download. The local sample import remains the accepted catalogue after it finishes. |

The [HelloControllerTest](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
also checks the schedule description shown in Price data.
[DailyRefreshPagesTest](../../../src/test/java/com/pricewatch/web/DailyRefreshPagesTest.java)
checks the application startup and page entrypoints and the failure notice.
