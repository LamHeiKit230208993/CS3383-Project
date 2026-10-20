# TC-WEB-003 — Prototype has no external asset requests

| Field | Details |
|---|---|
| Test class | `PrototypeControllerTest` |
| Test method | `prototypeAssetsDoNotLoadExternalResources()` |
| Type | File scan test |
| Inputs | `src/main/resources/templates/` and `src/main/resources/static/` |

## Purpose

Verify that the prototype does not load fonts, scripts, styles, or images from
an external URL. The prototype must open without network access and without
console errors caused by failed external requests.

## Steps

1. Walk every file under the templates and static resource directories.
2. Remove the Thymeleaf namespace declaration from the check.
3. Search the remaining text for `http://` or `https://`.

## Expected result

- No external URL is found.
- System font stacks are used instead of remote font imports.

## Limits

This is a static scan. It does not test every browser request or prove that a
page has no console errors.

Source: [`PrototypeControllerTest.java`](../../../src/test/java/com/pricewatch/web/PrototypeControllerTest.java)
