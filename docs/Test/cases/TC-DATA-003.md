# TC-DATA-003 — Bounded source download

Source: [`ConsumerCouncilSourceClientTest`](../../../src/test/java/com/pricewatch/catalog/ConsumerCouncilSourceClientTest.java).

Start a local HTTP test server with success, HTTP 503, and oversized-body routes.
The client must return the successful bytes exactly and reject both the failure
status and a response larger than 10 MiB.

This verifies transport handling without depending on the live provider. The
catalogue integration suite separately injects successful source bytes and a
fetch failure, checking that a failed refresh retains the accepted checksum and
catalogue. These checks do not prove live TLS, provider availability, or runtime
performance. Record any real live-refresh attempt separately.
