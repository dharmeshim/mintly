# Workspace Instructions & Rules (GEMINI.md)

This file contains coding standards, guidelines, and rules for building the **Family Expense Tracker** project in this workspace.

---

## 1. Project Overview & Architecture
- **Backend**: Spring Boot 3 (Java 21) exposing a stateless REST API under `/api`.
- **Frontend**: React (Vite + TypeScript) mobile-first Single Page Application (SPA).
- **Database**: PostgreSQL (Supabase session-mode pooler or local DB).
- **Security**: PIN-based authentication. Users choose a profile and enter a 4-digit PIN. Successful auth yields a JWT token containing their `profileId`.

---

## 2. Directory Layout
```
d:/mintly/
  ├── .gemini/
  │    └── GEMINI.md               <-- (This file) Workspace instructions
  ├── backend/                     <-- Maven Spring Boot project
  │    ├── pom.xml                 <-- Maven build dependencies
  │    └── src/
  │         └── main/
  │              ├── java/com/familyexpense/...
  │              └── resources/
  │                   ├── application.yml
  │                   └── db/migration/  <-- Flyway SQL migrations
  └── frontend/                    <-- React Vite SPA
       ├── package.json
       ├── vite.config.ts
       ├── src/
       │    ├── components/        <-- Reusable UI components
       │    ├── pages/             <-- Full page components (Dashboard, Login, etc.)
       │    ├── services/          <-- API clients and token storage
       │    ├── index.css          <-- Core premium styling & CSS variables
       │    └── main.tsx
       └── index.html
```

---

## 3. Styling Guidelines (Frontend)
- **Vanilla CSS**: We use custom CSS variables for premium look & feel (modern dark/light modes, harmonious colors, glassmorphism, nice gradients). Do NOT use Tailwind CSS.
- **Typography**: Import modern fonts (e.g., `Outfit` or `Inter` from Google Fonts).
- **Mobile-first**: Maintain standard touch target sizes (at least `44x44px`), responsive grids, bottom navigation layout for easy thumb reach, and standard numeric inputs for numeric fields.
- **Transitions**: Smooth transitions (`all 0.2s ease-in-out`) on buttons, cards, list items, and form inputs.

---

## 4. Backend Rules & Database Settings
- **Spring Boot 3 + Java 21**: Use record types for DTOs where appropriate. Use modern Spring conventions.
- **HikariCP**: Max pool size of 5 for Supabase database.
- **Flyway**: All database schema changes must be written as Flyway migrations under `src/main/resources/db/migration/`. Do not perform hand-edited SQL on the live database.
- **Profile Identification**: Always read the logged-in user's profile ID from the JWT authentication context when creating purchases. Never trust a `profileId` passed in the request body for writes.

---

## 5. Analytics & Derived Views
To keep the database lean and synchronized, do not write computed metrics to tables. Derive them dynamically via queries:
- **Price Delta**: Compare current unit rate to the previous purchase rate for that item.
- **Buying Interval**: Calculate days difference between consecutive purchases of the same item ordered by date.
- **Cost Per Head**: Divide total expenditure in a month by the count of active profiles.
