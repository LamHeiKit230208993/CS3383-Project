# TC-UI-002 - High-contrast, easy-to-read display (J12, R47)

Covers the contrast switch, the high-contrast palette, keyboard focus, and
meaning that does not rely on colour alone.

Sources:
[`LocaleUiTest`](../../../src/test/java/com/pricewatch/web/LocaleUiTest.java),
`market-desk.css`, checked by hand in Chrome.

## Steps and expected results

1. With the default setting the body has no `data-contrast` attribute and the
   contrast button reports `aria-pressed="false"`.
2. On the **Settings** page (`/my-data`), click **High contrast** (`GET /contrast?mode=high&back=/`). The cookie
   `CONTRAST=high` is set, the body gains `data-contrast="high"`, the button
   reports `aria-pressed="true"` and shows **On**.
3. In high-contrast mode the palette switches to black background, white text,
   and light-blue links; text is larger and every panel keeps a 2px border.
4. Keyboard: `Tab` reaches the skip link first, then the nav, language, contrast,
   and page controls, each with a visible 4px amber focus ring.
5. Meaning without colour: status pills keep their text (`Tracking`/`Stopped`),
   the chosen retailer keeps `✓ Selected`, and the lowest price keeps the
   `Lowest listed` label, so nothing depends on colour alone.
6. With Windows forced-colors on, panels keep visible `CanvasText` borders and
   the focus ring uses the system `Highlight` colour.

## Limits

- Contrast ratios were checked visually; no automated contrast measuring was
  added. Add one if a formal audit is required.
