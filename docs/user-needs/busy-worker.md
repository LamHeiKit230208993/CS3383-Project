# User Needs: Busy Worker

[Request source and shared terms](../product-description.md) · [Team decisions](../project-scope.md)

## Situation

The user shops between work and travel. They often use a phone with little time or an unreliable connection. They want a short answer that helps them act without reading a large comparison table.

## Requests

- “When I ask where my usual item is cheapest, give me a store, a price, and a short reason.”
- “If I only say 'milk', ask one useful question or use a preference I have actually saved.”
- “Remember brands or stores I do not want, if I choose to provide that information.”
- “Give me a short morning summary. Do not interrupt meetings with repeated messages.”
- “Let me see and remove the personal information the app stores.”
- “Let me speak in Cantonese or English while walking, and answer quickly.”
- “Eventually, help me buy with fewer steps and consider stores on my route.”
- “Let me look at saved information when my connection is poor.”

The earlier examples used an 8 a.m. summary and an answer within five seconds. These express the user's wish for speed and timing; they are not agreed delivery settings or measured performance.

## Problems to handle

- “My usual milk” has no recorded purchase or selected preference behind it. Ask for a choice rather than guess.
- The lowest listed price is at an inconvenient store. Without route data, the app cannot call it the most convenient choice.
- A list of saved favourites is not a purchase history.
- A stale saved price is available offline. Show its age and do not imply that a fresh lookup succeeded.
- A price answer is short but omits the pack size. The user could act on the wrong item.

## Scope links

| Request group | Team decision |
|---|---|
| Price chat, clarification, quiet hours, summary, personal-data controls | Build with stated limits: R15, R18, R23, R26, R45 |
| Saved offline information | Build: R48; remote AI calls still need a connection |
| Voice and the five-second target | Future: R19 |
| Nearby-store alerts, direct buying, and routes | Future: R25, R42 |

A separate “never recommend” preference feature was requested in the original profile but is not separately selected in the scope. Decide it explicitly before adding that behaviour. The user's preference for voice does not make voice a current build requirement.
