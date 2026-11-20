# TC-DISPLAY-001 · Display languages and contrast

Use the isolated browser app with the controlled catalogue. Select English,
Traditional Chinese, and Simplified Chinese. Check the document language,
the matching source label for P000000124, and localized Ask feedback.
Enable high contrast. Use Tab and Enter to submit preferences and search on
desktop and at 375 px. Check that keyboard focus has an outline, the mobile
page fits the viewport, and the setting remains selected on return.
Tab to a retailer card and press Space. Check that the card has a focus
outline before selection and a visible selected label and translated
estimate after selection.

This scenario checks core keyboard operation and representative labels.
It does not measure all contrast ratios or substitute for screen reader testing.

The server test uses a sample catalogue and separate sessions to confirm that
the language and contrast setting apply to the whole local app. It checks that
the selected category keeps the same filter value and shows the matching source
label. An unsupported language leaves the previous settings in place.

The secondary page test loads the sample, saves and tracks the milk pack, and
creates a reminder. It checks rendered Chinese text on the data, planned,
budget, saved, Alert history, Current rules, and reminders pages. It submits invalid reminder and
budget inputs to check localized validation.

Sources [browser suite](../../../src/test/browser/market-desk.cjs) and
[display preferences controller test](../../../src/test/java/com/pricewatch/web/DisplayPreferencesControllerTest.java),
[secondary page localization test](../../../src/test/java/com/pricewatch/web/SecondaryDisplayTest.java).
