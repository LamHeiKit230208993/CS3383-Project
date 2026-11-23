# AI_3383: Shopping Price Helper

A group project that uses Hong Kong price data to help people compare products, track prices, and plan shopping. The course focuses on using AI in software engineering.

## Find the right document

| Task | Start here |
|---|---|
| Run the app or its checks | [Setup guide](docs/SETUP.md) |
| Understand the product and requirements | [Product description](docs/product-description.md), then [project scope](docs/project-scope.md) |
| Check agreed technical safeguards | [Baseline decisions](docs/Plan/baseline-proposal.md) |
| Understand the application | [System design guide](docs/guide/system-guide.html), [OO patterns](docs/guide/oo-patterns.html), and [UML index](docs/diagrams/README.md) |
| Find a feature or its completion checks | [Job and feature index](docs/Plan/features.md) |
| Maintain Alerts or daily refresh | [Alerts and refresh implementation](docs/implementation-alerts.md) |
| Check data limits | [Feasibility review](docs/feasibility-review-simple.md) and [source inspection](docs/Plan/data-sample-review.md) |
| Review test coverage or past results | [Test strategy](docs/Test/strategy.md), [current catalogue](docs/Test/test-case-catalog.md), and [dated history](docs/Test/history.md) |

The [original requirements archive](docs/Eric/archive/README.md) is historical
evidence, not current implementation guidance.

## Current application

Market Desk provides catalogue import, price comparison, Cart, Wishlist,
unified Alerts, Ask, list sharing, an offline checklist, and display and
personal data settings. See the
[release summary](release/summary.md) and [dated test results](docs/Test/history.md).
The [project scope](docs/project-scope.md) records agreed feature decisions.

## Share the plan with teammates

Use the reviewed commit linked from the issue or pull request for implementation
work. Keep ownership and the checklist in the issue. Record changed contracts
and acceptance rules in the linked documentation.

The four [user-needs files](docs/product-description.md#user-needs) explain requests and their reasons. They include needs that the team has excluded or left for the future.

## Document rules

- The project scope is the main record of feature decisions. A user request is not automatically a build commitment.
- Use feature IDs when referring to requirements. Keep the IDs stable when changing wording.
- Keep team choices separate from technical evidence. “Build” does not mean “already tested”.
- Mark unknown facts and unresolved choices clearly. Do not fill them with assumptions.
- Use short sentences. Define technical terms when needed. Keep examples that explain a need or rule.
- Update the scope and affected references together when the team changes a decision.

Data source: [Consumer Council Online Price Watch](https://data.gov.hk/en-data/dataset/cc-pricewatch-pricewatch).
