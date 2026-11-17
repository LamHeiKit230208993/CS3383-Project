# Setup & Run Guide Ã¢â‚¬â€ Spring Boot App

This guide covers installing everything needed to build and run the Spring Boot app and how to start it in the browser.

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| **JDK** | 21 (LTS) | Temurin (Adoptium) is the recommended distribution. |
| **Maven** | 3.9.x | Not installed globally Ã¢â‚¬â€ the repo ships a Maven **wrapper** (`mvnw.cmd`), so no global install needed. |

The project targets **Spring Boot 3.4.5** on **Java 21**.

---

## 1. Install the JDK (one-time)

### Option A Ã¢â‚¬â€ via winget (easiest on Windows)

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e --accept-source-agreements --accept-package-agreements
```

### Option B Ã¢â‚¬â€ manual download

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
Saved, Alerts, Budget, Reminders, and the J09 Ask chat now have working forms.
Local integration checks passed on 6 November 2026; J09 adds the checked price chat.
See the [baseline guide](implementation-baseline-j01-j04.md) and
[extension guide](implementation-j05-j08.md) for behaviour and limits.

### Try the J05Ã¢â‚¬â€œJ08 flow

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

Alert timing uses Hong Kong time. Defaults are quiet hours **22:00Ã¢â‚¬â€œ08:00** and
daily summary **08:00**. Change them on Alerts; equal quiet start/end disables
quiet hours. Quiet hours delay notification eligibility, not manual list actions.
Events and reminders stay inside the app. Nothing is delivered while the app is
closed, and startup does not fetch prices.

### Ask (J09 checked price chat)

The **Ask** page answers price questions in natural language with a short tool-calling
agent loop. The model is given two read-only tools, `search_products(query)` and
`product_prices(code)`, and decides which to call. The app runs each lookup
deterministically against the accepted catalogue and feeds the results back; the model
may call a few times (up to four steps) to reason across products, then writes the
answer. Prices, retailers, and stock always come from the database rows the tools
return, never from the model, so no price is invented. If nothing matches, or the
question needs data the tools cannot provide, the assistant says so and asks the
shopper to pick a product rather than guessing.
The AI provider is **DeepSeek V4 Flash provided by CityU**, configured **server-side
only**. Put the settings in a local `.env` file in the project root (it is
git-ignored and loaded automatically at startup):

```
PRICEWATCH_AI_BASE_URL=https://llm.cs.cityu.edu.hk/v1
PRICEWATCH_AI_API_KEY=your-key-here
PRICEWATCH_AI_MODEL=CS3383/DeepSeek-V4-Flash
```

Copy `.env.example` to `.env` and fill in your key, then just run:

```powershell
.\mvnw.cmd spring-boot:run
```

Real environment variables set in the shell still take precedence over `.env`, so CI
or one-off runs can override without editing the file.

The base URL is the CityU OpenAI-compatible endpoint; the client appends
`/chat/completions`. The key is supplied the same way as in your editor's
`opencode.json`, but the app reads it only from these server-side environment
variables Ã¢â‚¬â€ it does not open `opencode.json`. The values are read through
`pricewatch.ai.base-url`, `pricewatch.ai.api-key`, and `pricewatch.ai.model` in
`application.properties`. Never hardcode or commit the key. If the AI is not
configured or unreachable, Ask still answers from checked records and the normal
search and comparison flow keep working.


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

### Open the app from a phone on the same Wi-Fi

By default the app listens only on `127.0.0.1`, so a phone cannot reach it. The
phone workflow turns on two things: the server listens on the network
(`--server.address=0.0.0.0`), and the guard accepts your computer's Wi-Fi address
(`PRICEWATCH_ALLOWED_HOSTS`). Do these steps once per Wi-Fi network.

**1. Find this computer's Wi-Fi address.** It looks like `192.168.x.x`.

```powershell
ipconfig
```

Read the IPv4 address of the active Wi-Fi adapter. Use it below in place of
`192.168.1.20`. On macOS/Linux use `ipconfig getifaddr en0` or `ip addr`.

**2. Start the app so the phone can connect.** Open PowerShell in the project
folder. Set the allowed host, then start the app bound to all interfaces. Build
the runnable JAR first if needed (a JAR under about 1 MB is not the packaged app;
stop any running copy, then rebuild):

```powershell
.\mvnw.cmd clean package
```

Then, in the same PowerShell window:

```powershell
$env:PRICEWATCH_ALLOWED_HOSTS = "192.168.1.20"
java -jar target\pricewatch-0.0.1-SNAPSHOT.jar --server.address=0.0.0.0
```

To run from source instead, pass the same flag through the wrapper:

```powershell
$env:PRICEWATCH_ALLOWED_HOSTS = "192.168.1.20"
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.address=0.0.0.0'
```

macOS/Linux use `export PRICEWATCH_ALLOWED_HOSTS=192.168.1.20` instead of the
`$env:` line, and `./mvnw` instead of `.\mvnw.cmd`.

For more than one address, separate them with commas, for example
`192.168.1.20,192.168.1.20:8080`. A single bare address is enough for both.

**3. Open the app on the phone.** Keep the phone on the same Wi-Fi and enter this
in its browser, using your own address:

```
http://192.168.1.20:8080
```

**4. If the phone cannot connect, allow the app through the firewall.** On
Windows, accept the prompt for Java when it first listens, or add an inbound rule
for the port:

```powershell
New-NetFirewallRule -DisplayName "PriceWatch 8080" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

Remove it later with
`Remove-NetFirewallRule -DisplayName "PriceWatch 8080"`.

**To switch it off**, stop the app (`Ctrl+C`) and clear the variable:

```powershell
Remove-Item Env:\PRICEWATCH_ALLOWED_HOSTS
```

With the app stopped, the phone address no longer works. Without the variable (or
with it empty), requests using a LAN address are refused and only `localhost`
works, even if the app is bound to `0.0.0.0`.

Only list addresses you trust: anyone on that Wi-Fi can open the running app
while it is bound to `0.0.0.0`. Do not do this on public or untrusted networks.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `JAVA_HOME not found in your environment` | Set `JAVA_HOME` (see Ã‚Â§2) and reopen your terminal. |
| `java -version` shows an old version | Your terminal still has a stale PATH Ã¢â‚¬â€ reopen it, or ensure JDK 21 bin is first in the persistent PATH. |
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
