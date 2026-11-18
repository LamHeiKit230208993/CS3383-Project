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
Saved, Alerts, Budget, and Reminders have working forms in the J05–J08
implementation. Open Ask for the price assistant described below.
See the [baseline guide](implementation-baseline-j01-j04.md) and
[extension guide](implementation-j05-j08.md) for behaviour and limits.

### Try the J05–J08 flow

1. Open a product comparison and choose **Save or track this product**. Save it to the
   wishlist, or explicitly track a retailer. These are separate actions.
2. Open **Alerts** to set a target or enable a drop rule for that tracking period.
   Only accepted LIVE data can create live history and price alerts. Loading the
   historical sample is useful for screens, lists, budgets, and reminders; it
   does not simulate live observations.
3. Open **Budget**, choose Weekly or Monthly, and save a planned limit. It is
   compared with the current My list estimate, including checked items. An
   explicit retailer choice stays selected when its price becomes unknown.
4. From a product comparison, choose **Set a replenishment reminder**. Choose packs, an interval
   in days, and an optional retailer. Adding its quantity to My list leaves the
   reminder active. Clear starts a new interval from now; Snooze delays its
   effective due time by 24 hours.

Alert timing uses Hong Kong time. Defaults are quiet hours **22:00–08:00** and
daily summary **08:00**. Change them on Alerts; equal quiet start/end disables
quiet hours. Quiet hours delay notification eligibility, not manual list actions.
Events and reminders stay inside the app. Nothing is delivered while the app is
closed, and startup does not fetch prices.

The running web app checks summaries every minute. Alerts and Reminders pages
also refresh every minute when visible, unless a form has edits or focus, or a
disclosure is open. Reopen or reload the page to refresh manually. Without
JavaScript, forms still work and page opening checks the current state.

For isolated checks, disable the scheduled summary job with
`--pricewatch.jobs.enabled=false`. This does not disable manual refresh, page
checks, or the browser's safe page refresh.

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

The default server binds to `127.0.0.1`. It is a local app, not a shared cloud
service. Saved data on this computer does not by itself make a phone work offline.

### Share or review a saved list

Open **My list**, choose a list, and select **Open list**. **Export JSON**
downloads the open list. **Import JSON** creates a separate editable copy.
The file keeps each pack, quantity, checked state, and retailer choice.
The receiving app gets prices from its own catalogue. Missing products or
retailer prices appear as **Unknown**. Quick-add, budgets, and reminders use
the default **My list**.

Choose a list and select **View offline** while Spring Boot is running on
the device. The read-only page shows the saved estimates and retrieval time.
This mode applies to the current browser session. Select **Exit offline review**
to edit the list, refresh prices, or use Ask.

### Display settings, personal data, and history

Open **Display settings** to choose English, Traditional Chinese, or
Simplified Chinese, and to enable high contrast. The choice is saved on this
device. Product names use the matching source labels when available.

Open **Personal data** to review saved record counts, find the relevant
screens, or download a JSON copy. **Review reset** opens a confirmation
screen. A confirmed reset clears local personal records and the current
session's chats. The accepted catalogue and its import details stay available.
Previously exported files remain on the devices that received them.

Open **Price history** from a product comparison. The table shows retained
observations for that exact pack, with a retailer, the retrieval time, and
the listed price or **Unknown**. An untracked pack can have no records.
The table uses the existing 90-day window. Tracking is separate from
wishlist saving or list editing.

---

### Ask about prices

J09 uses the CityU OpenAI-compatible endpoint at `https://llm.cs.cityu.edu.hk/v1`.
Set `DEEPSEEK_API_KEY_SKU` in Windows and launch the app from a terminal that
inherits it. The key stays server-side. The default model ID is
`CS3383/DeepSeek-V4-Flash`. Override it with `PRICEWATCH_CHAT_MODEL` if CityU
advertises a different ID. `PRICEWATCH_CHAT_BASE_URL` overrides the base URL.

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

DeepSeek interprets the question and recent chat context. It can request a
catalogue search, ask for a missing detail, or compare the selected pack at
another retailer. The server runs the lookup, asks the shopper to choose when
several packs match, and builds the price answer from checked listings.
Without the provider, Ask still supports direct catalogue terms and common
price questions. The recent conversation is sent to the configured CityU
endpoint when the key is available.

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
