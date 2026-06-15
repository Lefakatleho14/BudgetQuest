# BudgetQuest 💰🎮

> A gamified personal budgeting Android application built with Kotlin, Room Database, and MPAndroidChart.

---

## 👥 Team Members

| Name | Role |
|---|---|
| Lefa | Group Leader — Authentication, Dashboard, Date Filtering, Integration |
| Kago | Input Validation, Category Feature, Database Architecture |
| Bokang | Add Expense Screen, Registration Flow, Budget Goals |
| Mabizela | Dashboard Design, Image Attachment, Category Totals, Admin Feature |

---

## 📱 App Overview

BudgetQuest is an Android budgeting application designed to make personal finance management engaging and rewarding. The app combines practical expense tracking with gamification elements — awarding XP, unlocking badges, tracking streaks, and visualising spending data through interactive charts.

The app targets users who want a simple, intuitive way to monitor their spending without the complexity of traditional finance tools. All data is stored locally on the device using Room Database — no internet connection is required and no user data ever leaves the phone.

---

## 🎯 Purpose

The primary purpose of BudgetQuest is to:

- Help users track daily, weekly, and monthly expenses across custom categories
- Set and monitor minimum and maximum monthly budget goals
- Provide visual feedback on spending habits through charts and progress indicators
- Motivate consistent budgeting behaviour through a gamification reward system
- Give administrators oversight of all registered users and their spending patterns

---

## ✅ Features

### Core Features
- **User Authentication** — Register and login with username and password stored locally
- **Category Management** — Add, edit, and delete custom spending categories; default categories seeded on registration
- **Expense Tracking** — Log expenses with amount, date, description, category, and optional image attachment
- **Budget Goals** — Set minimum and maximum monthly spending targets
- **Expense List** — View all expenses with date range filtering
- **Category Totals** — Visual breakdown of spending per category with progress bars and percentages

### Gamification Features
- **XP System** — Earn experience points for logging expenses, creating categories, setting goals, and maintaining streaks
- **Level Progression** — Level up every 100 XP; level displayed on Dashboard and Achievements screen
- **Achievement Badges** — 8 unlockable badges with popup notifications when earned
- **Daily Streak Tracking** — Consecutive daily usage tracked and rewarded with streak badges
- **Achievement Popup** — Celebratory dialog shown immediately when a badge is unlocked

### Chart & Visualisation Features
- **Spending Chart** — Bar chart showing amount spent per category over a user-selectable date range
- **Goal Limit Lines** — Minimum (teal dashed) and maximum (red dashed) goal lines overlaid on the bar chart
- **Budget Progress Card** — Colour-coded progress bar showing how much of the monthly budget has been used

### Admin Features
- **Admin Dashboard** — Overview of all registered users with total spending and expense counts
- **User Expense View** — Admin can drill into any user's full expense history
- **Role-Based Routing** — Logging in as `admin` automatically routes to the Admin Dashboard

---

## 🚀 Innovative Features

> These two features were designed and built beyond the standard assignment requirements. The lecturer should look out for these specifically when evaluating the submission.

---

### 🔥 Feature 1 — Daily Usage Streak Tracking

**Location in app:** Dashboard → Achievements card (streak count) + Achievements screen (streak display)

**What it does:**
BudgetQuest tracks how many consecutive days the user opens and uses the app. Each time the user opens the Dashboard, the app compares today's date against the last recorded active date stored in the User entity. If the user was active yesterday, the streak increments by one. If they missed a day, the streak resets to 1. The longest ever streak is also stored separately and displayed on the Achievements screen.

**Why it is innovative:**
Streak tracking is a proven behavioural design pattern used by apps like Duolingo and Snapchat to build daily habits. Applying it to budgeting encourages users to open the app every day rather than only when they remember to log something. This consistency leads to more accurate expense records and better financial awareness over time.

**Technical implementation:**
- `User` entity stores `currentStreak`, `longestStreak`, and `lastActiveDate` (format: `yyyy-MM-dd`)
- `AchievementRepository.updateDailyStreak()` runs on every Dashboard open
- Streak milestones (3 days, 7 days) unlock dedicated badges (`STREAK_3`, `STREAK_7`)
- If the streak milestone badge is unlocked, a popup appears immediately

**Files involved:**
- `data/db/entity/User.kt`
- `data/repository/AchievementRepository.kt`
- `ui/dashboard/DashboardActivity.kt`
- `ui/achievements/AchievementsActivity.kt`

---

### ⚠️ Feature 2 — Overspending Alerts via Budget Progress Visualisation

**Location in app:** Dashboard (budget status line) + Spending Chart screen (progress card)

**What it does:**
BudgetQuest provides real-time visual alerts when a user's spending approaches or exceeds their set budget goals. On the Dashboard, a colour-coded status line beneath the monthly total immediately communicates the user's budget position. On the Spending Chart screen, a dedicated Budget Progress card shows a progress bar that changes colour dynamically:

- 🟠 **Orange** — Spending is below the minimum goal (not yet on track)
- 🟢 **Green** — Spending is within the min–max goal range (on track)
- 🔴 **Red** — Spending has exceeded the maximum goal (over budget)

Additionally, the bar chart on the Spending Chart screen overlays two dashed limit lines directly on the bars — a teal dashed line for the minimum goal and a red dashed line for the maximum goal — making it visually obvious at a glance which categories are contributing most to overspending.

**Why it is innovative:**
Rather than passive number displays, this feature uses colour psychology and visual anchoring to make the user's financial position immediately obvious without requiring any interpretation. The dual-mode approach (Dashboard status + Chart overlay) means the user sees budget feedback in context — both at a high level and broken down per category.

**Technical implementation:**
- `BudgetGoalRepository.getBudgetGoal()` fetches the user's min/max goals
- `ExpenseRepository.getTotalForMonth()` fetches the current month's total in real time
- `SpendingChartActivity` adds `LimitLine` objects to the chart's left Y-axis via MPAndroidChart
- `ProgressBar.progressTintList` is set dynamically based on the spending position
- `DashboardActivity.loadDashboardData()` also checks the budget position and triggers the `WITHIN_BUDGET` achievement if applicable

**Files involved:**
- `data/repository/BudgetGoalRepository.kt`
- `data/repository/ExpenseRepository.kt`
- `ui/charts/SpendingChartActivity.kt`
- `ui/dashboard/DashboardActivity.kt`
- `res/layout/activity_spending_chart.xml`

---

## 🏗️ Architecture & Design Considerations

### Architecture Pattern
BudgetQuest follows a **layered architecture** pattern:

```
UI Layer (Activities)
       ↓
Repository Layer (Business logic + validation)
       ↓
DAO Layer (Database queries)
       ↓
Room Database (SQLite)
```

This separation ensures that activities never interact with the database directly, all validation lives in one place, and the codebase remains easy to navigate and extend.

### Database Design
Room Database with 6 tables:

| Table | Purpose |
|---|---|
| `users` | Stores credentials, role, XP, level, streak data |
| `categories` | Per-user spending categories with FK to users |
| `expenses` | Expense records with FK to users and categories |
| `budget_goals` | One min/max goal row per user |
| `achievements` | Static catalog of all available badges |
| `user_achievements` | Junction table tracking which badges each user has unlocked |

Foreign key constraints enforce data integrity. `CASCADE` delete on User removes all related data. `SET_NULL` on Category deletion preserves expenses as uncategorised.

### UI Design Decisions
- **ConstraintLayout** used throughout for responsive, overlap-free layouts
- **ScrollView** wrapping ConstraintLayout on all form screens prevents content being cut off on small screens
- **CardView** used for all content sections to create visual depth and grouping
- **Material Components** theme provides consistent button styles, ripple effects, and colour tokens
- **Purple + Teal** colour palette chosen for high contrast and a modern, trustworthy feel
- **Vector drawables** used for all icons to ensure crisp rendering at any screen density
- **Radio buttons** for category selection (replacing spinner) so all options are visible at once
- **Default categories** seeded on registration so new users are never faced with an empty list

### Technical Decisions
- **Kotlin Coroutines** with `Dispatchers.IO` for all database operations — the UI thread never blocks
- **Kotlin Flow** for live-updating lists (categories, budget goals) — UI reacts automatically to data changes
- **`.first()`** extension used for one-shot Flow collection inside `withContext(Dispatchers.IO)` — avoids the illegal pattern of collecting inside `withContext`
- **KSP** (Kotlin Symbol Processing) instead of KAPT for Room code generation — faster build times
- **SharedPreferences** via `SessionManager` for lightweight session persistence between app restarts
- **MPAndroidChart v3.1.0** for the bar chart — chosen for its `LimitLine` support which is essential for the goal overlay feature

---

## 📂 Project Structure

```
app/src/main/
├── java/com/budgetquest/app/
│   ├── data/
│   │   ├── db/
│   │   │   ├── entity/          ← User, Category, Expense, BudgetGoal,
│   │   │   │                       Achievement, UserAchievement
│   │   │   ├── dao/             ← UserDao, CategoryDao, ExpenseDao,
│   │   │   │                       BudgetGoalDao, AchievementDao
│   │   │   └── BudgetQuestDatabase.kt
│   │   └── repository/          ← UserRepository, CategoryRepository,
│   │                               ExpenseRepository, BudgetGoalRepository,
│   │                               AchievementRepository
│   ├── ui/
│   │   ├── auth/                ← LoginActivity, RegisterActivity
│   │   ├── dashboard/           ← DashboardActivity
│   │   ├── category/            ← CategoryActivity, CategoryAdapter
│   │   ├── expense/             ← AddExpenseActivity, ExpenseListActivity,
│   │   │                           ExpenseAdapter
│   │   ├── budget/              ← BudgetActivity
│   │   ├── reports/             ← CategoryTotalsActivity, CategoryTotalAdapter
│   │   ├── achievements/        ← AchievementsActivity, BadgeAdapter,
│   │   │                           AchievementUnlockedDialog
│   │   ├── charts/              ← SpendingChartActivity
│   │   └── admin/               ← AdminDashboardActivity,
│   │                               AdminUserExpensesActivity, AdminUserAdapter
│   └── utils/
│       └── SessionManager.kt
└── res/
    ├── layout/                  ← All XML layouts
    ├── drawable/                ← Vector icons, shape backgrounds
    ├── values/                  ← colors.xml, strings.xml, themes.xml, dimens.xml
    └── xml/                     ← backup_rules.xml, data_extraction_rules.xml
```

---

## 🔧 Setup & Installation

### Prerequisites
- Android Studio Hedgehog or later
- Android device or emulator running API 24 (Android 7.0) or higher
- JDK 17

### Steps
1. Clone the repository:
```bash
   git clone https://github.com/YOUR_USERNAME/BudgetQuest.git
```
2. Open Android Studio → **File → Open** → select the `BudgetQuest` folder
3. Wait for Gradle sync to complete
4. Connect a physical Android device via USB (enable USB debugging) or start an emulator
5. Press **Run ▶** or use `Shift + F10`

### First Run
1. Tap **Register here** on the Login screen
2. Create a normal user account with any username (not `admin`)
3. You will be taken directly to the Dashboard with 6 default categories already created
4. To access the Admin Dashboard, register a separate account with the username `admin`

---

## 🔑 Admin Access

| Field | Value |
|---|---|
| Username | `admin` (case-insensitive) |
| Password | Set by you during registration — choose anything |
| Access | Automatically granted based on username at registration time |

---

## 📊 GitHub Actions

This project uses GitHub Actions for **Continuous Integration (CI)** to automatically build and verify the app on every push and pull request to the `main` branch.

### Workflow file location
`.github/workflows/android_ci.yml`

### What the workflow does
1. Checks out the repository code
2. Sets up JDK 17
3. Grants execute permission to the Gradle wrapper
4. Runs `./gradlew assembleDebug` to compile the app
5. Uploads the generated APK as a build artifact

### How to set it up
Create the file `.github/workflows/android_ci.yml` in your repository with the following content:

```yaml
name: Android CI

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: BudgetQuest-Debug-APK
          path: app/build/outputs/apk/debug/app-debug.apk
```

### Why GitHub Actions matters
Using CI means every code change is automatically verified to compile correctly before it is merged. This prevents broken code from reaching the main branch and gives the team confidence that the build is always in a working state. The uploaded APK artifact also means any team member or the lecturer can download and install the latest build directly from the GitHub Actions tab without needing Android Studio.

---

## 📦 Dependencies

| Library | Version | Purpose |
|---|---|---|
| AndroidX Core KTX | 1.13.1 | Kotlin extensions for Android |
| AppCompat | 1.7.0 | Backwards-compatible UI components |
| Material Components | 1.12.0 | Material Design UI elements |
| ConstraintLayout | 2.1.4 | Responsive XML layouts |
| Room Runtime + KTX | 2.6.1 | Local SQLite database with coroutine support |
| KSP | 1.9.23 | Kotlin Symbol Processing for Room code gen |
| Lifecycle LiveData + ViewModel | 2.8.2 | Lifecycle-aware data observation |
| Kotlin Coroutines Android | 1.8.0 | Async operations off the main thread |
| RecyclerView | 1.3.2 | Scrollable lists |
| CardView | 1.0.0 | Material card containers |
| MPAndroidChart | 3.1.0 | Bar charts with limit line support |

---

## 📄 License

This project was developed as a final year university assignment at **IIE Rosebank International**.
All rights reserved © 2026 Lefa, Kago, Bokang, Mabizela.
