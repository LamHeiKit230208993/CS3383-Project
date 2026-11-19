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
Saved, Alerts, Budget, and Reminders have working forms from J05–J08. J09 Ask,
J10 list sharing, J11 offline snapshots, and J12 history/personal-data controls
are implemented on this branch. See the
[baseline guide](implementation-baseline-j01-j04.md),
[extension guide](implementation-j05-j08.md), and
[J09–J12 contract](implementation-j09-j12.md) for behaviour and limits.

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

### Change language or contrast

Open **Preferences**, choose English, 繁體中文 (Traditional Chinese), or
简体中文 (Simplified Chinese), and choose normal or high contrast. Save the form.
The choice is stored in the local H2 database for the single local user and
applies to every page; no account, cookie, or JavaScript is involved. The
sidebar language form changes only the language and keeps the current contrast.

Product and brand names come from the imported source data and keep their own
language. Missing translations fall back in this order:

- English screen: English, then Traditional, then Simplified.
- Traditional Chinese screen: Traditional, then English, then Simplified.
- Simplified Chinese screen: Simplified, then English, then Traditional.

Prices and retailer/product codes are not translated. High contrast changes the
page colours and focus outline but does not replace a keyboard or assistive
technology check.

### Enable the Ask assistant (optional)

Ask works without an AI provider: it shows the checked product, every listed
retailer price or `Unknown`, the lowest price, and the retrieval time. The AI is
only asked to explain those checked facts in the question's language. If it is
missing, fails, times out, or mentions a price that is not in the checked facts,
the page shows the deterministic checked answer instead.

Set the key in the environment before starting the app. Never put it in a tracked
file, screenshot, test, log, or commit:

```powershell
$env:PRICEWATCH_AI_API_KEY = "<Your CityU API key>"
java -jar target\pricewatch-0.0.1-SNAPSHOT.jar
```

The CityU OpenAI-compatible defaults are already in
`application.properties`:

- Base URL: `https://llm.cs.cityu.edu.hk/v1`
- Model: `CS3383/DeepSeek-V4-Flash`

Override them only if CityU changes the contract:

```powershell
$env:PRICEWATCH_AI_BASE_URL = "https://llm.cs.cityu.edu.hk/v1"
$env:PRICEWATCH_AI_MODEL = "CS3383/DeepSeek-V4-Flash"
```

If the label says `AI unavailable or rejected`, the app fell back to the checked
answer. The rest of the app, including search, comparison, and every checked
fact on the Ask page, remains usable.

Do not read the key from `opencode.json`; put the value directly in the
environment variable or an untracked local properties file.

### Export or import a list

Open **My list**, then **Export or import**. Export downloads a version 1 JSON
snapshot of a saved list. Import creates a new separate list from a file; it
never overwrites or merges into My list. The page previews the created records,
and the new list can be previewed or exported again.

The file stores product codes, quantities, checked states, and saved retailer
choices, but not prices. The importing app uses its own current accepted
catalogue, so both local copies need compatible products. A retailer must be
listed for its product; a listed retailer with no current price keeps its saved
choice and shows an unknown price. A file is limited to 2 MB and 999 distinct
products, with quantities from 1 to 999.

The whole file is checked before anything is written. Malformed, oversized,
unknown-product, duplicate-product, out-of-range, or unlisted-retailer input
leaves every existing list unchanged. Imported lists are read-only in this
milestone: `/list` still edits only My list.

### Save a list for offline use

On **My list**, choose **Offline copy**. The transfer page also offers an
offline copy for each saved list. The browser downloads one `.pricewatch.html`
file containing the list name, products, quantities, checked states, saved
retailers, unit and line amounts, and the saved-on time. Prices that are not
known are shown as **Unknown**.

The file contains inline styling only. It has no script, form, link, image,
refresh control, chat control, or remote address. You can copy it to the viewing
device, stop the app or disconnect the device, and open it in a browser. Saved
amounts stay visible, with a data-age label and an **Unknown** source price date.
The copy is read-only: it never refreshes, and checking or editing inside the
file is not supported. Its age is the time elapsed since the visible saved-on
timestamp.

For isolated checks, disable the scheduled summary job with
`--pricewatch.jobs.enabled=false`. This does not disable manual refresh, page
checks, or the browser's safe page refresh.

### See a tracked product's history

On **Saved**, choose **View history** for a tracked product, or open
`/history/<product code>`. The page shows one section per tracking period with
its retailer and status, then the retrieval time and listed price of each
accepted LIVE observation in the last 90 days. `Unknown` means the stored
observation has no price.

When adjacent observations are at least seven days apart, the table shows a gap
row with the number of days that have no recorded observation. The page does not
invent daily values, and the source price date remains unknown. An unknown or
untracked product returns 404. The page is read-only.

### Review privacy and delete personal data

Open **Privacy** to see how many lists, list items, wishlist entries, tracking
periods, observations, alerts, and reminders are stored, and whether preferences,
budget, and alert timing are set. **Download personal data** saves one versioned
`pricewatch-personal-data.json` file containing those personal records. The
accepted product catalogue and its source metadata are not included.

**Delete personal records** requires the confirmation checkbox. It removes the
personal records above and resets product-wide tracking and target choices, but
keeps accepted products, listings, offers, and import metadata. Exported or
shared copies cannot be recalled. Deleting preferences returns the UI to the
English/normal default. Use the separate list export when you only want to share
one list; that file contains no prices or other personal settings.

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

---

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
state, keyboard navigation, and four viewport widths. Synthetic source records
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

Neither check uses the normal `data/pricewatch` database. An upgrade result is
valid only for the two supplied JAR versions; a current-to-current restart does
not prove an upgrade. See the [test catalogue](Test/test-case-catalog.md) and
[dated history](Test/history.md) for recorded results and limits.
