# Setup & Run Guide — Spring Boot + Vite/React App

This guide covers installing everything needed to build and run the app
and how to start it in the browser.

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| **JDK** | 21 (LTS) | Temurin (Adoptium) recommended. |
| **Node.js** | 20.19+ or 22 LTS | Installed via **nvm-windows** below. |
| **Maven** | 3.9.x | Not installed globally — the repo ships a Maven **wrapper** (`mvnw.cmd`), so no global install needed. |

The project targets **Spring Boot 3.4.5** on **Java 21**, and
**Vite 8** + **React 19** + **TypeScript 5.7**.

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

## 3. Install Node.js + npm (via nvm-windows)

The frontend (`frontend/`) needs Node 20.19+ (Vite 8 requirement). Using
**nvm-windows** is the easiest way to install and switch Node versions.

### Install nvm-windows

1. Download `nvm-setup.exe` from <https://github.com/coreybutler/nvm-windows/releases>.
2. Run the installer. It sets `NVM_HOME`, `NVM_SYMLINK`, and adds them to `PATH` automatically.
3. **Close and reopen** your terminal.

### Install and use Node 22 LTS

```powershell
nvm install 22
nvm use 22
```

### Verify

```powershell
node --version    # v22.x.x
npm --version     # 10.x or 11.x
nvm list          # shows installed versions; * marks the active one
```

---

---

## 4. Get the code & dependencies

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

## 5. Run the app

```powershell
.\mvnw.cmd spring-boot:run
```

First run downloads all dependencies (can take a few minutes). When you see `Started PricewatchApplication`, open:

```
http://localhost:8080
```

You should see the page render **"Hello, World!"**.

To stop the app, press `Ctrl+C` in the terminal.

### Ports & URLs

| Service | Port | URL | Notes |
|---------|------|-----|-------|
| Spring Boot (backend + serves React in prod) | **8080** | <http://localhost:8080> | Set in `src/main/resources/application.properties`. |
| Vite dev server (frontend only, HMR) | **5173** | <http://localhost:5173> | Set in `frontend/vite.config.ts`. |

When you use `npm run dev` (Vite dev), it proxies `/api/*` requests to
Spring on `:8080`, so the SPA can use the same URLs in dev and prod.

To change a port:
- Spring: edit `server.port` in `src/main/resources/application.properties`.
- Vite: edit `server.port` in `frontend/vite.config.ts`.

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

### How to use it (quick reference)

```powershell
# One-time
nvm install 22 && nvm use 22          # Node + npm
cd frontend && npm install            # frontend deps
cd ..

# Run the app — pick ONE workflow

# A) Production-style review (single URL, no HMR)
cd frontend && npm run build && cd ..
.\mvnw.cmd spring-boot:run           # → http://localhost:8080

# B) Local dev (live reload, two terminals)
.\mvnw.cmd spring-boot:run           # terminal 1
cd frontend && npm run dev            # terminal 2 → http://localhost:5173
```