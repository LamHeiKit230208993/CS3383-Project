# Architecture Options — Online Price Watch Companion App

*This document gives you the detail needed to choose between building this app with a framework (Spring Boot) or without one (plain Java + embedded Jetty). Both implement the same three-layer architecture; they differ in how much of the plumbing you get for free versus how much you own. It is written against the actual requirements in `detailed-proposal.md` and the data reality in `feasibility-analysis.md`.*

---

## 0. The shared architecture (both options)

Both options implement the same conceptual shape. The choice is not *whether* to layer, only *who writes the glue*.

```
Browser / Mobile
      │  HTTPS (JSON)
      ▼
┌─────────────────────────────────────────────┐
│ CONTROLLER   — parse HTTP, validate input,   │
│                return JSON. NO business logic│
├─────────────────────────────────────────────┤
│ SERVICE      — business rules: what is a     │
│                "best deal", fake-discount     │
│                detection, unit-price logic,   │
│                agent reasoning.               │
├─────────────────────────────────────────────┤
│ REPOSITORY   — data access: products, price  │
│                history, users, watchlists.    │
│                Knows SQL/DB, nothing about    │
│                HTTP.                          │
├─────────────────────────────────────────────┤
│ DATA STORES  ── PostgreSQL (products, users, │
│                watchlists) + Redis (cache,    │
│                sessions, rate limits)         │
└─────────────────────────────────────────────┘
      │
      ▼
External: Consumer Council data.gov.hk CSV (daily snapshot)
```

The **data layer is identical** in both options. This project has a distinctive data need (from `feasibility-analysis.md`):

- **Price history must be accumulated by us** — the live API only gives a daily snapshot. So the app needs a **scheduled snapshot job** that fetches the CSV each day and writes an `price_history` table. This is a day-1 architecture requirement even though history features ship later.
- **Product identity** is keyed on `Product Code` (shared across retailers), which simplifies the repository design but still needs a **normalisation** concern.
- **The agent** needs to query the same repository for prices + history, then produce a reasoned single answer.

Both options below must support: a daily fetch job, a search/compare API, a history accumulation path, an agent endpoint, and auth/watchlist persistence.

---

## 1. Option 1 — Spring Boot (framework)

### 1.1 The dependency set

| Dependency | Purpose |
|-----------|---------|
| `spring-boot-starter-web` | REST controllers, embedded Tomcat, JSON via Jackson |
| `spring-boot-starter-data-jpa` | Repository interfaces + ORM (Hibernate) |
| `spring-boot-starter-validation` | Bean Validation on request DTOs |
| `spring-boot-starter-security` | Auth (JWT or session) |
| `spring-boot-starter-test` | Test harness (JUnit 5, MockMvc) |
| `spring-boot-starter-actuator` | Health/status endpoints, metrics |
| `org.postgresql:postgresql` | JDBC driver |
| `org.springframework:spring-batch` *(optional)* | Scheduled daily snapshot ingestion |

### 1.2 How the app is structured

```
src/main/java/com/pricewatch
├── PriceWatchApplication.java      ← @SpringBootApplication, entry point
├── config/
│   └── SecurityConfig.java         ← filter chain, JWT
├── controller/
│   ├── ProductController.java      ← /api/products/search, /api/products/{id}
│   ├── ComparisonController.java   ← /api/compare
│   ├── HistoryController.java      ← /api/products/{id}/history
│   ├── AlertController.java        ← /api/watchlist, /api/alerts
│   └── AgentController.java        ← POST /api/agent/ask
├── service/
│   ├── ComparisonService.java      ← best-deal, unit-price, basket logic
│   ├── TrustService.java           ← fake-discount detection, % saving
│   ├── AgentService.java           ← LLM call + reasoning
│   └── SnapshotService.java        ← @Scheduled daily CSV fetch
├── repository/
│   ├── ProductRepository.java      ← interface, Spring Data generates SQL
│   ├── PriceHistoryRepository.java
│   ├── WatchlistRepository.java
│   └── UserRepository.java
├── model/
│   ├── entity/                     ← Product, PriceHistory, User, Watchlist
│   └── dto/                        ← request/response records
└── exception/
    └── GlobalExceptionHandler.java ← @RestControllerAdvice
```

### 1.3 What Spring gives you for free

- **Dependency injection** — you `@Autowired` services into controllers; no manual `new`, no wiring code.
- **Repository implementations** — you write an *interface* (`ProductRepository extends JpaRepository<Product, Long>`) and Spring Data derives queries from method names (`findByProductCodeAndRetailer(...)`). You never write the JDBC boilerplate.
- **Transaction management** — `@Transactional` on services; no manual commit/rollback.
- **Validation** — annotations (`@NotBlank`, `@Min`) on DTOs, enforced at the controller boundary.
- **Security** — `Spring Security` gives a filter chain, method-level auth (`@PreAuthorize`), and standard token handling out of the box.
- **Scheduling** — `@Scheduled(cron = "0 15 6 * * *")` for the daily snapshot job.
- **Serialization** — Jackson auto-maps request bodies ↔ Java objects ↔ JSON responses.
- **Error handling** — `@RestControllerAdvice` maps exceptions to consistent JSON error bodies in one place.
- **Testing & tooling** — `MockMvc`, `@SpringBootTest`, test slices; actuator health checks.
- **Config** — externalised in `application.yml` (datasource, secrets, feature flags).

### 1.4 What Spring costs you

- **Startup weight** — a Spring Boot app boots slower and uses more memory than a bare Jetty server. Irrelevant for a course/demo; relevant for a tiny always-on box.
- **Magic/indirection** — annotations and auto-config hide what actually runs. When something breaks, the stack is deep and you must learn Spring's model (bean lifecycle, proxies, `@Transactional` gotchas like self-invocation not being proxied).
- **Learning curve** — DI, Spring Data conventions, Security config, profiles. There's a lot to absorb before you write your first feature.
- **Convention lock-in** — you write code Spring's way. Refactoring away from it later is a rewrite.
- **Version coupling** — Spring Boot parent manages dependency versions; pulling in a library with conflicting versions can be fiddly.

### 1.5 Effort estimate for THIS project

| Concern | Effort with Spring |
|---------|--------------------|
| REST endpoints + JSON | Very low — a few lines per endpoint |
| Search/compare across retailers | Low — repository method + service |
| Daily snapshot ingestion | Low — `@Scheduled` + batch repo write |
| Agent endpoint | Low — controller + service calling an LLM |
| Auth (users, JWT, sessions) | Low-Medium — Spring Security scaffolds it |
| Watchlist + alerts | Low — entities + repos |
| Fake-discount detection | Medium — pure logic, but Spring's part is trivial |
| **Product normalisation** | **Medium** — this is the real work, and Spring doesn't help |
| Tests | Low-Medium — good tooling |

The honest picture: **Spring removes nearly all the "boilerplate" work, leaving you to focus on the domain logic** — normalisation, trust detection, agent reasoning, snapshot accumulation. That is exactly the hard, valuable part of *this* project.

---

## 2. Option 2 — Plain Java + embedded Jetty (no framework)

### 2.1 The dependency set (tiny)

| Dependency | Purpose |
|-----------|---------|
| `org.eclipse.jetty:jetty-server` | Embedded HTTP server (can run from `main`) |
| `org.eclipse.jetty:jetty-servlet` | Servlet API support |
| `com.fasterxml.jackson.core:jackson-databind` | JSON serialization |
| `org.postgresql:postgresql` | JDBC driver |
| `com.zaxxer:HikariCP` | JDBC connection pooling |
| `org.slf4j:slf4j-simple` | Logging |
| *(optional)* `org.quartz-scheduler:quartz` | Cron for the daily snapshot job |

### 2.2 How the app is structured

```
src/main/java/com/pricewatch
├── Main.java                    ← start Jetty, register handlers/servlets
├── http/
│   ├── Router.java              ← map path+method → handler (hand-written)
│   ├── Json.java                ← ObjectMapper helper (write JSON, parse body)
│   └── ExceptionMapper.java     ← catch → JSON error body
├── controller/
│   ├── ProductServlet.java      ← implements doGet/doPost per route
│   └── ... (or one servlet with a path switch)
├── service/
│   ├── ComparisonService.java
│   ├── TrustService.java
│   ├── AgentService.java
│   └── SnapshotService.java
├── repository/
│   ├── ProductRepository.java   ← hand-written JDBC (PreparedStatement)
│   ├── PriceHistoryRepository.java
│   ├── WatchlistRepository.java
│   └── UserRepository.java
├── model/
│   ├── Product.java             ← plain POJO (no annotations)
│   └── dto/                     ← records
├── db/
│   └── Database.java            ← HikariCP DataSource, a shared `getConnection()`
└── scheduler/
    └── SnapshotJob.java         ← quartz job or a simple ScheduledExecutorService
```

### 2.3 What you own (the trade-off)

Every one of the "free" Spring features is now your responsibility:

- **Routing** — you hand-write a `Router` or use servlet `doGet`/`doPost`. A simple `switch` on the path works, but grows ugly as routes multiply. You will probably write a small matching layer.
- **JSON** — `Json.fromJson(req, MyDto.class)` and `Json.toJson(resp, obj)`. Thin wrapper over Jackson, but you write it and wire it into every handler.
- **DB access** — every query is `PreparedStatement` + `ResultSet` mapping. `findByProductCode(...)` is a method you fully write. No automatic query derivation. For this project's search/normalisation queries, that is a real amount of SQL.
- **Transactions** — manually `conn.setAutoCommit(false)`, commit, rollback, and close the connection (ideally via try-with-resources) in each service method.
- **Validation** — manual checks in the controller (`if (req.name() == null) ...`) or a hand-rolled validator.
- **Auth** — no security filter chain. You write the JWT parsing/validation, the filter, and the per-endpoint auth guard yourself.
- **Scheduling** — a `ScheduledExecutorService` or Quartz. Trivial to set up, but you own the job's error handling and idempotency.
- **Error handling** — you wrap handlers in try/catch and map exceptions to responses, or write one `ExceptionMapper` that does it (Spring does this with one annotation).
- **DI** — none. You `new` services, or hand-roll a tiny service locator/`AppContext`. With a handful of singletons this is fine; as it grows you may find yourself writing what Spring already is.
- **Config** — read from `application.properties` or env vars with a small config class; no profile support out of the box.

### 2.4 What you gain (the upside)

- **Total visibility** — you can trace every request through your own code. Nothing is hidden behind a proxy or auto-config. Great for understanding how a web app actually works.
- **Tiny footprint** — fast startup, low memory, a single fat `jar` runs anywhere. Ideal if you deploy to a small free-tier VM/container.
- **No magic** — a bug is in *your* code, not buried in a framework you don't fully control. Easier to reason about for a solo learner.
- **Full control** — you choose the exact libraries and behaviour. No version-coupling constraints, no convention lock-in.
- **Educational value** — you learn HTTP, servlets, JDBC, connection pooling, serialization, and scheduling by doing them. This is genuinely valuable for a course project and for interviews.

### 2.5 Effort estimate for THIS project

| Concern | Effort without framework |
|---------|--------------------------|
| REST endpoints + JSON | Medium — router + Jackson wiring for every endpoint |
| Search/compare across retailers | Medium — hand-written SQL + result mapping |
| Daily snapshot ingestion | Medium — scheduler + batch JDBC |
| Agent endpoint | Medium — controller + service, plus JSON wiring |
| Auth (users, JWT, sessions) | **High** — you write the filter + token logic |
| Watchlist + alerts | Medium — entities + JDBC + queries |
| Fake-discount detection | Medium — pure logic, unaffected by the choice |
| **Product normalisation** | **Medium** — the real work, same as Spring |
| Tests | Medium — you wire up your own harness |

The honest picture: **the framework choice does not reduce the hard domain work** (normalisation, trust detection, agent reasoning, snapshot accumulation). What it changes is the amount of **plumbing** you write. Without Spring, you spend a large share of effort on routing, JDBC, transactions, validation, and auth — plumbing that is identical for every web app and adds no value to *this* product.

---

## 3. Side-by-side decision table

| Criterion | Spring Boot | Plain Java + Jetty |
|-----------|-------------|--------------------|
| Time to first working endpoint | Minutes | Hours |
| Boilerplate (routing, DB, JSON, auth) | Minimal — provided | Substantial — you write it |
| Code you must write | Focused on domain logic | Domain logic **+** all the plumbing |
| Understandability of internals | Hidden (magic) | Complete visibility |
| Memory/startup footprint | Heavier | Light |
| Learning curve | Steeper (DI, Security, Data) | Shallow at first, but more code |
| Test tooling | Excellent (MockMvc, test slices) | You assemble it |
| Auth implementation | Scaffolded by Spring Security | Hand-written |
| Error handling | One `@RestControllerAdvice` | Hand-rolled `ExceptionMapper` |
| Flexibility / no lock-in | Lower (conventions) | Higher (you own everything) |
| Fit for a *course/demo* project | **Best** | Doable but slower to a demo |
| Fit for *learning how HTTP works* | Poor | **Best** |
| Long-term maintenance (this app) | Lower | Higher (more code to maintain) |

---

## 4. How this maps to THIS project's unique needs

Three things specific to the price-watch app push the decision:

1. **The daily snapshot ingestion job.** Both options need it. Spring's `@Scheduled` makes it ~5 lines; without it you set up Quartz or a `ScheduledExecutorService` and own error handling. Slight edge to Spring, not decisive.

2. **Product normalisation is the hard part — and it is identical either way.** This is pure domain logic: parsing size from free-text `Product Name`, grouping by `Product Code`, de-duplicating variant pack sizes. **Neither framework helps here.** You will write this regardless. So it should *not* be the deciding factor — it is the same cost in both.

3. **Auth + user data (watchlists, budgets, shared lists).** This project has real per-user state (per the personas: watchlist, alerts, shopping lists, budgets, privacy-erase). That is a lot of CRUD + auth. **Spring makes this dramatically cheaper** (Spring Security + Spring Data JPA repositories). Without a framework, auth is the single biggest plumbing sink.

4. **The agent + trust engine.** The agent endpoint is just a controller + service calling an LLM. The trust logic (fake-discount detection, % saving) is pure computation. **The framework does not matter here** — it is all domain logic.

**Net effect for this app:** the framework choice mostly affects the *user-data plumbing* (auth + CRUD) and the *boilerplate*, which are both things Spring does best. The domain differentiators (normalisation, agent, trust detection) are equally hard either way.

---

## 5. Recommendation

**Use Spring Boot** for this project.

Reasons specific to the price-watch app:

- The product is **user-data heavy** (watchlists, alerts, budgets, shared lists, privacy erase). That is auth + CRUD, and Spring Security + Spring Data JPA remove the largest chunk of manual work.
- The **differentiators** (normalisation, agent, trust engine) are the part you should spend your effort on. Spring frees you to spend it there instead of on JDBC routing.
- You want a **demo that works** — Spring gets you to a polished endpoint faster, which matters for a course/portfolio project.
- The **hidden-magic drawback** is real but manageable: the hidden magic is almost all *plumbing* (routing, transactions, serialization), not your domain logic. You still understand and own the product logic.

**Choose plain Java + Jetty only if** one of these is your real goal:

- You want to **learn how HTTP/servlets/JDBC actually work** (strong educational value; the reason to pick this).
- You need a **tiny, single-jar, low-memory** deployable and have few users/features.
- You deliberately want **zero framework lock-in** and are comfortable writing your own plumbing.

---

## 6. Decision criteria checklist (to confirm your choice)

Answer these honestly; if any point toward Spring, use Spring.

- Do you have per-user accounts, auth, and CRUD? → *This project: yes (watchlist, alerts, lists, budgets).* **Spring.**
- Do you want a demo working within a few days? → **Spring.**
- Is your goal to learn the internals of a web server / JDBC? → **Plain Java.**
- Will the app have many routes / growing surface? → **Spring.**
- Are you comfortable with (or willing to learn) DI, Spring Security, Spring Data? → **Spring.**
- Do you want a small memory/startup footprint on a constrained deploy target? → **Plain Java.**
- Is the domain logic the part you most want to focus your effort on? → **Spring.**