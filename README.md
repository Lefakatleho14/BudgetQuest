BudgetQuest

BudgetQuest is a gamified personal finance Android application that transforms budgeting into an interactive experience. Instead of boring spreadsheets, users earn insights by tracking expenses, setting budgets, and viewing progress through dashboards, reports, and category analytics.

Features

Expense Management

Add, edit, and view expenses
Attach optional images to transactions
Categorize expenses for better tracking
Filter expenses by date range

Budget System

Set monthly minimum and maximum budget goals
Real-time budget tracking
Visual feedback on spending performance

Analytics & Reports

Category-wise spending breakdown
Percentage contribution per category
Monthly totals and summaries
Progress visualization using charts & bars

Categories

Create and manage custom categories
Assign expenses to categories
Automatic aggregation per category

Dashboard Overview

Monthly spending summary
Total expenses count
Category count
Budget status indicator (under / on track / over budget)

Authentication

User login & session management
Secure local session storage

Gamified UX Elements

Progress-based feedback system
Achievement-style budget tracking mindset
Clean, intuitive dashboard navigation

Tech Stack

Language: Kotlin
Architecture: MVVM-inspired structure (Repository pattern)
Database: Room Persistence Library
Async: Kotlin Coroutines + Flow
UI: XML Views (Material Components)
Components:

RecyclerView
LiveData / LifecycleScope
CardView
ViewBinding-style manual binding

Project Structure

com.budgetquest.app
│
├── data
│   ├── db
│   │   ├── entity
│   │   ├── dao
│   │   └── BudgetQuestDatabase
│   ├── repository
│
├── ui
│   ├── auth
│   ├── dashboard
│   ├── expense
│   ├── budget
│   ├── category
│   └── reports
│
├── utils
│   └── SessionManager


App Screens

*  Login / Register
*  Dashboard
*  Add Expense
*  Expense List (with filters)
*  Categories Management
*  Budget Settings
*  Category Totals Report

Installation & Setup

Prerequisites

* Android Studio Hedgehog or later
* Kotlin 1.9+
* Gradle 8+

Steps

bash
git clone https://github.com/Lefakatleho14/BudgetQuest.git


1. Open project in Android Studio
2. Allow Gradle sync to complete
3. Run on emulator or physical device

Architecture Overview

BudgetQuest follows a clean separation of concerns:

UI Layer: Activities + Adapters
Repository Layer: Data abstraction for Room
Database Layer: Room DAOs + Entities
Utilities: Session handling, helpers

This ensures scalability, maintainability, and testability.

Key Functional Flows

Expense Flow

1. User adds expense → AddExpenseActivity
2. Data stored in Room database
3. Dashboard & reports auto-refresh via Flow

Budget Flow

1. User sets min/max goals
2. Monthly spending compared in real-time
3. UI updates with budget status indicator

### Reporting Flow

1. Expenses grouped by category
2. Totals computed in repository
3. UI displays percentages and charts

Testing

* Unit tests: JUnit
* UI tests: Espresso (configured in Gradle)

Future Improvements

* Firebase authentication integration
* Cloud sync for expenses
* Advanced charts (MPAndroidChart)
* Dark mode enhancement
* AI-based spending insights
* Notification reminders for budgets

Author

Lefa Katleho
GitHub: [Lefakatleho14](https://github.com/Lefakatleho14)
