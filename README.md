# RescueLink: Community Disaster Response and Relief Management System

**Version 1 (authentication foundation)** | PF201: Object-Oriented Programming | Java + SQLite + Maven

RescueLink is a console-based Java system for barangay disaster response and relief recordkeeping. It helps barangay personnel register households, record rescue and assistance requests, monitor evacuation centers, and track relief goods, with role-based access and securely hashed passwords.

> RescueLink supports community coordination and recordkeeping only. It does **not** replace official emergency hotlines (such as 911), professional rescue services, or government warning systems.

---

## What Version 1 includes

Version 1 delivers the secure account foundation that the rest of the system will be built on.

| Feature | Description |
|---|---|
| **Register account** | A new user enters full name, username, password, optional contact details, and chooses a role. The account is saved with status `PENDING`. |
| **Login** | Username and password are checked against the SQLite database. Passwords are verified against a salted hash. Accounts that are `PENDING` or `DEACTIVATED` cannot log in. |
| **Logout / Exit** | Logout ends the session and returns to the main menu. Exit closes the application. They are separate options. |
| **First-run setup** | When no active administrator exists, the app asks you to create the System Administrator account. No default password is stored anywhere in the code. |
| **Account approval** | The administrator can list all users, list pending accounts, activate (approve) an account, and deactivate an account. |
| **Activity log** | Registration, admin setup, login, logout, activation, and deactivation are recorded in the `ActivityLogs` table. |
| **Database** | An SQLite file is created automatically with all nine tables from the proposal. Data persists between runs. |

### Not built yet (planned for later versions)

The nine proposed tables are created now, but only `Users` and `ActivityLogs` are used in Version 1. These modules are **not implemented yet**:

- Household management and vulnerability tagging
- Incident reports and assistance requests
- Evacuation centers and evacuee check-in/check-out
- Relief inventory, donations, and distribution transactions
- Dashboard and the two summary reports
- Profile editing and viewing of activity logs

---

## User roles

RescueLink has four roles, taken from the system proposal.

| Role | Purpose |
|---|---|
| System Administrator / Barangay Official | Manages accounts and permissions, maintains household records, views dashboard, reports, and logs. |
| Disaster Response Personnel | Creates and updates incident reports, records rescue requests, updates response status. |
| Relief Coordinator | Records donations and stock, records distributions, reviews inventory and low-stock alerts. |
| Resident / Requester | Submits assistance requests and views only their own requests. |

- Anyone can self-register as Response Personnel, Relief Coordinator, or Resident. The account stays `PENDING` until an administrator activates it.
- The Administrator role **cannot** be self-registered. It is created only during first-run setup.
- In Version 1, only the Administrator's account-management permission is in use. The remaining permissions are already defined in the `Role` enum and will be enforced as each module is added.

---

## Technology

- Java 17 or newer
- SQLite (via the `sqlite-jdbc` driver)
- Maven
- IntelliJ IDEA (recommended)
- Console menus (no GUI)

---

## Project structure

```text
RescueLink/
├── pom.xml
├── README.md
├── .gitignore
├── data/
│   └── rescuelink.db            (created automatically on first run, not committed)
└── src/main/java/com/rescuelink/
    ├── Main.java                Application entry point
    ├── dao/                     Database connection and SQL
    │   ├── DatabaseConnection.java   Opens connections, creates the 9 tables, transaction helpers
    │   ├── UserDao.java              All SQL for the Users table
    │   ├── ActivityLogDao.java       Writes audit log entries
    │   └── DataAccessException.java
    ├── model/                   Data classes
    │   ├── User.java                 Java Record for an account
    │   ├── Role.java                 The four roles and their permissions
    │   ├── AccountStatus.java        PENDING, ACTIVE, DEACTIVATED
    │   ├── Permission.java
    │   ├── NewAccount.java           Record for a new account before it is saved
    │   └── AccessDeniedException.java
    ├── service/                 Business rules
    │   ├── UserService.java          Registration, login, logout, approval, validation
    │   └── AuthException.java
    ├── ui/                      Console menus
    │   ├── MainMenu.java
    │   ├── AuthenticationMenu.java   First-run setup, register, login
    │   ├── UserMenu.java             Home screen after login
    │   ├── UserManagementMenu.java   Administrator account approval
    │   └── ConsoleInput.java         Safe input helpers
    └── util/
        ├── PasswordHasher.java       Salted PBKDF2 password hashing
        └── DateTimeUtil.java
```

The layers are kept separate: `ui` talks to `service`, `service` talks to `dao`, and `dao` is the only layer that runs SQL.

---

## Database

The SQLite file is stored at `data/rescuelink.db`. It is created automatically the first time the app runs, and existing data is never deleted on restart.

The nine tables follow the proposal: `Users`, `Households`, `Incidents`, `AssistanceRequests`, `EvacuationCenters`, `ReliefInventory`, `ReliefTransactions`, `ActivityLogs`, and `EvacueeRecords`. Foreign keys are enforced on every connection, and CHECK constraints protect values such as roles, statuses, capacity, and stock quantities.

**Users table (the Version 1 table):**

| Column | Purpose |
|---|---|
| `user_id` | Primary key (the "id") |
| `full_name` | Full name |
| `username` | Unique, not case-sensitive |
| `password_hash` | Salted hash (the "password"), never plain text |
| `role` | One of the four roles (the "roles") |
| `contact_details` | Optional |
| `account_status` | `PENDING`, `ACTIVE`, or `DEACTIVATED` (the "status") |
| `created_at` | Registration time |

---

## Security

- All SQL uses `PreparedStatement`.
- Passwords are hashed with PBKDF2-HMAC-SHA256 (600,000 iterations, random 16-byte salt). Plain-text passwords are never stored.
- Login shows the same message for an unknown username and a wrong password.
- Permission checks happen in the data-access layer, which re-reads the acting user from the database. Hiding menu options is not the only protection.
- An administrator cannot deactivate their own account.
- The database file is listed in `.gitignore` so personal records are not published.

---

## How to run

### In IntelliJ IDEA

1. **File > Open** and select the `RescueLink` folder (open it as a Maven project).
2. **File > Project Structure > Project SDK**: choose JDK 17 or newer.
3. Wait for Maven to download the dependencies. If it does not start, right-click `pom.xml` and choose **Maven > Reload project**.
4. Right-click `Main.java` and choose **Run 'Main.main()'**.

Note: IntelliJ's Run window cannot hide typed passwords, so they are visible while typing there. In a real terminal they are hidden.

### From a terminal

```bash
mvn compile exec:java
```

---

## Quick test walkthrough

1. Start the app. Create the administrator account when prompted.
2. Choose **Register account** and register a Resident.
3. Choose **Login** with the resident. You should be told the account is pending.
4. Log in as the administrator, open **User management**, and activate the resident. Then log out.
5. Log in as the resident. It should work now.
6. Stop and restart the app. The accounts are still there and the setup prompt does not appear again.

If you need a clean start while developing, close the app and delete `data/rescuelink.db`. A new database is created on the next run.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Red errors / "cannot resolve" in IntelliJ | Reload Maven (**Maven > Reload project**) and confirm the Project SDK is 17 or newer. |
| `release version 17 not supported` | Your JDK is older than 17. Install a newer JDK and select it in Project Structure. |
| `No suitable driver` or `ClassNotFoundException: org.sqlite` | Maven has not downloaded the SQLite dependency. Reload Maven and check your internet connection. |
| `data/rescuelink.db` not visible | Right-click the project folder and choose **Reload from Disk**. |
| `database is locked` | Close any other program (such as a database viewer) that has the file open. |
| Locked out with no active administrator | Delete `data/rescuelink.db` to run the first-run setup again (this erases all data). |

---

## Scope and boundaries

In scope: centralized community disaster-response and relief recordkeeping with role-based access.

Out of scope: automatic emergency dispatch, live GPS tracking, integration with emergency hotlines, SMS or mass notifications, live weather feeds, government alert integration, online donations or payments, and real-time synchronization across barangays.

---

## Author

Segundino, Jhon Loyd, PF201: OOP, IT2F
