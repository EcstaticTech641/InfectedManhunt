<div align="center">

# 🧟 Infected Manhunt (RGA Companion)

### Beat the Ender Dragon before the infection takes everyone

</div>

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4%2B-28A745?style=for-the-badge&logo=minecraft)
![Java](https://img.shields.io/badge/Java-25-007396?style=for-the-badge&logo=openjdk)
![Paper](https://img.shields.io/badge/Server-Paper%2026.2-2875D7?style=for-the-badge)
![RGA API](https://img.shields.io/badge/RGA%20API-1.13.0--SNAPSHOT-8A2BE2?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-1.0.0--SNAPSHOT-FFC107?style=for-the-badge)

</div>

> **Infected Manhunt** is an automated companion minigame plugin for **Ronlab Game Assistant (RGA)**. One player starts infected. Every runner who is tagged or dies joins the hunt. Can the survivors defeat the Ender Dragon before the infection spreads to everyone — or will the infected team grow too powerful to stop?

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
5. **Session Conclusion**: When the Ender Dragon dies (Speedrunners win) or all Speedrunners are infected (Infected win), `InfectedManhunt` requests graceful session teardown via `requestSessionConclude(...)`, allowing RGA Core to restore player inventories and return players to the hub.

---

## ✨ Features

| Feature | Description |
|:--|:--|
| 🔌 **RGA Core Integration** | Zero direct inventory or game mode manipulation — relies entirely on RGA lifecycle events and `RGASessionControl` |
| 🧟 **Infection on Tag / Death** | Speedrunners who are tagged or die instantly switch to the infected team and begin hunting |
| 🧭 **Filtered Compass Tracking** | Infected players receive auto-updating compasses that filter out spectators via `RGASessionControl.isSpectator()` |
| 🌌 **Cross-Dimension Fallback** | Tracking compasses automatically target last-known portal locations when runners enter Nether/End |
| ⚡ **Thread-Safe & Immutable** | State stored in `ConcurrentHashMap` with immutable copy exports (`List.copyOf()`, `Map.copyOf()`) |
| ⚙️ **Configurable Settings** | Adjust tracking update frequency, last-known location notifications, and team colors in `config.yml` |

---

## 🚀 Quick Start Guide

### Prerequisites
- **Java 25** JDK / JRE
- **Paper 26.2+** server (or Paper-derived software)
- **Ronlab Game Assistant (`RonlabGameAssistant`)** installed on the server

### Installation Steps

1. **Install Plugins**:
   - Place `RonlabGameAssistant.jar` and `infectedmanhunt-1.0.0-SNAPSHOT.jar` into your server's `plugins/` directory.

2. **Configure RGA Minigame**:
   - Ensure `minigames.yml` in `plugins/RonlabGameAssistant/` includes an entry with `id: infectedmanhunt` (or `infected_manhunt`).

3. **Start Server**:
   - Start the server. `InfectedManhunt` will load with `load: BEFORE` and bind to `RGASessionControl`.

4. **Play**:
   - Create a party and queue for Infected Manhunt via RGA commands or lobby menus (`/hub`, `/rga`).
   - Match lifecycle, tracking, and session teardown are handled automatically.

---

## ⚙️ Configuration (`config.yml`)

Settings live in `plugins/InfectedManhunt/config.yml`:

```yaml
start:
  announce_titles: true

respawn:
  mode: vanilla

tracking:
  update_ticks: 20
  message_cooldown_seconds: 5
  notify_last_known: true

teams:
  speedrunner_color: "GREEN"
  infected_color: "RED"
  speedrunner_prefix: "§a[R] "
  infected_prefix: "§c[INF] "
```

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
