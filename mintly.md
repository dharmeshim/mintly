# 📱 Expense Tracker – Android App Requirements

---

## 🧭 1. Objective

Design and develop a **lightweight, modern Android application** that enables users to record daily expenses with maximum speed and clarity, while keeping the interface extremely minimal, timeless, and distraction-free.

The app should feel **calm, fast, and premium**, inspired by design systems like *Nothing*, *Notion*, and modern fintech products.

---

## 🎨 2. Design Philosophy

| Area | Requirement |
|-----|------------|
| Visual Style | **Ultra-minimal, clean, timeless** |
| UI Elements | No icons, no illustrations, no unnecessary graphics |
| Typography | Primary design element |
| Color Usage | Mostly monochrome + 1 subtle accent |
| Motion | Micro-animations only, no visual noise |
| Layout | Airy spacing, balanced proportions |
| App Size | **As small as possible** |
| Complexity | Keep only what is essential |

> Design should feel **invisible**. Content and interaction come first.

---

## 🧩 3. Core Features

### 🧾 A. Add Expense

**Primary Screen (Home)**

**Elements:**
- **Amount** — large, centered numeric input
- **Description** — single-line text field
- **Category** — text-based selector

**Behavior:**
- Auto capture current **date & time**
- Auto suggest category from description keywords
- One-tap save
- Haptic feedback on save
- Last-used category preselected
- Undo last entry

---

### 📅 B. Monthly Calendar View

- Text-based monthly calendar
- Each date displays daily total
- Subtle text emphasis for higher spending days
- Tap a date → view expenses for that day
- Swipe left/right to navigate months
- Monthly total displayed at top

---

### 🗂️ C. Category Management

- Categories are text only (no icons)
- Keyword-based auto classification

| Category | Sample Keywords |
|----------|----------------|
| Food | lunch, dinner, snack, cafe |
| Travel | uber, fuel, bus |
| Shopping | clothes, shoes |
| Bills | electricity, water, rent |
| Health | doctor, medicine |
| Fun | movie, game |

**Features:**
- Add / edit / delete categories
- Assign keywords
- Change category color (subtle dot indicator only)

---

### 💾 D. Data Backup

- One-tap backup
- Local export
- Optional Google Drive sync
- Encrypted storage
- Auto-backup toggle

---

## 🧱 4. App Structure

Bottom navigation — **3 tabs only**

1. Add Expense  
2. Calendar  
3. Categories  

No extra menus. No pop-ups. No clutter.

---

## 🧠 5. Smart Behaviors

- Offline-first
- No login required
- Instant app launch
- Auto-save drafts
- Optional gentle reminder if no expense added in a day

---

## 🛠️ 6. Technical Requirements

| Area | Specification |
|-----|-------------|
| Platform | Android |
| Language | Kotlin |
| Architecture | MVVM |
| Database | Room |
| UI | Jetpack Compose |
| Backup | Google Drive API |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Latest stable |

---

## 🔐 7. Privacy & Trust

- No ads
- No trackers
- No analytics
- No unnecessary permissions
- All data stays with the user

---

## 🚀 8. Future Scope (Hidden)

- Spending insights
- Budgets
- Export to CSV
- Multi-currency
