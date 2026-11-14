# TC-ALERT-001 — Target and drop alerts, quiet hours, snooze, summary

This case covers J06 in-app alerts. Dated execution results are recorded
separately in the test history.

Source: [`AlertServiceTest`](../../../src/test/java/com/pricewatch/alerts/AlertServiceTest.java).

A TARGET rule fires when the current price is at or below the target (D04). It is
checked when the rule is created and after each valid import. A target is met from
a single known current observation.

A DROP rule needs an earlier distinct observation for the same product/retailer
and fires when the latest price is strictly lower than the most recent distinct
prior price. With only identical repeated observations, a drop does not fire.

Repeat prevention: the same observation never fires twice (tracked by
`lastFiredObservationId` and a rule/observation uniqueness check). Repeated
imports do not duplicate events.

Quiet hours (22:00–07:00 HKT) store events but hide them until 07:00 (D05). The
morning summary covers events since the latest 08:00 boundary. Snooze hides an
event for 8 hours. A closed-app period is never reported as a delivered
notification. Tests use a fixed clock.
