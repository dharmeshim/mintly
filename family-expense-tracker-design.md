# Family Expense Tracker — Project Design Document

## 1. Purpose

A mobile-centric web app for a family (2–3 active loggers) to track household
purchases, watch price trends per item, see category-wise spending, monitor
buying intervals (e.g. LPG refills), and view cost-of-living per head per
month — without spreadsheets.

---

## 2. Tech Stack

| Layer | Choice | Why |
|---|---|---|
| Backend | Spring Boot 3 (Java 17+) | Mature, typed, good with Postgres, easy cloud deploy |
| Build tool | Maven | As requested |
| Database | PostgreSQL, hosted on **Supabase free tier** | Free managed Postgres, $0 to start, no separate DB ops — see Section 8.5 for connection handling details |
| ORM | Spring Data JPA + Hibernate | Standard pairing, less boilerplate |
| Migrations | Flyway | Versioned schema changes against Supabase, applied via pooler connection |
| Frontend | React (Vite) | Separate SPA, calls REST API, good mobile UX control |
| Auth | PIN-per-profile + JWT session | Lightweight — no email/password, fits a shared family device |
| Charts | Recharts (React) | Price trend lines, category pie/bar charts |
| Hosting — backend | Any cloud VM / PaaS (Railway, Render, EC2, etc.) running the Spring Boot jar | DB is decided (Supabase); compute host still open, design doesn't lock this in |
| Hosting — DB | Supabase free tier Postgres | See Section 8.5 |
| Keep-alive / backups | GitHub Actions cron (free) | Pings health endpoint to avoid 7-day inactivity pause; can also run scheduled `pg_dump` backups |

---

## 3. High-Level Architecture

```
┌─────────────────────┐         ┌──────────────────────────┐        ┌────────────┐
│   React SPA (Vite)  │  REST   │   Spring Boot Backend     │  JPA   │ PostgreSQL │
│  (mobile-first UI)  │ <-----> │  (Controllers → Services  │ <----> │            │
│                      │  JSON  │   → Repositories)         │        │            │
└─────────────────────┘         └──────────────────────────┘        └────────────┘
```

- Stateless REST API, JWT in `Authorization` header after PIN login.
- React app is a single static build served separately (or via CDN/static
  hosting); calls the Spring Boot API over HTTPS.
- Spring Boot connects to **Supabase Postgres via the Supavisor pooler
  (session mode)**, not a direct connection — see Section 8.5 for why.
- No file/image storage needed for v1 (receipts explicitly out of scope).

---

## 4. Data Model

### 4.1 Entity Overview

```
Family (implicit single household for v1 — no multi-tenant table needed)
  └── Profile (1..N)
  └── Category (1..N)
        └── Item (1..N)
              └── Purchase (1..N)   ← logged by a Profile
```

### 4.2 Entities

**Profile**
| Field | Type | Notes |
|---|---|---|
| id | UUID/Long | PK |
| name | String | e.g. "Dad", "Mom" |
| pinHash | String | bcrypt hash of 4–6 digit PIN |
| active | boolean | drives "active members" count for cost-per-head |
| createdAt | timestamp | |

> Profiles are never hard-deleted — marking `active=false` preserves
> historical purchase attribution while excluding them from future
> per-head splits.

**Category**
| Field | Type | Notes |
|---|---|---|
| id | UUID/Long | PK |
| name | String | fully custom, user-created, unique |
| color | String (hex) | optional, for chart legends |
| createdAt | timestamp | |

**Item**
| Field | Type | Notes |
|---|---|---|
| id | UUID/Long | PK |
| name | String | e.g. "LPG Cylinder", "Toor Dal" — unique per category |
| categoryId | FK → Category | |
| unit | String | kg, litre, piece, packet, etc. |
| createdAt | timestamp | |

**Purchase**
| Field | Type | Notes |
|---|---|---|
| id | UUID/Long | PK |
| itemId | FK → Item | |
| profileId | FK → Profile | who logged/bought it |
| quantity | BigDecimal | |
| rate | BigDecimal | price per unit at time of purchase |
| totalAmount | BigDecimal | = quantity × rate (computed, stored for fast aggregation) |
| purchaseDate | Date | defaults to today, editable |
| shop | String | optional |
| paymentMode | enum (CASH, ONLINE, CARD, OTHER) | optional |
| notes | String | optional |
| createdAt | timestamp | audit |

### 4.3 Why this shape works for every requirement

| Requirement | How it's derived |
|---|---|
| Track all expenses | Every `Purchase` row = one expense entry |
| Rate change tracking | Query: previous `Purchase` for same `itemId` ordered by date → diff `rate` |
| Add buying items + rate | `Item` create (inline-or-pick) + `Purchase` create form |
| Category-wise spending | `SUM(totalAmount) GROUP BY Category` via `Item.categoryId` |
| Mobile-centric | React SPA built mobile-first, responsive up to desktop |
| Buying interval (e.g. LPG) | Query: all `Purchase` for an `itemId` ordered by date → date diffs between consecutive rows |
| Cost of living per head/month | `SUM(totalAmount)` for month ÷ `COUNT(active Profile)` |

No new tables needed for rate history or intervals — they're **derived
views over `Purchase`**, not separately stored. This keeps the schema small
and avoids data-sync bugs (no risk of a "history" table drifting from the
actual purchases).

---

## 5. Backend Design (Spring Boot)

### 5.1 Package structure

```
com.familyexpense
 ├── config/          → SecurityConfig (JWT filter), CorsConfig
 ├── auth/             → AuthController, JwtService, PinAuthService
 ├── profile/          → ProfileController, ProfileService, ProfileRepository, Profile entity
 ├── category/         → CategoryController, CategoryService, CategoryRepository, Category entity
 ├── item/             → ItemController, ItemService, ItemRepository, Item entity
 ├── purchase/         → PurchaseController, PurchaseService, PurchaseRepository, Purchase entity
 ├── analytics/         → AnalyticsController, AnalyticsService
 │                         (rate trend, buying interval, category spend, cost-per-head — all read-only/computed)
 └── common/           → exception handling, DTOs, mappers
```

Analytics is deliberately a **separate module** from `purchase` — it only
reads, never writes, and composes queries across `Purchase` + `Profile`. This
keeps `PurchaseService` focused on CRUD and `AnalyticsService` focused on
aggregation, so each stays simple to test.

### 5.2 Key REST Endpoints (draft)

```
POST   /api/auth/login                 { profileId, pin } → JWT

GET    /api/profiles                   list profiles (name + active flag, no PIN)
POST   /api/profiles                   create profile { name, pin }
PATCH  /api/profiles/{id}              update name / active flag

GET    /api/categories
POST   /api/categories                 { name, color }

GET    /api/items?search=              for "pick existing" autocomplete
POST   /api/items                      inline-create { name, categoryId, unit }

GET    /api/purchases?month=&category=&item=  filterable list
POST   /api/purchases                  log a purchase
PATCH  /api/purchases/{id}
DELETE /api/purchases/{id}

GET    /api/analytics/item/{itemId}/price-history   → [{date, rate, qty}], latest-vs-previous diff
GET    /api/analytics/item/{itemId}/buying-interval  → [{date, daysSinceLast}]
GET    /api/analytics/category-spend?month=          → [{category, total}]
GET    /api/analytics/cost-per-head?month=            → {total, activeMembers, perHead}
GET    /api/analytics/dashboard?month=                → bundled summary for home screen
```

### 5.3 Auth flow

1. App loads → `GET /api/profiles` → shows tappable profile avatars (no
   PINs exposed).
2. User taps their profile → PIN pad (4–6 digit) → `POST /api/auth/login`.
3. Backend verifies bcrypt hash → issues JWT (contains `profileId`,
   short-ish expiry, refresh on activity).
4. Every subsequent request carries the JWT; `purchase.profileId` on create
   is taken from the token, not the request body, so people can't log
   purchases under someone else's name by mistake.

---

## 6. Frontend Design (React, mobile-first)

### 6.1 Screens

1. **Profile Picker** — grid of family avatars → tap → PIN pad.
2. **Home / Dashboard**
   - This month's total spend
   - Cost-per-head this month (big, prominent — it's a headline ask)
   - Mini category breakdown (donut/bar)
   - "Add Purchase" floating action button (FAB) — always reachable with
     one thumb, since this is the most frequent action.
3. **Add/Edit Purchase** (the highest-frequency screen, must be fast)
   - Item field: type-ahead, shows existing matches, "+ Create new item"
     inline if no match
   - Category auto-fills from item if it exists; pick/create if new item
   - Quantity, Rate → Total auto-computed live
   - Date (defaults today), Shop (optional), Payment mode (optional)
   - Save → returns to Home with a toast confirmation
4. **Item Detail**
   - Price history line chart over time
   - Latest vs previous rate, shown as a delta badge (↑/↓ %)
   - Buying interval list ("Last bought 34 days ago", full history below)
5. **Category View**
   - Month selector
   - Spend by category (bar/donut), tap category → list of purchases in it
6. **Purchase History / Search**
   - Flat filterable list (by date range, category, item, profile)

### 6.2 Mobile-first principles applied

- Bottom navigation bar (Home / Add / Categories / History) — thumb-reachable.
- FAB for "Add Purchase" on Home, since that's the core repeated action.
- Large tap targets, single-column forms, numeric keypad auto-triggered for
  qty/rate fields.
- Charts simplified on narrow screens (e.g. price history defaults to last
  6 entries, expandable).

---

## 7. Key Calculated Logic (pseudocode, lives in `AnalyticsService`)

**Rate change for an item:**
```
purchases = findByItemId(itemId) ORDER BY purchaseDate DESC
latest = purchases[0]
previous = purchases[1]
delta = latest.rate - previous.rate
deltaPercent = delta / previous.rate * 100
```

**Buying interval:**
```
purchases = findByItemId(itemId) ORDER BY purchaseDate ASC
intervals = []
for i in 1..purchases.length:
    intervals.append(purchases[i].date - purchases[i-1].date)
```

**Category-wise spend (month):**
```
SELECT c.name, SUM(p.totalAmount)
FROM Purchase p JOIN Item i ON p.itemId = i.id
                JOIN Category c ON i.categoryId = c.id
WHERE p.purchaseDate BETWEEN monthStart AND monthEnd
GROUP BY c.name
```

**Cost per head (month):**
```
total = SUM(p.totalAmount) WHERE purchaseDate IN month
activeMembers = COUNT(Profile WHERE active = true)
perHead = total / activeMembers
```
> Uses *currently* active members, not a historical snapshot — simplest
> correct behavior for v1. (If someone goes inactive mid-month, edge-case
> historical accuracy can be revisited later; flagged here, not solved yet.)

---

## 8. What's explicitly OUT of scope for v1

- Receipt/photo upload
- Personal vs shared expense splitting (everything is shared, equal split)
- Reminders/notifications for "due" purchases (interval is shown as
  history only, not push alerts)
- Multi-family / multi-tenant support
- OAuth/social login — PIN only

These were deliberate trade-offs from our earlier discussion to keep v1
lean and shippable.

---

## 8.5. Database Hosting on Supabase (Free Tier)

Decision: use **Supabase's free Postgres** instead of self-hosting/managing
Postgres separately. This removes a whole layer of ops (no provisioning a
DB server, no manual backups setup beyond what's noted below) and keeps the
project at $0 to start.

### 8.5.1 What free tier actually gives us (verified, June 2026)

| Limit | Value | Relevance to this app |
|---|---|---|
| Database size | 500 MB | Plenty — purchase rows are tiny; thousands of purchases ≈ a few MB |
| Direct connections | 60 max | Not used by default — see connection strategy below |
| Pooler (Supavisor) connections | 200 max client connections | This is what Spring Boot will use |
| Active projects | 2 max | One for prod is enough; second slot free for a dev/test project later |
| **Inactivity pause** | **Project pauses after 7 days with zero API/DB activity** | **Real risk for a family app** — if nobody logs an expense for a week, Supabase pauses the project and the backend can't reach the DB until someone manually un-pauses it from the dashboard |
| Backups | None on free tier | We should not treat this DB as the only copy of family financial history |
| IPv4 | Not included (direct connection is IPv6-only; IPv4 direct costs $4/mo add-on) | Affects how Spring Boot connects — addressed below |

### 8.5.2 Connection strategy — Spring Boot → Supabase

Two ways to connect; we pick based on where the backend runs:

- **Direct connection (port 5432, IPv6)** — only sensible if our cloud
  host supports outbound IPv6 (some do, many don't by default). Best for
  a long-lived server process *if* IPv6 is available, since it avoids
  pooler limits entirely.
- **Supavisor pooler — session mode (port 5432 via pooler hostname)** —
  works over plain IPv4, behaves like a normal long-lived Postgres
  connection. **This is the right default for us**, since our backend
  is a single, persistent Spring Boot instance (not serverless/edge
  functions), and we can't assume IPv4 is unavailable on whatever VM/PaaS
  we deploy to later.
- Supavisor **transaction mode** (port 6543) is meant for serverless/edge
  functions with many short-lived clients — not our case, since Spring
  Boot keeps a connection pool open. We will *not* use transaction mode.

**Decision: connect via Supavisor session mode pooler**, using the
pooler connection string Supabase gives per-project (format roughly
`postgres://[user].[project-ref]:[password]@[pooler-host]:5432/postgres`).
This is the safest default regardless of final hosting choice, and avoids
hitting IPv6-only direct connections from a host that doesn't support it.

### 8.5.3 HikariCP tuning (critical — must not use Spring Boot defaults)

Spring Boot's default HikariCP pool size is **10 connections per app
instance**. That's fine against a normal dedicated Postgres box, but
against Supabase's shared pooler — capped at 200 *total* client
connections across potentially other projects/tools sharing that
tier — we should run lean on purpose:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://<pooler-host>:5432/postgres
    username: postgres.<project-ref>
    password: ${SUPABASE_DB_PASSWORD}
    hikari:
      maximum-pool-size: 5      # this app is low-traffic (2-3 family users); no need for 10+
      minimum-idle: 1
      connection-timeout: 10000
      idle-timeout: 300000
      max-lifetime: 1800000     # recycle connections every 30 min, pairs well with pooler-side timeouts
```

Reasoning: 2-3 concurrent family users will never need 10 simultaneous DB
connections. Keeping the pool small (a) leaves headroom under the 200-client
pooler cap for future use (admin dashboard, a second environment, etc.) and
(b) avoids Spring Boot opening connections it doesn't need and holding them
idle.

Also set, since `pgbouncer`/Supavisor session mode doesn't support every
Postgres feature Hibernate might try by default:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          lob:
            non_contextual_creation: true   # avoids known issue with pooled Postgres + Hibernate LOB handling
```

### 8.5.4 Handling the 7-day inactivity pause

This is the one free-tier behavior that can actually break the app for a
family that doesn't open it daily. Options, in order of recommendation:

1. **Lightweight keep-alive ping** — a free scheduled job (GitHub Actions
   cron, or Uptime Robot free tier) that hits a trivial backend health
   endpoint (`GET /api/health`, which itself does a cheap `SELECT 1`)
   roughly every 3–4 days. Costs nothing, keeps the project warm,
   doesn't require touching Supabase's own dashboard.
2. Accept the pause and manually resume from the Supabase dashboard when
   it happens — fine for a true low-stakes hobby project, but a paused DB
   means the app is fully down until someone notices and clicks resume,
   which is a bad experience for a "family checks it weekly" app.
3. Upgrade to Pro ($25/mo) later if the app proves useful and the family
   wants zero-maintenance reliability — explicitly **not** a v1 decision,
   just noting the upgrade path exists.

**Recommendation for v1: option 1**, a free GitHub Actions cron hitting
the health endpoint every 3 days. Cheap insurance, no new paid services.

### 8.5.5 Backups

Free tier has **no automatic backups** — if the Supabase project is ever
deleted, paused-and-corrupted, or hits the 500 MB cap and something goes
wrong, family purchase history could be lost. Since this is real financial
history, not throwaway data:

- Add a simple scheduled `pg_dump` (via GitHub Actions, same pattern as
  the keep-alive ping) exporting to a free storage target (e.g. a private
  GitHub repo artifact, or free-tier Cloudflare R2/S3) on a weekly
  cadence. This is a small addition to the same GitHub Actions workflow
  already running the keep-alive ping — not a separate system.
- Out of scope to build in v1's first pass, but flagged here so it isn't
  forgotten before real data accumulates.

### 8.5.6 Migrations

Use **Flyway** (pairs natively with Spring Boot) for schema migrations
against the Supabase DB, applied via the same pooler session-mode
connection. Keeps schema changes versioned and repeatable rather than
hand-edited through the Supabase SQL editor, which avoids drift between
what's in source control and what's actually live.

### 8.5.7 Environment / secrets handling

- `SUPABASE_DB_PASSWORD` (and full connection string) stored as an
  environment variable / secret on whatever host runs Spring Boot —
  never committed to the repo.
- Local dev: a `.env`-style local Postgres (or a Supabase free "dev"
  project, since 2 active projects are allowed) keeps local development
  from touching production family data.

---

## 10. Suggested Build Order

1. **Supabase setup**: create free project, grab pooler (session mode)
   connection string, configure HikariCP per Section 8.5.3, verify Spring
   Boot connects and Flyway can apply an empty baseline migration.
2. Backend skeleton: entities, repositories, Flyway migration for schema,
   basic CRUD controllers (Profile, Category, Item, Purchase).
3. Auth: PIN hashing + JWT login flow.
4. Analytics endpoints (read-only queries from Section 7).
5. React skeleton: routing, profile picker, PIN pad, JWT storage.
6. Add Purchase screen (core loop) end-to-end against real backend.
7. Dashboard + Category View + Item Detail (charts via Recharts).
8. Polish: empty states, loading states, mobile responsiveness pass.
9. Keep-alive + backup GitHub Action (Section 8.5.4, 8.5.5).
10. Deployment: Dockerize backend, build React static bundle, deploy to
    chosen cloud target (decide provider when we get here).

---

## 11. Open Questions for Later (not blocking v1 build)

- Exact PIN length/policy (4 vs 6 digit)?
- Should `Category` colors be auto-assigned or user-picked?
- Currency — assuming ₹ (INR) throughout; confirm formatting (₹1,234 vs
  ₹1,234.00).
- Cloud provider choice for final deploy (Railway/Render/EC2/etc.) —
  affects Dockerfile and env config specifics, not the app design itself.
  Note: check whether the chosen host supports outbound IPv6 — if yes,
  direct Supabase connection becomes an option instead of pooler-only.
- Confirm before Oct 30, 2026: Supabase is rolling out a requirement for
  explicit Postgres grants for PostgREST/Data-API access on free projects.
  We're not using the auto-generated Data API (Spring Boot talks straight
  to Postgres via JDBC), so this likely doesn't affect us — but worth a
  quick re-check closer to that date in case scope changes.
