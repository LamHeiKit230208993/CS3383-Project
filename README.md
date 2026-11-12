# AI_3383: Shopping Price Helper

A group project that uses Hong Kong price data to help people compare products, track prices, and plan shopping. The course focuses on using AI in software engineering.

The original idea was a price-comparison app with a chat helper. The team has since selected household lists, budgets, reminders, language support, and offline use as well. Their exact decisions are recorded in the scope.

## Find the right document

| Task | Start here |
|---|---|
| Run the app or its checks | [Setup guide](docs/SETUP.md) |
| Understand the product and requirements | [Product description](docs/product-description.md), then [project scope](docs/project-scope.md) |
| Check agreed technical safeguards | [Baseline decisions](docs/Plan/baseline-proposal.md) |
| Understand the application | [J01–J04 baseline](docs/implementation-baseline-j01-j04.md), [J05–J08 extension](docs/implementation-j05-j08.md), and [UML index](docs/diagrams/README.md) |
| Assign jobs or check dependencies | [Job plan](docs/Plan/features.md) |
| Work on J05–J08 | [Milestone execution plan](docs/Plan/tracking-alerts-budgets-reminders-j05-j08.md); D01–D07 were agreed on 6 November 2026 |
| Check data limits and wider design proposals | [Feasibility review](docs/feasibility-review-simple.md), [source inspection](docs/Plan/data-sample-review.md), and [architecture](docs/architecture-options.md) |
| Review test coverage or past results | [Current catalogue](docs/Test/test-case-catalog.md) and [dated history](docs/Test/history.md) |

The [original requirements archive](docs/Eric/archive/README.md) is historical
evidence, not current implementation guidance.

## Current milestone

PR #15 is merged. J01–J04 provide the local import, search, comparison, and saved-list baseline. The 25 October baseline checks passed 47 Java tests and 9 browser scenarios; see the [test history](docs/Test/history.md#pr-15-review-regressions--25-october-2026). Those results do not verify later changes.

**J05–J08 are implemented locally under issue #17.** This branch adds
Saved products and retailer tracking, in-app alerts, planned budgets, and
replenishment reminders. Validation on 6 November passed 86 Java tests, 9 baseline
and 11 milestone browser scenarios, and an old-to-new H2 upgrade check. The
changes await commit/PR review and merge; the issue remains open. Use the
[implementation guide](docs/implementation-j05-j08.md) for routes and behaviour,
and the [test catalogue](docs/Test/test-case-catalog.md) for verification evidence.
The milestone plan records the agreed rules; the project scope remains the
authority for feature decisions. J09–J12 remain later work.

## Share the plan with teammates

Use the reviewed commit linked from the milestone issue as the shared planning
reference. The earlier plan was shared on `origin/docs/john`; the current
implementation is not automatically available there. Publish new work through
the team's normal reviewed PR process before asking teammates to use it.

Link the [milestone plan](docs/Plan/tracking-alerts-budgets-reminders-j05-j08.md)
from the J05–J08 issue. Keep the issue's checklist and ownership there; keep
contracts, decision answers, and acceptance rules in the document. Record
decisions through reviewed commits so teammates and agents read the same rules.

To obtain published work, run `git fetch origin`, then inspect the branch and
commit linked from the issue or PR. Commit or stash local edits before merging
that reviewed branch into a working branch. Review and resolve any conflicts.
Do not reset or replace a teammate's feature branch. Implementation agents
should read the milestone plan from the checked-out commit, not an older copied
prompt. Use `main` once the relevant PR has merged.

The four [user-needs files](docs/product-description.md#user-needs) explain requests and their reasons. They include needs that the team has excluded or left for the future.

## Document rules

- The project scope is the main record of feature decisions. A user request is not automatically a build commitment.
- Use feature IDs when referring to requirements. Keep the IDs stable when changing wording.
- Keep team choices separate from technical evidence. “Build” does not mean “already tested”.
- Mark unknown facts and unresolved choices clearly. Do not fill them with assumptions.
- Use short sentences. Define technical terms when needed. Keep examples that explain a need or rule.
- Update the scope and affected references together when the team changes a decision.

Data source: [Consumer Council Online Price Watch](https://data.gov.hk/en-data/dataset/cc-pricewatch-pricewatch).

---

Setup & run instructions: [docs/SETUP.md](docs/SETUP.md)
