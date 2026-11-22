# TC-CHAT-001 — DeepSeek conversation and checked catalogue tools

Use an isolated database and the provider fixture for automated checks. The
fixture supplies sample prices and replies.

1. Ask for a reply in Traditional Chinese. Verify that the fixture's text
   appears in chat and the app makes no catalogue tool call. Check that an
   underspecified shopping question can receive a clarification in the same way.
2. Ask “I want the cheapest milk” with two matching packs. Verify the model
   requests a lowest-listed-price search, receives ranked exact packs, and
   checks the saved listings. The price card shows AEON and HK$7.50 for the
   sample 236 mL pack. Check that its link opens the exact product.
3. Ask “What about Wellcome?” in the same chat. Verify the tool uses the same
   pack and the card shows Wellcome and HK$12.00. Check that a bilingual
   retailer name such as “惠康 WELLCOME” resolves to the same listing. Ask
   about a missing listing and check that its price stays unknown. Reopen
   the chat after starting a separate one. Its cards keep their retrieval times.
4. Search for milk options, then choose the 236 mL pack from the exact-pack
   picker. Verify the selected pack returns to DeepSeek and checked prices
   appear. An unknown query returns an empty product result and leaves room
   for the model to ask a useful question.
5. In the provider test, check that tools are declared in the request, tool
   replies match their call IDs, and the model's final text reaches the app.
   Exercise invalid tool arguments, malformed replies, a provider outage, and
   the tool call limit. Check that only permitted catalogue operations run.
6. Hold the reply request in Chrome. Confirm the shopper's bubble is already
   visible, another read sees that message, and the next draft can be typed.
   Fail the send and check draft recovery. Fail the reply, then retry and check
   that the accepted message appears once. Fail the provider itself and check
   that retry resumes the same question and Discover stays available.
7. At 375 by 667 px, reopen a chat, check message alignment and that mobile
   navigation leaves the composer clear. Check that a personal-data export
   includes checked prices saved with the transcript.
8. In `.env`, verify the key, base URL and model load as local configuration;
   `.env.example` contains no real key and `.env` is excluded from Git.

The provider contract is checked in
[DeepSeekChatClientTest](../../../src/test/java/com/pricewatch/web/DeepSeekChatClientTest.java).
[ChatConfigurationTest](../../../src/test/java/com/pricewatch/web/ChatConfigurationTest.java)
checks that chat base URL and model overrides map to client properties.
[AskTurnTest](../../../src/test/java/com/pricewatch/web/AskTurnTest.java)
checks turn acceptance, duplicate reply requests and retry.
[AskCatalogueToolsTest](../../../src/test/java/com/pricewatch/web/AskCatalogueToolsTest.java)
checks full-result price ranking, exact-pack selection, retailer names, and the
retrieval time in checked results.
[HelloControllerTest](../../../src/test/java/com/pricewatch/web/HelloControllerTest.java)
and the [Chrome workflow](../../../src/test/browser/market-desk.cjs)
exercise the catalogue and screen flow. Run live provider checks separately
with the historical sample to verify CityU compatibility.
