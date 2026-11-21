# TC-ALERT-004 — Scheduled and repeating alert rules

Use isolated H2 databases, a controlled clock, and the exact-pack fixtures in
[WatchRuleServiceTest](../../../src/test/java/com/pricewatch/notifications/WatchRuleServiceTest.java).

1. Create a one-time price rule. Disable it and accept a qualifying live refresh.
   Re-enable it and accept a sample followed by a qualifying live refresh. Only the
   last refresh creates an alert. The completed rule is off. A later live refresh,
   an enable request, and saving the same rule cannot repeat that alert. Editing
   its target re-arms the rule.
2. Create a daily price rule. Accept qualifying live prices before and after one
   full daily period. Only the first qualifying refresh in each period creates
   an alert. High then low prices inside the same period cannot create another alert.
3. Create a one-time calendar reminder for a product outside Wishlist. Pause it
   until its Hong Kong due time, re-enable it, then start two due checks together.
   The reminder creates one event with a scheduled time and no price evidence.
4. Create a monthly reminder on January 31. In February expect the last day of
   the month, and in March expect the original 31st. Skip two due dates and check
   that one event records the oldest due date and the next date is in the future.
   A fortnightly rule advances by fourteen days.
5. Let a combined weekly rule come due with a price above its target. A target
   edit cannot satisfy the pending occurrence with cached data. The next qualifying
   live refresh creates one event with price evidence and its original due date.
   The next weekly occurrence uses the original calendar time.
6. Open a temporary H2 file. Create an older watch rule and price event. Restore
   the previous schema's required price and retrieval columns. Reopen the file
   and verify the old rule, arming state, and history. Schedule a calendar-only
   rule and confirm that it saves without synthetic prices or retrieval time.

The fixtures control prices, time, and database schema. Browser workflows are
recorded separately.
