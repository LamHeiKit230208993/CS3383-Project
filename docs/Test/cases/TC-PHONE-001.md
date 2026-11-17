# TC-PHONE-001 — Open the app from a phone on the same Wi-Fi

Covers letting a phone on the same local network open the running app. It is off
by default; the server must be started bound to a non-loopback address and the
phone's host address listed in `pricewatch.allowed-hosts`. Dated execution
results are recorded separately in the test history.

Sources: [`LocalRequestGuardTest`](../../../src/test/java/com/pricewatch/web/LocalRequestGuardTest.java),
[`HelloControllerTest`](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java).

## Default (loopback only)

With the default `server.address=127.0.0.1`, only `localhost` and the loopback
address are served. Requests that use any other host name get `403` and no data
is changed.

## Enabling phone access

Start with `--server.address=0.0.0.0` and set
`PRICEWATCH_ALLOWED_HOSTS` to this computer's LAN address (for example
`192.168.1.20`). A request whose host is one of the listed addresses is served;
any other host is still rejected with `403`. The address to type on the phone is
documented in the setup guide; the Price data page no longer displays it.

## Writes from a phone

The existing origin check still applies: a write from another site, or a
cross-site flagged request, is rejected even on an allowed host. The phone must
open the app directly.
