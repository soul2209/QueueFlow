# 🌊 QueueFlow

<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&size=26&duration=2800&pause=900&color=58A6FF&center=true&vCenter=true&width=600&lines=Welcome+to+QueueFlow;Smarter+Queues%2C+Smoother+Service;Smart+Virtual+Queue+%26+Token+Management" alt="Animated QueueFlow introduction" />
</p>

<p align="center">
  <strong>A smart virtual queue and token management system with a Java console app and a browser-based web interface.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Language-Java-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java"/>
  <img src="https://img.shields.io/badge/Web-HTML%20%7C%20CSS%20%7C%20JS-58A6FF?style=for-the-badge" alt="HTML, CSS, JS"/>
  <img src="https://img.shields.io/badge/Status-Development-8957E5?style=for-the-badge" alt="Development"/>
</p>

---

## ✨ About the Project

**QueueFlow** replaces physical waiting lines with virtual tokens. Customers pick a service, get a token (like `B-001`), and can check their position and estimated wait at any time. Staff call, complete, or skip customers at their counters, and admins set up services and counters and view statistics.

The project comes in two parts that share the same rules and demo data:

- ☕ **Java console application** — the object-oriented core (`Main.java` + queue classes).
- 🌐 **Web interface** — a dashboard (`index.html`, `style.css`, `script.js`) that runs entirely in the browser with no server or build step.

## ⚡ Features

- 🎟️ **Virtual tokens** — Service-prefixed IDs such as `B-001`, `E-002`, `R-003`.
- 🚦 **Priority queueing** — Three levels (Normal, Senior citizen, Emergency). Higher priority is served first; within the same priority it is strictly first-come-first-served.
- ⏱️ **Smart wait estimates** — Position in line and estimated wait time, based on a running average of real service times and the number of open counters (default 3 min per customer, never below 1 min).
- 🏢 **Multiple services and counters** — Add services and open as many counters per service as needed. A token cannot be issued for a service with no open counter.
- 👥 **Role-based flows** — Separate Customer, Staff, and Admin actions.
- 🚫 **No-show handling** — Staff can skip a called customer who does not turn up.
- ❌ **Token cancellation** — Customers can cancel while still waiting.
- 📊 **Live statistics** — Tokens issued, served, skipped, cancelled, waiting counts, average service time, and counters per service.
- 🧾 **CSV audit log** — Every served, skipped, or cancelled token is logged to `queueflow_log.csv`.
- 🛡️ **Input validation** — Application-specific errors via `QueueFlowException` (Java) and matching checks in the web UI.

## 🔄 Token Lifecycle

```text
 WAITING ──call next──▶ SERVING ──complete──▶ SERVED
    │                       │
    │ cancel                └──skip (no-show)──▶ SKIPPED
    ▼
 CANCELLED
```

## 🎬 How It Flows

<p align="center">
  <img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&size=17&duration=2200&pause=700&color=3FB950&center=true&vCenter=true&width=600&lines=Get+a+Token;Wait+in+the+Virtual+Queue;Get+Called+to+a+Counter;Served+and+Logged" alt="Animated queue workflow"/>
</p>

```text
   ┌─────────────────────────┐        ┌─────────────────────────┐
   │  Main.java (Console UI) │        │  index.html + script.js │
   │  Customer/Staff/Admin   │        │  Web dashboard          │
   └────────────┬────────────┘        └─────────────────────────┘
                │                       (standalone, same logic in JS)
                ▼
   ┌─────────────────────────┐
   │      QueueManager       │  ← rules, validation, stats, CSV log
   └────────────┬────────────┘
        ┌───────┴────────┐
        ▼                ▼
 ┌──────────────┐  ┌────────────┐
 │ ServiceQueue │  │  Counter   │
 │ (priority Q) │  │ serves one │
 └──────┬───────┘  │ token at a │
        ▼          │   time     │
 ┌──────────────┐  └────────────┘
 │    Token     │
 └──────────────┘
```

*Conceptual overview of the main project components.*

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java (no external libraries) | Core queue logic and console application |
| HTML, CSS, JavaScript (vanilla) | Browser-based web interface |
| Object-Oriented Programming | Modular design across separate classes |
| CSV | Simple persistence and audit trail |

## 🚀 Getting Started

### Prerequisites

- Java Development Kit (JDK) installed (for the console app).
- Any modern web browser (for the web interface).
- A terminal or Java IDE, such as IntelliJ IDEA, Eclipse, or VS Code.

Verify your Java installation:

```bash
java -version
javac -version
```

### Clone the Repository

```bash
git clone https://github.com/soul2209/QueueFlow.git
cd QueueFlow
```

### Run the Java Console App

```bash
javac *.java
java Main
```

The app starts with demo data already loaded:

| Service | Prefix | Counters |
|---|---|---|
| Billing | `B` | 2 |
| Enquiry | `E` | 1 |
| Registration | `R` | 1 |

Console menu:

| Role | Options |
|---|---|
| **Customer** | 1. Get a token · 2. Check token status · 3. Cancel token |
| **Staff** | 4. Call next · 5. Complete current · 6. Skip current (no-show) · 7. View queue |
| **Admin** | 8. Add service · 9. Add counter · 10. Stats |
| | 0. Exit |

### Run the Web Interface

No installation or server is needed. Open `index.html` in your browser:

```bash
# macOS
open index.html
# Linux
xdg-open index.html
# Windows
start index.html
```

The web interface has three pages and a live side panel:

- **Customer** — Get a token, choose a priority, check status, or cancel a token.
- **Staff desk** — Select a counter, then call next, complete, or skip. Clicking a counter card jumps straight to it.
- **Admin** — Add services, open counters, view statistics, and download the log as `queueflow_log.csv`.
- **Live queue and counters** — Always-visible queue per service and the status of every counter, plus header metrics for waiting, served, and busy counters.

> **Note:** The web interface keeps its data in browser memory and is independent of the Java app. Refreshing the page resets it to the same demo data. Use the **Download log (CSV)** button in Admin to save the session's results.

## 📁 Project Structure

```text
QueueFlow/
├── Main.java                 # Console UI: menu, input handling, demo data
├── QueueManager.java         # Core logic: services, counters, tokens, stats, CSV logging
├── ServiceQueue.java         # Priority queue per service + wait-time estimation
├── Token.java                # Token model with Priority and Status enums
├── Counter.java              # Service counter that serves one token at a time
├── QueueFlowException.java   # Checked exception for business-rule violations
├── index.html                # Web interface layout
├── style.css                 # Web interface styling
├── script.js                 # Web interface logic (mirrors the Java rules)
└── README.md
```

`queueflow_log.csv` is created automatically in the working directory the first time a token is served, skipped, or cancelled.

### Log format

Each row of the CSV log has the following columns, with no header:

```text
timestamp, token_id, customer_name, priority, outcome, service_seconds
```

`outcome` is one of `SERVED`, `SKIPPED`, or `CANCELLED`.

## 🎯 Project Goals

- Build a structured queue management application in Java.
- Practice object-oriented programming and modular design.
- Keep queue rules independent from the user interface.
- Offer both a console and a web front end for the same workflow.
- Make queue-related operations easier to understand and maintain.

## 🔮 Future Improvements

- [ ] Connect the web interface to the Java logic through a backend API.
- [ ] Persistent storage so queue state survives restarts.
- [ ] Public display board showing the token currently being served.
- [ ] Customer notifications (SMS or email) when their turn is near.
- [ ] Per-service reports and analytics from the CSV log.
- [ ] Automated tests for queue operations.

## 👨‍💻 Author

**Dawn Dsouza**

GitHub: [@soul2209](https://github.com/soul2209)

Project: [QueueFlow](https://github.com/soul2209/QueueFlow)

---

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:8957E5,100:58A6FF&height=100&section=footer" alt="Animated gradient footer"/>
</p>

<p align="center">
  <i>Organize the queue. Simplify the flow. 🌊</i>
</p>
