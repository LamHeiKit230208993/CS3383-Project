# Architecture

> Status: Implemented (October 2026) — replaces the earlier Thymeleaf prototype
> (see [`../../legacy-thymeleaf/`](../../legacy-thymeleaf/)).

## 1. One-paragraph summary

The app is a **single-page web application** split into two cooperating
artefacts that share one HTTP origin:

- **Frontend** — Vite + React 19 + TypeScript + Tailwind CSS v4, built into
  a static bundle at `frontend/dist/`. Lives in the browser.
- **Backend** — Spring Boot 3.4.5 on Java 21, exposing a small REST API
  under `/api/**`. Lives on the server (JVM).

Spring Boot serves the static bundle at `/` and forwards any unknown path
to `index.html` (SPA fallback), so the React app can own its own routing.
The frontend talks to the backend through `/api/...` URLs, which work
identically in dev and production.

## 2. Why two builds?

| | Frontend (`npm run build`) | Backend (`mvnw spring-boot:run`) |
|---|---|---|
| **What it produces** | `frontend/dist/` — static HTML/JS/CSS | A running JVM on port 8080 |
| **Where it runs** | In the user browser | On the server |
| **Language / tooling** | TypeScript + JSX → esbuild/Rollup | Java 21 → javac + Spring |
| **Triggered by** | Node + npm (Vite) | JDK 21 + Maven (wrapper) |

The browser cannot execute Java; the JVM cannot render React. Both halves
are needed for a working app. See [`../SETUP.md`](../SETUP.md) for tool
installation.

## 3. Folder map

```
CS3383-Project/
├── frontend/                       # Vite + React 19 + TS + Tailwind v4
│   ├── src/
│   │   ├── main.tsx                #   React entrypoint
│   │   ├── App.tsx                 #   Main view component
│   │   ├── index.css               #   Tailwind v4 + globals
│   │   └── vite-env.d.ts
│   ├── index.html                  #   Vite HTML shell
│   ├── package.json                #   npm scripts: dev / build / preview
│   ├── tsconfig.json               #   TypeScript config (paths: @/* → src/*)
│   └── vite.config.ts              #   Port 5173, /api proxy → :8080
│
├── src/                            # Spring Boot backend
│   ├── main/
│   │   ├── java/com/pricewatch/
│   │   │   ├── PricewatchApplication.java     #   @SpringBootApplication entrypoint
│   │   │   └── web/
│   │   │       ├── WebConfig.java             #   Serves frontend/dist/, SPA fallback
│   │   │       └── ApiController.java         #   /api/health (sample)
│   │   └── resources/
│   │       └── application.properties         #   server.port=8080, etc.
│   └── test/...
│
├── legacy-thymeleaf/               # Earlier Thymeleaf prototype (archived)
│
├── docs/                           # Project documentation
│   ├── SETUP.md
│   ├── feasibility-review-simple.md
│   └── Sam/                        # ← this folder (architecture notes)
│
├── pom.xml                         # Spring Boot 3.4.5, Java 21, Thymeleaf starter
└── .gitignore
```

## 4. Request lifecycle

### 4a. First page load

```
Browser                                       Spring Boot (8080)
  |                                                  |
  |--- GET / ---------------------------------------->|
  |                                                  |  WebConfig: serve frontend/dist/index.html
  |<-- 200 text/html (index.html) --------------------|
  |                                                  |
  |--- GET /assets/index-bihKQQwS.js ---------------->|
  |<-- 200 application/javascript -------------------|
  |                                                  |
  |--- GET /assets/index-DRy-ocCy.css -------------->|
  |<-- 200 text/css ---------------------------------|
  |                                                  |
  |   (browser executes React; React mounts on #root)|
```

### 4b. The SPA calls the backend

```
Browser (React)                                Spring Boot (8080)
  |                                                  |
  |--- GET /api/health ----------------------------->|
  |                                                  |  ApiController.health()
  |<-- 200 {"status":"ok","service":"pricewatch-.."} |
```

The SPA uses **same-origin** requests — no CORS headers, no extra config.
This works in both dev and prod because Vite proxies `/api/*` to Spring in
dev and Spring serves both the bundle and the API in prod.

### 4c. SPA fallback for unknown paths

If the user visits `/list` directly:

```
Browser                                       Spring Boot (8080)
  |                                                  |
  |--- GET /list ------------------------------------>|
  |                                                  |  No controller matches `/list`
  |                                                  |  WebConfig PathResourceResolver:
  |                                                  |    1. Look for frontend/dist/list
  |                                                  |    2. Not found → return index.html
  |<-- 200 text/html (index.html) --------------------|
  |   (React reads window.location.pathname, renders the list view)
```

This is how React Router (or our state-based routing) keeps working when
the page is reloaded or linked to directly.

## 5. The two workflows

### Development (live reload)

```bash
# Terminal 1
./mvnw spring-boot:run            # :8080, serves JSON

# Terminal 2
cd frontend
npm run dev                       # :5173, serves React with HMR
```

- Visit **http://localhost:5173** to see the app.
- Edit any file under `frontend/src/` → browser updates in ~200 ms.
- The Vite `server.proxy` in `vite.config.ts` rewrites `/api/*` to
  `http://localhost:8080`, so React code can call `/api/...` directly.
- Edit Java → restart `mvnw`.

### Review / Demo (single-server, no Vite)

```bash
# One-time build of the SPA
cd frontend && npm run build

# Run the server (serves both bundle and API)
./mvnw spring-boot:run
```

- Visit **http://localhost:8080** — Spring serves the React bundle at `/`
  and JSON at `/api/*`.
- After editing the React app, re-run `npm run build` then refresh the
  browser (Spring does not auto-rebuild the bundle).

## 6. Where to add things

| You want to... | Edit this | Example |
|---|---|---|
| Add a new screen/page (React) | `frontend/src/App.tsx` (or a new component file under `frontend/src/`) | A new `<BudgetPage />` view |
| Add a new REST endpoint | `src/main/java/com/pricewatch/web/ApiController.java` (or a new `@RestController` under `web/`) | `@GetMapping("/api/products")` |
| Change which port Spring uses | `src/main/resources/application.properties` | `server.port=8081` |
| Change which port Vite uses | `frontend/vite.config.ts` | `server.port: 5174` |
| Change the API proxy target | `frontend/vite.config.ts` | `target: 'http://localhost:8081'` |
| Add a backend dependency | `pom.xml` | `spring-boot-starter-data-jpa` etc. |
| Add a frontend dependency | `frontend/package.json` | `npm i <package>` then commit `package.json` |

## 7. Build & deploy artefacts

| Artefact | Path | Used by |
|---|---|---|
| React bundle (production) | `frontend/dist/` | Spring Boot (via `WebConfig`) |
| React bundle (dev) | in-memory, served by Vite | Developer only |
| Spring Boot JAR | `target/pricewatch-0.0.1-SNAPSHOT.jar` | Production deployment |
| Classpath resources | `src/main/resources/` | Spring Boot |

To produce a single deployable artefact:

```bash
cd frontend && npm run build
cd ..
./mvnw package
java -jar target/pricewatch-0.0.1-SNAPSHOT.jar
```

The JAR embeds `frontend/dist/` only if it has been copied into
`src/main/resources/static/` first. With the current `WebConfig`, Spring
reads `frontend/dist/` from disk at runtime, so the JAR must be run from
the repo root (or `frontend/dist/` must be packaged into the JAR via a
Maven resource step — not currently configured).

## 8. Key files in detail

### `frontend/vite.config.ts`

- Plugins: `@vitejs/plugin-react`, `@tailwindcss/vite`
- Path alias: `@/*` → `frontend/src/*`
- Dev server: port **5173**, host `0.0.0.0`
- Proxy: `/api` → `http://localhost:8080`
- Build output: `frontend/dist/`

### `src/main/java/com/pricewatch/web/WebConfig.java`

- Registers a single `ResourceHandler` for `/**` over two locations:
  `file:./frontend/dist/` (preferred) and `classpath:/static/`.
- Uses a custom `PathResourceResolver` to fall back to `index.html` when
  the requested file does not exist — this is the SPA fallback.
- `cachePeriod(0)` during development; tighten this for production.

### `src/main/java/com/pricewatch/web/ApiController.java`

- `@RequestMapping("/api")` base path.
- `@GetMapping("/health")` returns a tiny JSON status object — used to
  confirm the wiring end-to-end.
- New endpoints should follow the same pattern. Prefer DTOs (Java records
  or `Map<String, Object>`) over raw entities for stability.

### `pom.xml`

- `spring-boot-starter-web` — REST controllers and embedded Tomcat.
- `spring-boot-starter-thymeleaf` — kept for now (unused); safe to remove
  once the team confirms the Thymeleaf prototype is fully retired.
- `spring-boot-starter-test` — JUnit + MockMvc for backend tests.

### `.gitignore`

- `target/` — Maven build output
- `frontend/node_modules/`, `frontend/dist/`, `frontend/.vite/` — Vite/npm artefacts

## 9. Open items

- **Thymeleaf starter**: still in `pom.xml` though no controller returns a
  template. Remove once the team confirms.
- **Static-file caching**: `WebConfig` uses `cachePeriod(0)`. For a real
  deployment, split cache rules: long-cache hashed assets (`/assets/*-*.js`),
  no-cache `index.html`.
- **Routing**: the current `App.tsx` uses a single `useState` for the
  active view (no router). Adding `react-router-dom` would let the SPA
  honour deep links without the SPA-fallback hack — though the fallback
  already works for full-page reloads.
- **API surface**: only `/api/health` exists. Feature endpoints come from
  the project scope (see `feasibility-review-simple.md`).
- **Packaging**: `frontend/dist/` is read from disk at runtime. To ship a
  self-contained JAR, copy `dist/` into `src/main/resources/static/` during
  the Maven `prepare-package` phase (via `maven-resources-plugin`).

## 10. Glossary

- **SPA** — Single-Page Application. The browser loads one HTML page; JS
  swaps the rendered view as the user navigates.
- **HMR** — Hot Module Replacement. Vite pushes updated modules to the
  browser without a full reload.
- **SPA fallback** — Server returns `index.html` for any unknown path so
  the SPA can take over routing.
- **Proxy** — In Vite dev, requests to `/api/*` are forwarded to another
  server (Spring) so the SPA sees the same URLs in dev and prod.
- **CORS** — Cross-Origin Resource Sharing. Not relevant here because the
  SPA and API share one origin (`:8080`).
