# User Needs: Household Shopper

[Request source and shared terms](../product-description.md) · [Team decisions](../project-scope.md)

## Situation

One person plans most household shopping, but a partner may do the buying. Fresh goods are bought often; rice, oil, and cleaning products last longer. The household wants to control spending and avoid forgotten or duplicate purchases.

## Requests

- “Let me reuse a weekly list and change the quantities.”
- “Show the estimated total as I add items, and warn me before I pass my budget.”
- “Keep actual amounts paid separate from advertised prices.”
- “Let my partner add items and mark them bought. Show who changed the list and who is shopping.”
- “Do not replace a product we need with another brand just because it is cheaper.”
- “Remind me when regular items are likely to run out. Let me delay a reminder if we still have enough.”
- “Let a reminder add the correct product to my shopping list.”
- “Keep the saved list usable when the shop has a poor connection.”
- “Show changes in the cost of our regular basket. Help me judge whether to buy now or wait.”
- “Consider storage space and expiry before suggesting larger quantities.”

## Problems to handle

- Two people mark or change the same item while shopping. One person's change must not silently erase the other's.
- One person edits offline while the other edits online. The app needs an agreed rule when they reconnect.
- A child needs a specific product. An alternative brand is not automatically an acceptable replacement.
- Marking an item “bought” may not record its actual paid price. Budget and savings displays must use the right type of record.
- A reminder assumes that an item is nearly used up, but the household still has some. Snooze or revised use information must affect the next reminder.
- Price history has only a few days. It cannot support a claim about the lowest price in three months.

## Scope links

| Request group | Team decision |
|---|---|
| Recurring lists, estimates, budgets, categories, and sharing | Build: R27–R28, R30–R32. R32 transfers a list by link or file and imports a separate local copy; live shared editing and handoff are not included. |
| Use-based reminders, add-to-list, clear, and snooze | Build: R34, R36–R37; the use input and next-date rules remain open |
| Offline use and personal-data controls | Build: R45, R48. First offline use is read-only saved-list review; offline editing is later work. |
| Basket-cost changes, actual savings, buy/wait advice | Conditional: R29, R43, R16, R35; R35 is tracked-only |
| Stock checks and bulk/storage advice | Excluded: R07, R17, R33 |

Brand restrictions and household examples explain why substitutions need care. They do not add automatic health or allergy advice to the project.

The requests above retain the household's wishes. The team chose one local user, no account, and no cloud saving. Partners must send a new export to pass later changes; the app does not combine their copies.
