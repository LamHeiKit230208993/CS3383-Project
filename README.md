# AI_3383: Shopping Price Helper

A group project that uses Hong Kong price data to help people compare products, track prices, and plan shopping. The course focuses on using AI in software engineering.

The original idea was a price-comparison app with a chat helper. The team has since selected household lists, budgets, reminders, language support, and offline use as well. Their exact decisions are recorded in the scope.

## Find the right document

| Task | Start here |
|---|---|
| Run the app or its checks | [Setup guide](docs/SETUP.md) |
| Understand the product and requirements | [Product description](docs/product-description.md), then [project scope](docs/project-scope.md) |
| Check agreed technical safeguards | [Baseline decisions](docs/Plan/baseline-proposal.md) |
| Understand the application | [System design guide](docs/guide/system-guide.html), [OO patterns](docs/guide/oo-patterns.html), and [UML index](docs/diagrams/README.md) |
| Assign jobs or check dependencies | [Job plan](docs/Plan/features.md) |
| Work on J05–J08 | [Milestone execution plan](docs/Plan/tracking-alerts-budgets-reminders-j05-j08.md); D01–D07 were agreed on 6 November 2026 |
| Check data limits and wider design proposals | [Feasibility review](docs/feasibility-review-simple.md), [source inspection](docs/Plan/data-sample-review.md), and [architecture](docs/architecture-options.md) |
| Review test coverage or past results | [Test strategy](docs/Test/strategy.md), [current catalogue](docs/Test/test-case-catalog.md), and [dated history](docs/Test/history.md) |

The [original requirements archive](docs/Eric/archive/README.md) is historical
evidence, not current implementation guidance.

## Current milestone

J01–J12 are merged into `main`. Market Desk provides local catalogue import,
price comparison, Cart, Wishlist, alerts, Ask, list sharing, an offline
checklist, and display and personal data settings. See the
[release summary](release/summary.md) and [dated test results](docs/Test/history.md).
The [project scope](docs/project-scope.md) records agreed feature decisions.

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
