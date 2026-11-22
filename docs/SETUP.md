# Setup & Run Guide — Spring Boot App

This guide covers installing everything needed to build and run the Spring Boot app and how to start it in the browser.

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| **JDK** | 21 (LTS) | Temurin (Adoptium) is the recommended distribution. |
| **Maven** | 3.9.x | Not installed globally — the repo ships a Maven **wrapper** (`mvnw.cmd`), so no global install needed. |

The project targets **Spring Boot 3.4.5** on **Java 21**.

---

## 1. Install the JDK (one-time)

### Option A — via winget (easiest on Windows)

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e --accept-source-agreements --accept-package-agreements
```

### Option B — manual download

Download from <https://adoptium.net/temurin/releases/?version=21> and run the installer (install to `C:\Program Files\Eclipse Adoptium\jdk-21...`).

After install, confirm it works:

```powershell
& "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" -version
```

---

## 2. Set the environment variables (one-time)

In an **admin** PowerShell, run:

```powershell
[Environment]::SetEnvironmentVariable(
  "JAVA_HOME",
  "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot",
  "Machine"
)

$path = [Environment]::GetEnvironmentVariable("Path", "Machine")
[Environment]::SetEnvironmentVariable(
  "Path",
  "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin;$path",
  "Machine"
)
```

> **Important:** change the JDK path above to match your install.

**Close and reopen your terminal**, then verify:

```powershell
java -version   # should print "openjdk version 21..."
```

---

## 3. Get the code & dependencies

```powershell
git clone <repo-url>
cd CS3383-Project
```

There's no global Maven install needed. The repo includes `mvnw.cmd` (Windows) / `mvnw` (Mac/Linux), which automatically downloads Maven 3.9.6 and all project dependencies on first run.

Verify the wrapper works:

```powershell
.\mvnw.cmd -v
```

---

## 4. Run the app

```powershell
.\mvnw.cmd spring-boot:run
```

First run downloads all dependencies (can take a few minutes). When you see `Started PricewatchApplication`, open:

```
http://localhost:8080
```

You should see **Market Desk**. Open **Data** and choose **Load checked sample**
to try the first flow without a network connection. This imports the unchanged
two-product excerpt checked on 17 October 2026. It is historical sample data,
not current prices. Search for milk or bread, open one exact product, and add it
to **My list**. Quantity and checked state now survive app restart.

Choose **Refresh live prices** to download the current provider response.
Startup does not fetch prices. A failed refresh keeps the last accepted data.
After any catalogue has been accepted, replacing it with the two-product sample
requires the explicit sample-replacement confirmation on Data.
The source date stays unknown when the source does not provide one. A retrieval
time is not a price date.

Running the app needs no separate frontend build, React, Node runtime, or frontend
dependency. The optional browser regression checks below use Node and Playwright. Maven supplies Spring Data JPA and H2 with the other Java libraries.
Use Wishlist for tracked products and Alerts for price alerts.
Open Ask for the price assistant described below.
See the [baseline guide](implementation-baseline-j01-j04.md) and
[extension guide](implementation-j05-j08.md) for behaviour and limits.

### Try the J05–J08 flow

1. Open a product comparison and choose **Save to wishlist**. The app starts
   watching all retailers for the exact pack. New retailers are picked up on
   accepted live refreshes. Use the actions on the Wishlist row or select
   several products to add one pack of each missing item to the default Cart.
   Removing a product stops new price tracking and keeps its recorded history.
2. Select **Set rules** on the pack. Use **Add rule** to choose a price
   condition or a calendar time in Hong Kong time. A price condition checks
   accepted live refreshes and lists the retailers that meet the target.
   Choose one time, daily, weekly, every two weeks, or monthly. One-time
   rules turn off after they fire. A repeating price rule needs a new
   qualifying live refresh in a later repeat period.
3. Open **Settings → Personal data**, choose Weekly or Monthly in the Budget
   card, and save a planned limit. It is
   compared with the current My list estimate, including checked items. An
   explicit retailer choice stays selected when its price becomes unknown.
4. Open **Alerts → History** to see what fired. Open a history row's rule
   editor to make a change. Each **Current rules** card shows the product,
   condition, and On or Off switch. Switches also work on earlier retailer
   rules and quantity-based reminders. Removing a rule keeps its history.

The running app checks due rules each minute and when Alerts opens. Its
scheduled work can be disabled for isolated checks with
`--pricewatch.jobs.enabled=false`. Page opening still checks due reminders.
Alert and price records show Hong Kong time. Quiet hours and scheduled
summaries are retired. Old settings and summary records remain exportable.

To stop the app, press `Ctrl+C` in the terminal.

### Local database

Normal use stores an H2 file at `data/pricewatch.mv.db`, relative to the folder
where you start the app. Database files are ignored by Git. Stop the app before
copying this file for a backup. Do not run two app instances against the same
database file.

To use a different location, supply the datasource URL at launch:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:file:./data/my-pricewatch'
```

Use the same working folder and URL after restart to read the same catalogue and
list. A different folder or URL creates a different local database. Tests use
isolated databases and do not need your normal data file.

The default server binds to all network interfaces. It is a local app, not a
shared cloud service. The request guard accepts private-LAN addresses as well as
localhost.

### Open the app from a phone

1. Keep the phone and computer on the same Wi-Fi or mobile hotspot network.
2. Start the app with `.\mvnw.cmd spring-boot:run`.
3. Find the computer's IPv4 address with `ipconfig`. Use the address under the
   active Wi-Fi or Ethernet adapter, for example `192.168.1.25`.
4. Open `http://192.168.1.25:8080` in the phone's browser.

If Windows Firewall blocks the phone, an administrator can allow only TCP port
8080 for the app:

```powershell
New-NetFirewallRule -DisplayName "Market Desk LAN" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow -Profile Private
```

Public networks can prevent device discovery or block local HTTP traffic. The app
does not provide authentication or Internet hosting; only use it on a trusted
network. Saved data on this computer does not by itself make a phone work offline.

### Share or review a saved list

Open **Cart**. Select a product card to open its comparison, then use **Back
to Cart** to return to the same list. Use **Mark done** to cross out and lock
a row. Select **Done** to reopen it before changing packs or retailer.
**Continue browsing** takes you back to Discover.
On wide screens, the list summary stays in view as you scroll through the items.
Use the Cart list switcher to open a named list. **New list** creates an
empty list and opens it. Rename or delete a named list from its list controls.
The product comparison page lets you choose the destination before adding.
**Export JSON** downloads the open
list. **Import list** creates a separate editable copy from a JSON file.
The file keeps each pack, quantity, checked state, and retailer choice.
The receiving app gets prices from its own catalogue. Missing products or
retailer prices appear as **Unknown**. Quick-add and reminders use the default
list. Discover and the budget settings summary use the main Cart. The Cart
page compares the open list with the same saved limit. Switch lists to check
another estimate. A list with missing prices shows an incomplete total.

Choose **List actions → Download offline checklist** to save an HTML copy.
Open the file in a browser without a connection or the local app running.
The file shows the selected Cart, saved retailer prices and retrieval time,
and the saved budget, if set. Change pack quantities, select a saved retailer,
edit the budget, or mark items done. The estimate and budget update as you edit.
Changes apply while the downloaded copy is open.

### Display settings, personal data, and history

On desktop, the Settings section menu scrolls to Personal data, Budget, Display
settings, and Price data. On phones, choose a submenu and use its left arrow
to return to Settings. Personal data includes the Budget form in both layouts.
Open **Settings → Display settings** to choose English, Traditional Chinese, or
Simplified Chinese, and to enable high contrast. The choice is saved on this
device. Product names use the matching source labels when available.

Open **Personal data** to review saved record counts, find the relevant
screens, or download a JSON copy. **Review reset** opens a confirmation
screen. A confirmed reset clears local personal records and the current
session's chats. The accepted catalogue and its import details stay available.
Previously exported files remain on the devices that received them.

Open **Price history** from Wishlist or a product comparison. Filter the
chart and table to one retailer or all retailers, and choose 30 or 90 days.
The chart uses recorded retrieval times and breaks at unknown prices and
stopped tracking periods. The table lists exact amounts and recorded gaps.

---

### Ask about shopping

J09 uses the CityU OpenAI-compatible endpoint at `https://llm.cs.cityu.edu.hk/v1`.
Create a local `.env` file from `.env.example` and enter the API key:

```dotenv
DEEPSEEK_API_KEY_SKU=replace-with-your-api-key
PRICEWATCH_CHAT_BASE_URL=https://llm.cs.cityu.edu.hk/v1
PRICEWATCH_CHAT_MODEL=CS3383/DeepSeek-V4-Flash
```

The app loads `.env` from the folder where Maven is started, so a new terminal
does not need environment variables configured again. `.env` is ignored by Git.
The key stays server-side. Existing OS environment variables take precedence over
`.env`. Override the model or base URL in `.env` when CityU advertises a
different value.

For a terminal-only setup, set the key in the current PowerShell session:

```powershell
$env:DEEPSEEK_API_KEY_SKU = "replace-with-your-api-key"
.\mvnw.cmd spring-boot:run
```

This setup lasts until the terminal is closed. It is the recommended option
for shared or temporary machines because the key is not saved by the app.

To save the key for future terminal sessions, set it for the current Windows
user instead:

```powershell
setx DEEPSEEK_API_KEY_SKU "replace-with-your-api-key"
```

Close every open terminal and VS Code window, reopen a terminal, verify that
the variable is available, and start the server:

```powershell
echo $env:DEEPSEEK_API_KEY_SKU
.\mvnw.cmd spring-boot:run
```

Optional persistent model and endpoint overrides:

```powershell
setx PRICEWATCH_CHAT_MODEL "CS3383/DeepSeek-V4-Flash"
setx PRICEWATCH_CHAT_BASE_URL "https://llm.cs.cityu.edu.hk/v1"
```

Do not commit the key to Git, put it in `application.properties`, or paste it
into source or documentation files. Restart the terminal or VS Code after
using `setx`.

Open **Ask** and enter a product name or code, or a question such as
“Where is Kowloon Dairy’s 236 mL fresh milk cheapest?” with the sample loaded.
Then ask “What about Wellcome?” to check the same pack at that retailer.
The current browser session keeps up to 12 chats, each with 24 recent messages
and its own selected product. Use **New chat** to start a separate conversation.
Open any retained chat under **Recent chats** to continue it during the session.
The message appears as soon as you send it. Ask saves your message before
requesting a reply and shows its progress in the chat. You can draft the next
message while you wait. A failed send restores the draft. If the reply is
interrupted, use **Try reply again**. Press **Enter** to send or
**Shift+Enter** to start a new line.

DeepSeek receives the recent messages and can reply directly, ask for a missing
detail, or call the app's catalogue tools. It can search for matching products
by relevance or lowest listed pack price, read a pack's prices, and offer exact
pack choices. When you pick a pack, DeepSeek continues the conversation with
that selection. **Checked catalogue prices** cards show the prices returned
by the app from its accepted catalogue.

Ask can also show your carts, Wishlist, alert rules and recent alerts. Try
“Show my cart”, “Add one 236 mL milk to Weekend groceries”, or “What is 20%
of HK$85?”. An unspecified destination means the main Cart. To set an alert,
request a price target or reminder. Ask will collect the exact product and
schedule, then show a preview. Confirm removals and alert changes in chat.
Open a linked item and use **Back to Ask** to return to its conversation.
Ask requires a working DeepSeek connection. If a reply fails, the question
stays in the chat for **Try reply again**. Check the key and endpoint if replies
keep failing. You can still browse and compare prices in **Discover**.

If Windows can reach CityU but Java reports a TLS handshake error, launch
the app with Windows' trusted certificate store. Certificate and hostname
verification remain enabled.

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.jvmArguments=-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
```

For a packaged JAR, put both `-D` options before `-jar`.

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `JAVA_HOME not found in your environment` | Set `JAVA_HOME` (see §2) and reopen your terminal. |
| `java -version` shows an old version | Your terminal still has a stale PATH — reopen it, or ensure JDK 21 bin is first in the persistent PATH. |
| Port 8080 already in use | Change the port in `src/main/resources/application.properties` (`server.port=8081`) or stop the process using 8080. |

---

## Useful commands

| Command | What it does |
|---------|--------------|
| `.\mvnw.cmd spring-boot:run` | Build + run the app |
| `.\mvnw.cmd clean` | Delete the `target/` build output |
| `.\mvnw.cmd test` | Run the test suite |
| `.\mvnw.cmd package` | Build a runnable JAR (`target/pricewatch-0.0.1-SNAPSHOT.jar`) |

To run the packaged JAR instead of `spring-boot:run`:

```powershell
.\mvnw.cmd package
java -jar target\pricewatch-0.0.1-SNAPSHOT.jar
```


## Optional browser regression checks

These checks use Node.js 18 or newer and the pinned Playwright development
package. They do not add a frontend build or change how the app runs.
Build the current Java code first, then install and run the browser checks:

```powershell
.\mvnw.cmd package
npm ci
npx playwright install chromium
npm run test:browser
```

The script starts its own app on an available loopback port. It uses an in-memory
H2 database and a local test feed. It does not connect to your running app, saved
shopping list, or the live price provider. `JAVA_HOME` must point to JDK 21;
otherwise the script uses `java` from PATH.

By default it uses Playwright's Chromium. To use an existing Google Chrome install
instead, skip the browser download and run:

```powershell
$env:BROWSER_CHANNEL = 'chrome'
npm run test:browser
Remove-Item Env:BROWSER_CHANNEL
```

To measure JavaScript executed during the shopper scenarios, set
`BROWSER_JS_COVERAGE=1` for the browser run. The suite writes
`target/browser-checks/javascript-coverage.json` with V8 source ranges for
Cart, Ask, and the inline script in the downloaded offline Cart. The report
measures source units reached during the run. See the
[test strategy](Test/strategy.md) for how this relates to Java branch coverage.

The checks cover native forms with JavaScript disabled, saved retailer choices,
missing selected prices, exact price previews, multilingual search, category URL
state, keyboard navigation, and four viewport widths. The combined workflow
checks Ask, sharing, offline review, translated history, and personal-data reset.
Synthetic source records
exercise relevance and decimal boundaries; they are test data, not real prices.
The script stops its own app and test server on completion or failure. It saves
screenshots and the app log under ignored `target/browser-checks/`.

### Milestone browser and upgrade checks

Build the current JAR first, then run the additional checks:

```powershell
npm run test:milestone
npm run test:persistence
```

The milestone check uses an isolated app and a local synthetic feed. It covers
Saved/tracking, alerts, budgets, reminders, form validation, timed refresh, and
desktop/mobile layouts. Screenshots and logs go under ignored
`target/browser-milestone/`. Synthetic price changes are test data, not provider
price evidence.

The persistence check creates a temporary file database, saves a selected retailer,
quantity and checked state, then restarts the app and checks those records. By
default it uses the current JAR for both starts. To check an upgrade, keep a copy
of the earlier JAR before rebuilding, then supply it explicitly:

```powershell
$env:BASELINE_JAR = 'D:/path/to/earlier-pricewatch.jar'
npm run test:persistence
Remove-Item Env:BASELINE_JAR
```

When the earlier build supports J05–J08 tracking, also set
`$env:UPGRADE_TRACKING = '1'`. This adds a controlled live source and checks
that retailer tracking and a recorded price survive reopening the legacy
database. Clear the setting with `Remove-Item Env:UPGRADE_TRACKING`.

Neither check uses the normal `data/pricewatch` database. An upgrade result is
valid only for the two supplied JAR versions; a current-to-current restart does
not prove an upgrade. See the [test catalogue](Test/test-case-catalog.md) and
[dated history](Test/history.md) for recorded results and limits.
