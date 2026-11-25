# TC-DISPLAY-001 · Display languages and contrast

Use the isolated browser app with the controlled catalogue. Select English,
Traditional Chinese, and Simplified Chinese. Check the document language,
the matching source label for P000000124, and localized Ask feedback.
In English, Discover and Compare show the English name with no secondary
Chinese name. Both pages still use the selected source name in each Chinese
display mode. Search continues to match names across all three languages.
Enable high contrast. Use Tab and Enter to submit preferences and search on
desktop and at 375 px. Check that keyboard focus has an outline, the mobile
page fits the viewport, and the setting remains selected on return.
With English and high contrast selected at desktop width, check the sidebar
and Compare panel have 28 px corners and navigation links and Discover tags
keep their pill corners.
Tab to a retailer card and press Space. Check that the card has a focus
outline before selection and a visible selected label and translated
estimate after selection.

Open Settings at desktop width and select each section in the left menu.
Verify its heading reaches the top offset, the selected link updates, and
reduced motion disables smooth scrolling. At 375 px, open Personal data,
Display settings, and Price data from the menu. Use the back arrow to return
from each section. Check that the Display switch works with the Space key.

This scenario checks core keyboard operation and representative labels.
It does not measure all contrast ratios or substitute for screen reader testing.

The server test uses a sample catalogue and separate sessions to confirm that
the language and contrast setting apply to the whole local app. It checks that
the selected category keeps the same filter value and shows the matching source
label. An unsupported language leaves the previous settings in place.
It checks the English catalogue, filtered results, and Compare against a
sample product with distinct names in all three languages.

The secondary page test loads the sample, saves and tracks the milk pack, and
checks rendered Chinese text on Price data, Ask, Personal data, Wishlist,
Alert history, Current rules, and the item editor. It submits invalid rule
and budget inputs to check localized validation.

Sources [browser suite](../../../src/test/browser/market-desk.cjs) and
[display preferences controller test](../../../src/test/java/com/pricewatch/web/DisplayPreferencesControllerTest.java),
[secondary page localization test](../../../src/test/java/com/pricewatch/web/SecondaryDisplayTest.java).
