# TC-USABILITY-001 · Cart and Settings walkthrough

## Setup and method

On 22 November 2026 an agent operated headless Google Chrome using Playwright
against the packaged PR #24 build at revision `741d3fe`. The app used an
isolated in-memory H2 database and the historical sample catalogue. The agent
reviewed screenshots of each resulting screen. Viewports were 1440 × 900 and
375 × 812. This is an agent-operated walkthrough.

## Tasks and observed results

| Task | Expected | Observed |
|---|---|---|
| Create a named Cart and add the sample 236 mL milk | The named Cart shows the selected retailer and estimated price | The Weekend Cart showed AEON and HK$7.50. Its desktop summary identified Weekend and displayed the same total. |
| Reach Display settings from desktop Settings | The section appears and the navigation shows its current location | Display settings appeared at the top of the content area and its navigation link was selected. |
| Open Display settings and return on a phone viewport | The submenu opens and its arrow returns to Settings | The display form and back arrow were visible. Clicking the arrow returned to /settings. |
| Reopen the named Cart on a phone viewport | The saved product and controls appear in a usable layout | The named list and product appeared with retailer, quantity, price, and Done control. |

## Friction and follow-up

At 375 × 812, the fixed mobile navigation overlaps the bottom of the initial
viewport near Mark done and delete. A separate run on the packaged merged
revision `4a61bf1` scrolled the Done control into view, focused it, and
activated it. The control became checked and the visual focus outline remained
visible. After scrolling 30 px, the Done control occupied y=662–708 and the
navigation began at y=731. This verifies that the Done control can be exposed
and activated on the tested viewport. In a further isolated run on the same
build, the focused delete control was at y=661–707 while navigation began at
y=731. Enter opened its confirmation. Escape closed it and the item remained.
Physical-device behaviour was not tested.

The screenshots were captured at `target/usability-cart-desktop.png`,
`target/usability-settings-desktop.png`, `target/usability-settings-mobile.png`,
and `target/usability-cart-mobile.png` in the ignored build output. The original
workflows are also covered by the maintained shopper browser suite.
The merged-build mobile screenshots are
`target/usability-current-before-scroll.png` and
`target/usability-current-after-scroll.png`. The delete cancellation capture
is `target/usability-current-delete.png`.
