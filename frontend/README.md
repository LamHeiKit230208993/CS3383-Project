# Pricewise — Vite + React frontend

The single-page UI for the Pricewise price-comparison app. Built with
**Vite 8**, **React 19**, **TypeScript 5.7**, and **Tailwind CSS v4**.

## How it talks to Spring Boot

- **Production** (one server): run `npm run build`, then start Spring Boot
  with `mvnw spring-boot:run`. Spring serves `dist/` at `/` and exposes
  `/api/**` for the SPA. Open <http://localhost:8080>.
- **Development** (two servers): start Spring on `:8080` and Vite on
  `:5173`. Vite proxies `/api/*` to Spring, so the SPA can use the same
  `/api/...` URLs in dev and prod. Open <http://localhost:5173>.

## Commands

| Command | What it does |
|---|---|
| `npm install` | Install dependencies (run once, and after pulling changes) |
| `npm run dev` | Start the Vite dev server on `:5173` with HMR + `/api` proxy |
| `npm run build` | Produce a production bundle in `dist/` |
| `npm run preview` | Serve `dist/` locally for testing the production build |

## Layout

```
frontend/
├── index.html              # Vite HTML shell (entry: /src/main.tsx)
├── package.json
├── vite.config.ts          # React + Tailwind plugins, /api proxy → :8080
├── tsconfig.json
├── public/                 # Copied verbatim into dist/
│   └── favicon.svg
└── src/
    ├── main.tsx            # React entry; mounts <App/> in #root
    ├── App.tsx             # Main app component (UI demo)
    ├── index.css           # Tailwind import + custom CSS
    └── vite-env.d.ts
```

## Connecting to the Spring API

Use `fetch('/api/...')` from any component. Vite's dev proxy and Spring's
static handler both forward `/api/**` correctly.

```ts
const res = await fetch('/api/health')
const data = await res.json()
```

The current Spring backend exposes:

- `GET /api/health` → `{ status, service, time }`

Add more endpoints by creating `@RestController` classes in
`src/main/java/com/pricewatch/web/`.

## Toolchain

Pinned via `.mise.toml` if you use [`mise`](https://mise.jdx.dev/);
otherwise any modern Node (≥ 20) will work — Vite 8 requires Node 20.19+.

```toml
node = "22"
"npm:pnpm" = "10.34.3"   # optional; npm works fine
```
