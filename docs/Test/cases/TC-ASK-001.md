# TC-ASK-001 — Checked-facts price assistant

This case covers J09 Ask. Dated execution results are recorded separately in
the test history.

Sources:
[`AskServiceTest`](../../../src/test/java/com/pricewatch/ask/AskServiceTest.java),
[`AskControllerTest`](../../../src/test/java/com/pricewatch/web/AskControllerTest.java),
[`HttpAiAnswerClientTest`](../../../src/test/java/com/pricewatch/ask/HttpAiAnswerClientTest.java), and
the Ask scenario in `src/test/browser/milestone-check.cjs`.

## Retrieval and answers

Open `/ask` with a loaded catalogue and ask for a product by code, name, brand,
or category. Expect a deterministic match from the accepted catalogue before
any model call. The page shows the checked product, brand, retailer prices,
lowest listed price, retrieval time, an explicit unknown source price date, and
an answer. A missing price stays unknown and is never treated as zero.

Submit a question that names no product, or two products with an ambiguous
match. Expect a no-match message or a product chooser instead of a guessed
answer. Choosing a product runs the same checked-facts path. A blank question
or one longer than the configured limit is rejected without calling the model.

The model may explain the facts but cannot add or change an amount. If the
answer contains a `HK$` or `$` amount that is not one of the checked prices, the
page uses the deterministic fallback. The same fallback applies when the model
is unavailable, returns an empty answer, or the request fails. The fallback
still names the product, lowest price and retailer when known, or states that
the price is unknown.

## Provider boundary

`HttpAiAnswerClient` calls an OpenAI-compatible `/chat/completions` endpoint
using the configured base URL and model, a Bearer header, and a 15-second
default timeout. Tests use a local HTTP server and assert the path, header,
model, stream setting, and body; a 500 response and an empty answer are
failures. An empty API key makes no network call. Automated tests use a fake
client.

The real key must only come from `PRICEWATCH_AI_API_KEY` or an untracked local
properties file. It must never appear in a tracked file, screenshot, test, log,
commit, or documentation. The CityU defaults are
`https://llm.cs.cityu.edu.hk/v1` and `CS3383/DeepSeek-V4-Flash`.

## Limits

The assistant explains accepted catalogue facts; it is not a price source, a
stock check, or a shopping recommendation. The browser scenario runs without an
API key and therefore checks the safe fallback path.
