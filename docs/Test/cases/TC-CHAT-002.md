# TC-CHAT-002 · Shopping actions in Ask

Use a mocked provider, one local session, and an isolated Cart. Submit a turn
with a prepared add. Fail the first provider call after the preparation.
Verify it saves nothing, then retry to completion and verify one saved add.

Prepare a removal. Verify its preview is pending and its token cannot be
confirmed from another conversation. Confirm twice and verify one removal
and one receipt. Use a separate pending removal to test cancellation.

In Chrome, run the shopper workflow with the local catalogue and provider
fixture. Create a named list from Ask, add two exact milk packs, and confirm
the main Cart stays empty. Open a named Cart summary on mobile, follow an
item link, and return to the same Ask message. Check the Wishlist card and
the HK$30.00 result for 15% of HK$200. Cancel and then confirm removal of the
named list item. Provide a calendar reminder's missing schedule in a second
turn and confirm it appears in Current rules after approval. Delete the named
list through a separate confirmation. The fixture supplies the model's tool
calls without an external provider account.

Sources [AskTurnTest](../../../src/test/java/com/pricewatch/web/AskTurnTest.java)
and [shopper browser checks](../../../src/test/browser/market-desk.cjs).
