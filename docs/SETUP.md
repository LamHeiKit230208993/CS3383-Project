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

The root redirects to the **Market Ledger UI prototype** at `/search`. All
product, price, history, alert, and assistant content is sample data, and demo
state resets on reload. The original Hello page is kept at
`http://localhost:8080/hello`.

See [the prototype theme and evaluation notes](Plan/ui-prototype-theme.md) for
the screen list and checks.

To stop the app, press `Ctrl+C` in the terminal.

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