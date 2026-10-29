# Baseline Decisions

Updated: 29 October 2026. These are stable technical safeguards. Feature
selection belongs to the [project scope](../project-scope.md), delivery order to
the [job plan](features.md), and implemented interfaces to the
[implementation guide](../implementation-baseline-j01-j04.md).

## App and storage

Use one local Spring Boot app for one user, with no account, sign-in, or cloud
saving. Normal H2 file storage preserves records across restarts. Memory-only
storage is for isolated checks. Jetty, PostgreSQL, and a separate cache are backup
options; adding them needs a clear reason and team review. This plan requests no
framework upgrade.

## Catalogue and selected history

Keep the latest accepted catalogue for search. Retain older observations only
for explicitly tracked exact products/packs and retailers in the first history
pass. Wishlist, shopping-list, tracking, and purchase records have distinct meanings.
Stopping tracking stops collection; retention and re-enabling rules must be
agreed before dependent implementation.

The source supplies the full catalogue. Selecting products for local history
does not establish a selective-download API. All-product history is not proven
impossible; storage growth and performance have not been measured. See the dated
[source inspection](data-sample-review.md) for evidence.

## Local and offline behaviour

J01–J04 refreshes only on an explicit sample-load or live-refresh request.
Startup does not fetch prices. Later scheduling needs agreed rules and tests;
a closed app or sleeping computer cannot collect prices or deliver notifications.
First alerts and reminders appear inside the active app. Keep the last accepted
data after a failed import.

First offline use is read-only list review on the device that saved the data.
H2 on a home computer alone does not make a phone work offline. The phone needs
its own saved page/list or readable exported copy. Label saved amounts and dates;
live refresh and remote chat are unavailable offline. Offline editing and
background/cloud notifications remain later work.

## Scope safeguards

Use the scope's marks and meanings. In particular, `!` means explicitly tracked;
a target can use one current price, while a price drop needs an earlier comparable
observation. Conditional R04, R06, R11, R16, R20, R24, R29, R35, R38, R40, R43,
and R44 remain conditional. Voice under R19 remains future work. Scheduling a
required feature later does not make it optional.

R32 transfers a chosen list as a separate local copy. Transfer format is a J10
decision and does not block J05–J08. Conditional historical explanations are not
future-price predictions. Actual spending or savings needs purchase evidence.

For the next issue, read the
[J05–J08 milestone plan](tracking-alerts-budgets-reminders-j05-j08.md).
For observed results, use the [test catalogue](../Test/test-case-catalog.md)
and its dated history. This decisions file does not duplicate those results.
