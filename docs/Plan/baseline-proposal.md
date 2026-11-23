# Baseline Decisions

These are stable technical safeguards. Feature
selection belongs to the [project scope](../project-scope.md), delivery order to
the [job plan](features.md), and implemented interfaces to the
[system guide](../guide/system-guide.html).

## App and storage

Use one local Spring Boot app for one user, with no account, sign-in, or cloud
saving. Normal H2 file storage preserves records across restarts. Memory-only
storage is for isolated checks. Jetty, PostgreSQL, and a separate cache are backup
options; adding them needs a clear reason and team review. This plan requests no
framework upgrade.

## Catalogue and selected history

Keep the latest accepted catalogue for search. Retain older observations only
for tracked exact packs and retailers. Saving a pack to Wishlist starts
tracking. Cart choices and purchases have separate meanings. Stopping tracking
stops collection and keeps retained observations.

The source supplies the full catalogue. Selecting products for local history
does not establish a selective-download API. All-product history is not proven
impossible; storage growth and performance have not been measured. See the dated
[source inspection](data-sample-review.md) for evidence.

## Local and offline behaviour

Automatic live refresh runs on first use each Hong Kong day. A closed app
cannot collect prices or deliver notifications. Alerts and reminders appear
inside the active app. A failed import keeps the last accepted catalogue.

Download the offline Cart as a standalone HTML checklist with saved amounts
and retrieval time. Edits apply to the open file. It can be opened on a device
without the app or network.

## Scope safeguards

Use the scope's marks and meanings. In particular, `!` means explicitly tracked;
a target can use one current price, while a price drop needs an earlier comparable
observation. Conditional R04, R06, R11, R16, R20, R24, R29, R35, R38, R40, R43,
and R44 remain conditional. Voice under R19 remains future work. Scheduling a
required feature later does not make it optional.

R32 uses versioned JSON to transfer a chosen list as a separate local copy.
Conditional historical explanations rely on recorded evidence. Actual spending
or savings needs purchase evidence.

For current alert behavior, read the
[Alerts implementation guide](../implementation-alerts.md).
For observed results, use the [test catalogue](../Test/test-case-catalog.md)
and its dated history. This decisions file does not duplicate those results.
