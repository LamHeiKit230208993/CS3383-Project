# TC-CHAT-002 · Shopping actions in Ask

Use a mocked provider, one local session, and an isolated Cart. Submit a turn
with a prepared add. Fail the first provider call after the preparation.
Verify it saves nothing, then retry to completion and verify one saved add.

Prepare a removal. Verify its preview is pending and its token cannot be
confirmed from another conversation. Confirm twice and verify one removal
and one receipt. Use a separate pending removal to test cancellation.

## Confirmation guard and recovery

In a fresh session, prepare a request to remove one pack from a named Cart
with two packs. Exercise the guard **A or B**, where **A** means there is no
matching pending action and **B** means the conversation is waiting for a reply.

| Case | Action | Conversation | A | B input | Result |
|---|---|---|---|---|---|
| C1 | Matching pending action | Ready | False | False | Confirm and save one pack |
| C2 | Unknown action ID | Ready | True | False | Redirect and keep two packs |
| C3 | Matching pending action | Waiting | False | True | Redirect and keep two packs |

C1 and C2 exercise A independently. C1 and C3 exercise B independently.
The rejected action in C2 leaves B unevaluated. An invalid decision with
a valid pending action and a ready conversation returns HTTP 400 and
preserves both packs.

Change the pending Cart quantity to four after preparing a removal. The
original confirmation must leave all four packs saved and show a new preview.
A confirmation of the new action removes one pack and keeps the pinned retailer.
Simulate a failed write at the shopping service boundary and verify that Ask
marks the action failed, produces no success receipt, and ignores a second
submission of that action. The failure case tests the controller response
with a mock. The stale and guard cases use real H2 storage.

## Browser workflow

In Chrome, run the shopper workflow with the local catalogue and provider
fixture. Create a named list from Ask, add two exact milk packs, and confirm
the main Cart stays empty. Open a named Cart summary on mobile, follow an
item link, and return to the same Ask message. Check the Wishlist card and
the HK$30.00 result for 15% of HK$200. Cancel and then confirm removal of the
named list item. Provide a calendar reminder's missing schedule in a second
turn and confirm it appears in Current rules after approval. Delete the named
list through a separate confirmation. The fixture supplies the model's tool
calls without an external provider account.

Sources [AskTurnTest](../../../src/test/java/com/pricewatch/web/AskTurnTest.java),
[HelloControllerTest](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
and [shopper browser checks](../../../src/test/browser/market-desk.cjs).
