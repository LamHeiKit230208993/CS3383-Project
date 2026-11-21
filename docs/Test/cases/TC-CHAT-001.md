# TC-CHAT-001 — J09 checked price chat (tool-calling agent)

## Purpose

Check that the Ask page answers price questions in natural language using a short
tool-calling agent loop, asks the shopper to choose when a request is ambiguous, never
invents a price, and keeps search and comparison usable when the AI provider is
unavailable.

## Setup

- Start the test application with an isolated H2 database (`ChatControllerTest`,
  `ChatAgentTest`). The AI client is replaced with a scripted stub so no network call
  is made.
- Load the checked sample so product `P000000124` (100 %Fresh Milk 236mL) is present.

## Steps

1. Send a natural question (`what is the cheapest milk product`). Confirm the model
   calls `search_products`, then `product_prices`, and the reply names the checked
   product, retailer, and amount from the catalogue.
2. Compare two products (`compare milk and bread`). Confirm both are looked up and the
   reply uses the listed amounts.
3. Have the model request an unknown code. Confirm the tool returns "No product" and no
   fabricated fact is recorded.
4. Make the model keep calling tools. Confirm the loop stops at four steps and forces a
   final answer without tools.
5. Configure the AI to fail (throw `AiUnavailableException`) and leave it unconfigured.
   Confirm both still answer from checked records and a normal search (`/`) keeps
   working with no invented price.

## Expected result

The model reasons and requests lookups; the app runs them deterministically against the
catalogue. Answers use only returned product/retailer/amount facts; ambiguous or
out-of-scope requests ask the shopper to choose; an unavailable AI still leaves search
and comparison working; no price is invented. Verified by `ChatControllerTest` and
`ChatAgentTest`.

## Limits

The AI is scripted in tests; a live DeepSeek tool-calling call is not exercised by the
suite. The loop, tool execution, step limit, and fallback are covered.
