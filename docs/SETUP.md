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
Saved, Updates, and Ask are planned features. See the
[implementation guide](implementation-baseline-j01-j04.md) for the current scope.

To stop the app, press `Ctrl+C` in the terminal.

### Local database

Normal use stores an H2 file at `data/pricewatch.mv.db`, relative to the folder
where you start the app. Database files are ignored by Git. Stop the app before
copying this file for a backup. Do not run two app instances against the same
database file.

Flyway manages the schema. On first run it either applies
`src/main/resources/db/migration/V1__baseline.sql` to an empty database or marks
an existing J01–J04 schema as the version 1 baseline; Hibernate only validates
it. **Back up `data/pricewatch.mv.db` before the first run of this version** and
before any later migration. Later jobs add `V2__…` files rather than editing the
baseline. See the [extension foundation guide](extension-foundation.md#migration-policy).

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
