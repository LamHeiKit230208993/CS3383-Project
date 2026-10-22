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

You should see the **Market Desk** app. Press **Import sample file** to load the checked
sample catalogue, or **Fetch daily JSON** to download the current one. Search, compare,
and add items to your list; the catalogue and the list are stored in a local H2 file.
There is no separate frontend build or extra frontend dependency to install.
The Saved, Updates, and Ask scenes are still sample screens (later jobs).
See [the implementation record](implementation-baseline-j01-j04.md) for the built scope,
and [the theme notes](Plan/market-desk-theme.md) for the visual prototype.

To stop the app, press `Ctrl+C` in the terminal.

---

## 5. Inspect the database with the H2 Console

The project can expose H2's browser-based console for local development. The
setting is in
[`src/main/resources/application.properties`](../src/main/resources/application.properties):

```properties
spring.h2.console.enabled=true
```

If it is set to `false`, change it to `true` and restart the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Open the console at:

```text
http://localhost:8080/h2-console
```

Enter the following connection details:

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:file:./data/pricewatch` |
| User Name | `sa` |
| Password | leave blank |

Click **Connect**. The console should show the tables created by Hibernate,
including `CATALOGUE`, `PRODUCT`, `LISTING`, `SHOPPING_LIST`, and `LIST_ITEM`.
For example:

```sql
SELECT COUNT(*) AS product_count FROM PRODUCT;

SELECT CODE, BRAND, CATEGORY1, CATEGORY2, CATEGORY3
FROM PRODUCT
LIMIT 20;

SELECT * FROM SHOPPING_LIST;

SELECT * FROM LIST_ITEM;
```

The database is file-based, so the console reads the same data used by the
application. Stop the application before using another database tool against
the same file. Do not edit `data\pricewatch.mv.db` directly.

The H2 Console is an administrative tool. Keep it enabled only while working
locally, do not expose it to the internet, and set the property back to
`false` when you finish:

```properties
spring.h2.console.enabled=false
```

Restart the application after changing the setting.

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `JAVA_HOME not found in your environment` | Set `JAVA_HOME` (see §2) and reopen your terminal. |
| `java -version` shows an old version | Your terminal still has a stale PATH — reopen it, or ensure JDK 21 bin is first in the persistent PATH. |
| First import takes a long time | The daily file is about 2.7 MB with ~2,700 products; the first replace writes them all. Wait for the page to return. |
| Want a clean start | Stop the app and delete the `data/` folder. It holds the H2 file database. |
| H2 Console does not open | Confirm `spring.h2.console.enabled=true`, restart the app, and use `http://localhost:8080/h2-console`. |
| H2 Console connection fails | Use `jdbc:h2:file:./data/pricewatch`, user `sa`, and an empty password. Start the app from the repository root so `./data` resolves correctly. |
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
