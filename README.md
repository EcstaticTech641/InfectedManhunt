<div align="center">

# 🧟 Infected Manhunt (RGA Companion Plugin)

### Beat the Ender Dragon before the infection takes everyone

</div>

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4%2B-28A745?style=for-the-badge&logo=minecraft)
![Java](https://img.shields.io/badge/Java-25-007396?style=for-the-badge&logo=openjdk)
![Paper](https://img.shields.io/badge/Server-Paper%2026.2-2875D7?style=for-the-badge)
![RGA API](https://img.shields.io/badge/RGA%20API-1.13.0--SNAPSHOT-8A2BE2?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-1.0.0--SNAPSHOT-FFC107?style=for-the-badge)

</div>

> **Infected Manhunt** is an automated companion minigame plugin for **Ronlab Game Assistant (RGA)** adhering strictly to the **Micro-Companion Architecture (CPMK)** standard. One player starts infected. Every runner who is tagged or dies joins the hunt. Can the survivors defeat the Ender Dragon before the infection spreads to everyone — or will the infected team grow too powerful to stop?

---

## 🧟 How It Works

This companion plugin hooks directly into RGA Core lifecycle events (`MinigameStartEvent` and `RGAGameRequestConcludeEvent`) to manage gameplay without needing standalone world management or manual inventory/spectator handling.

| Team | Goal | Win Condition |
|:--|:--|:--|
| 🏃 **Runners** | Defeat the Ender Dragon | Kill the dragon before everyone is infected |
| 🧟 **Infected** | Tag and convert every runner | Convert all runners to the infected team |

### Lifecycle Flow
1. **Match Provisioning**: RGA Core generates the session world and fires `MinigameStartEvent`.
2. **Role Allocation**: `InfectedManhunt` assigns the initial Infected hunter and Speedrunners.
3. **PvP Tagging & Death**: When an Infected tags or kills a Speedrunner, the victim is converted to `INFECTED`.
4. **Compass Tracking**: Active Infected hunters receive lodestone-tracked compasses that point to the nearest active Speedrunner across dimensions.
5. **Session Conclusion**: When the Ender Dragon dies (Speedrunners win) or all Speedrunners are infected (Infected win), `InfectedManhunt` requests graceful session teardown via `RGAGameRequestConcludeEvent`, allowing RGA Core to restore player inventories and return players to the hub.

---

## ✨ Features & Architecture (CPMK 5 Pillars)

| Feature | Description |
|:--|:--|
| 🔌 **RGA Core Integration** | Operates cleanly via `MinigameStartEvent` and `RGAGameRequestConcludeEvent` through the `rga-api` event bus |
| 🧟 **Infection on Tag / Death** | Speedrunners who are tagged or die instantly switch to the infected team and begin hunting |
| 🧭 **Filtered Compass Tracking** | Infected players receive auto-updating compasses that filter out spectators via `RGASessionControl.isSpectator()` |
| 🌌 **Cross-Dimension Fallback** | Tracking compasses automatically target last-known portal locations when runners enter Nether/End |
| 🧪 **Solo QA Developer Mode** | Freeze win condition evaluation when `initialPlayerCount == 1` for continuous solo testing of map resets and mechanics |
| ⚡ **Thread-Safe & Immutable** | State stored in `ConcurrentHashMap` with immutable copy exports (`List.copyOf()`, `Map.copyOf()`) |
| ⚙️ **Configurable Schemas** | Detailed vector, spawn, time-limit, and color configurations in `config.yml` and `arena.yml` |

---

## 🧪 Solo QA Developer Mode (`initialPlayerCount == 1`)

When a session starts with only 1 player (solo QA testing), win condition evaluation automatically freezes. This prevents the session from concluding immediately due to zero remaining speedrunners/infected, enabling developers and testers to validate map reset routines, tracking compass updates across dimensions, and arena mechanics continuously.

---

## 🔑 Commands & Permissions

- `/infected reload` (`infectedmanhunt.admin`) — Reload `config.yml` and `arena.yml` settings.
- `/infected status` (`infectedmanhunt.admin`) — View active sessions and participant breakdown.
- `/infected end <world>` (`infectedmanhunt.admin`) — Forcefully conclude session in specified world.

---

## 🚀 Quick Start Guide

### Prerequisites
- **Java 25** JDK / JRE
- **Paper 26.2+** server
- **Ronlab Game Assistant (`RonlabGameAssistant`)** installed on the server

### Installation Steps

1. **Install Plugins**:
   - Place `RonlabGameAssistant.jar` and `infectedmanhunt-1.0.0-SNAPSHOT.jar` into your server's `plugins/` directory.

2. **Configure RGA Minigame**:
   - Ensure `minigames.yml` in `plugins/RonlabGameAssistant/` includes an entry with `id: infectedmanhunt`.

3. **Start Server**:
   - Start the server. `InfectedManhunt` will bind to `RonlabGameAssistant` via Paper 26.2 dependency declarations.

4. **Play**:
   - Queue for Infected Manhunt via RGA commands or lobby menus (`/hub`, `/rga`).

---

## 📖 Comprehensive Documentation

For complete technical specifications, scoreboard lifecycle management, event bus contracts, and administrative configuration schemas, consult the [USER_GUIDE.md](file:///m:/projects/InfectedManhunt/USER_GUIDE.md).

---

## 🔨 Building From Source

Requires Java 25 and Maven 3.9+:

```bash
mvn clean package
```

The compiled plugin jar will be generated at `target/infectedmanhunt-1.0.0-SNAPSHOT.jar`.

---

<div align="center">

**InfectedManhunt Companion Plugin • Ronlab Game Assistant Ecosystem**

</div>
