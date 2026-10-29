# AI_3383: Shopping Price Helper

A group project that uses Hong Kong price data to help people compare products, track prices, and plan shopping. The course focuses on using AI in software engineering.

The original idea was a price-comparison app with a chat helper. The team has since selected household lists, budgets, reminders, language support, and offline use as well. Their exact decisions are recorded in the scope.

## Find the right document

| Task | Start here |
|---|---|
| Run the app or its checks | [Setup guide](docs/SETUP.md) |
| Understand the product and requirements | [Product description](docs/product-description.md), then [project scope](docs/project-scope.md) |
| Check agreed technical safeguards | [Baseline decisions](docs/Plan/baseline-proposal.md) |
| Understand the implemented baseline | [J01–J04 implementation](docs/implementation-baseline-j01-j04.md) and [accepted UML](docs/diagrams/baseline/README.md) |
| Assign jobs or check dependencies | [Job plan](docs/Plan/features.md) |
| Build J05–J08 | [Milestone execution plan](docs/Plan/tracking-alerts-budgets-reminders-j05-j08.md); resolve the relevant decision gates first |
| Check data limits and wider design proposals | [Feasibility review](docs/feasibility-review-simple.md), [source inspection](docs/Plan/data-sample-review.md), and [architecture](docs/architecture-options.md) |
| Review test coverage or past results | [Current catalogue](docs/Test/test-case-catalog.md) and [dated history](docs/Test/history.md) |

The [original requirements archive](docs/Eric/archive/README.md) is historical
evidence, not current implementation guidance.

## Current milestone

PR #15 is merged. J01–J04 provide the local import, search, comparison, and saved-list baseline. The latest recorded checks passed 47 Java tests and 9 browser scenarios; see the [test history](docs/Test/history.md#pr-15-review-regressions--25-october-2026). These are recorded results, not a new run on 29 October.

The team has grouped **J05–J08 into the next milestone issue**. Its implementation is planned, not complete. The milestone plan is the execution reference; the project scope remains the authority for feature decisions.

## Share the plan with teammates

Publish the reviewed plan on `origin/docs/john`. That branch includes the merged
application baseline as well as documentation. Use it as the shared planning
reference until a documentation PR merges the changes into `main`.

Link the [milestone plan](docs/Plan/tracking-alerts-budgets-reminders-j05-j08.md)
from the J05–J08 issue. Keep the issue's checklist and ownership there; keep
contracts, decision answers, and acceptance rules in the document. Record
decisions through reviewed commits so teammates and agents read the same rules.

To obtain the shared branch, teammates can run `git fetch origin`, then inspect
`origin/docs/john`. To bring it into their own working branch, commit or stash
local edits and run `git merge origin/docs/john`. Review and resolve any conflicts.
Do not reset or replace a teammate's feature branch. Use `main` for the same flow
after the documentation PR merges. Implementation agents should read the linked
milestone plan from the checked-out commit, not an older copied prompt.

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
