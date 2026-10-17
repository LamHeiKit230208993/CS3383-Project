# Market Desk UI prototype

This version is built on `dev/john` for comparison with the other team designs.
It is a screen prototype for issue #7. It does not complete J01–J04 or prove
that the product features work with real data.

## Theme choice

Market Desk uses white and light-grey surfaces, deep-blue main actions, green
price cues, and clear sans-serif type. Large prices and visible pack sizes help
users compare the same item. Text and symbols support colour cues.

A persistent **UI prototype · Sample data** banner explains the limits. Desktop
uses a sidebar. Phone navigation has at most five links: Discover, List, Saved,
Updates, and Ask. Product cards lead to Compare.

## Screen flows

- Discover: search sample products, filter results, and read a no-results state.
- Compare: read the exact product and pack, retailer prices, tied prices, offer
  conditions, and sample date. Choose a quantity and add the item to the list.
- List: change quantities, check items, and compare the estimated list cost
  with a planned budget. The amount is not actual spending.
- Saved: distinguish wishlist choices from price tracking.
- Updates: inspect target and price-drop alerts, reminder settings, quiet hours,
  snooze, and a sample morning summary inside the app.
- History: inspect a labelled sample chart and an insufficient-history state.
- Ask: try sample questions, a price answer with its data reference, and a
  request to clarify the product.
- Settings: inspect language choices and test the high-contrast preview.

## Prototype limits

All product, price, history, alert, and assistant content is sample data. Demo
state stays in browser memory and resets on reload. There is no database,
price import, live AI call, notification service, account, or cloud saving.
Settings and demo actions do not establish full language or offline support.

## Run and evaluate

Use the commands in [the setup guide](../SETUP.md) to start Spring Boot. Open
the local root page. There is no separate frontend build.

1. Search, compare an exact product, choose a quantity, and add it to the list.
2. Change a quantity and check an item. Confirm the total is an estimate.
3. Visit Saved, Updates, Ask, History, and Settings through the visible links.
4. Try a search with no result and inspect the limited-history example.
5. Use only the keyboard. Check that focus stays visible, dialogs can close,
   and controls have useful names.
6. Check phone, tablet, and desktop widths. Check that the page does not scroll
   sideways and that phone navigation does not hide content.
7. Test high contrast and reduced motion. Read chart values without using
   colour alone.
8. Reload. Confirm temporary state resets and the sample-data label remains.

Choose a theme based on task clarity, readable prices and pack sizes, simple
navigation, phone use, and consistent feedback. Record browser checks and
automated test results separately; this plan does not claim that they passed.
