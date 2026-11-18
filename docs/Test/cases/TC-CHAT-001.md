# TC-CHAT-001 — Checked product prices and provider fallback

1. Open Ask with no API key. Load the checked sample and ask where Kowloon
   Dairy's 236 mL fresh milk is cheapest. Confirm AEON and HK$7.50 come from
   the stored catalogue and the page reports that AI explanation is unavailable.
2. Ask “What about Wellcome?” in the same session. Confirm both messages
   appear and the answer retains the 236 mL pack and shows HK$12.00.
   Start a new chat, confirm its transcript starts empty, and ask for an unknown
   product. Reopen the first chat and verify that its original messages and
   same-pack answer remain. Another browser session cannot read that chat.
3. Run the local provider stub. Verify the versioned request path, test key and
   configured model. Ask an underspecified question, then give a product clue.
   The model requests clarification first, then a catalogue search. The app
   asks the shopper to choose a pack before showing the requested retailer's
   price. A follow-up uses the chosen pack. An invented price from the model
   is rejected.
4. Return an incomplete provider response while pack choices are pending.
   Verify the choices remain. Submit a question from an expired conversation
   link and verify it opens a new chat with that question. After a failed
   product search, ask for another retailer and verify the app still uses the
   last matched pack.
5. Open Ask directly in a fresh browser context. Ask “who are u” and confirm
   the assistant introduces itself. Hold the reply request after Send.
   Verify the accepted question appears and is present on a separate read
   of the conversation. Confirm the waiting indicator and type the next
   draft while the reply is still pending.
6. Simulate a failed send and verify the draft returns. Fail the reply request
   after a successful send. Verify the saved message remains, then retry the
   reply and confirm the message appears only once.

Sources: [HelloControllerTest](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
and [ChatIntentClientTest](../../../src/test/java/com/pricewatch/web/ChatIntentClientTest.java).
The [Chrome workflow](../../../src/test/browser/market-desk.cjs) also checks
message alignment, history switching, and composer clearance above navigation.
At 375 by 667 px, at least three fixture messages are readable at once.
The automated tests use fixture data. [AskTurnTest](../../../src/test/java/com/pricewatch/web/AskTurnTest.java)
also checks the waiting state before the provider runs, duplicate reply requests,
an interrupted reply, and local greetings and introductions.
