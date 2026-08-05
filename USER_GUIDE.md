# 🧟 InfectedManhunt — Developer & Administrator User Guide

**InfectedManhunt** is an automated companion minigame plugin for **Ronlab Game Assistant (RGA)** built on the **Micro-Companion Architecture (CPMK)** standard for **PaperMC 26.2** running on **Java 25**.

---

## 📋 Table of Contents
1. [Architectural Overview & CPMK Pillars](#-architectural-overview--cpmk-pillars)
2. [Minigame Mechanics & Rulesets](#-minigame-mechanics--rulesets)
3. [CPMK Event Bus Integration](#-cpmk-event-bus-integration)
4. [Solo QA Developer Mode (`initialPlayerCount == 1`)](#-solo-qa-developer-mode-initialplayercount--1)
5. [Scoreboard & Lifecycle Management](#-scoreboard--lifecycle-management)
6. [Commands & Permission Nodes](#-commands--permission-nodes)
7. [Configuration Schemas](#-configuration-schemas)
8. [Building & Deployment](#-building--deployment)

---

## 🏛 Architectural Overview & CPMK Pillars

`InfectedManhunt` strictly adheres to the 5 Core CPMK Pillars:

1. **Core Gameplay Function Retention:** 100% of custom infection logic, PvP tagging, lodestone tracking, and Ender Dragon victory rules are preserved without modification to native loops.
2. **Ronlab Integration Standard:** Listens strictly to CPMK event bus payloads (`MinigameStartEvent` and `MinigameConcludeEvent` / `RGAGameRequestConcludeEvent`). `paper-plugin.yml` specifies `api-version: '26.2'` and lists `RonlabGameAssistant` under `dependencies.server` with `required: true` and `join-classpath: true`.
3. **Baseline Structure & Rules Provision:** Sidebar scoreboards apply PaperMC's `objective.numberFormat(NumberFormat.blank())` to strip margin numbers. Scoreboard assignment (`player.setScoreboard()`) occurs strictly during post-teleport spawn phases to eliminate chunk-loading hangs. Teardown routines restore players to `getMainScoreboard()` and unregister objectives.
4. **Companion-Type Agnostic Design:** Operates as a self-contained module that does not manipulate `rga-core` internals, communicating purely through `rga-api` event contracts and `RGASessionControl`.
5. **Feature Implementation & Modification Specs:** Fully documents administrative commands, permission nodes, configuration schemas, and Solo QA Developer Mode testing behavior.

---

## 🎮 Minigame Mechanics & Rulesets

### Teams & Objectives
- 🏃 **Speedrunners**: Must progress through the Overworld, Nether, and The End to defeat the Ender Dragon before all runners are infected.
- 🧟 **Infected Hunters**: Spawn with lodestone tracking compasses. Their goal is to tag or kill Speedrunners to convert them into Infected Hunters.

### Infection Triggers
- **PvP Tagging:** When an Infected player damages a Speedrunner via melee or projectile, the Speedrunner is instantly converted to the `INFECTED` team.
- **Player Death:** When a Speedrunner dies from any cause (PvP, fall damage, mobs, environment), they join the `INFECTED` team upon respawn.

### Tracking Compass System
- Infected players receive lodestone compasses targeting active Speedrunners.
- Right-clicking a compass cycles the active target through online, non-spectator Speedrunners.
- **Cross-Dimension Fallback:** When a Speedrunner enters the Nether or The End, hunter compasses automatically lock onto the runner's last-known portal entrance location in the Overworld/End.
- **Spectator Filtering:** Compasses automatically ignore players flagged as spectators via `RGASessionControl.isSpectator(player)`.

---

## 📡 CPMK Event Bus Integration

`InfectedManhunt` delegates session provisioning and teardown to `rga-core`:

```
┌────────────────────────┐         MinigameStartEvent         ┌─────────────────────────┐
│        rga-core        │ ─────────────────────────────────> │     InfectedManhunt     │
└────────────────────────┘                                    └─────────────────────────┘
                                                                           │
                                                                   Gameplay & Infection
                                                                           │
                                                                           ▼
┌────────────────────────┐    RGAGameRequestConcludeEvent     ┌─────────────────────────┐
│        rga-core        │ <───────────────────────────────── │     InfectedManhunt     │
└────────────────────────┘                                    └─────────────────────────┘
```

1. **`MinigameStartEvent`:** Received when `rga-core` finishes world provisioning. `InfectedManhunt` initializes `InfectedSession`, designates the initial Infected hunter, assigns Speedrunners, and sends action titles.
2. **`RGAGameRequestConcludeEvent` / `MinigameConcludeEvent`:** Fired when win conditions are met (Ender Dragon slain or all runners infected). `InfectedManhunt` calculates scores, triggers cleanup, and returns session control to `rga-core`.

---

## 🧪 Solo QA Developer Mode (`initialPlayerCount == 1`)

To facilitate solo testing during development and map construction, `InfectedManhunt` features **Solo QA Developer Mode**:

- **Activation Condition:** When a minigame session is initiated with exactly 1 player (`initialPlayerCount == 1`).
- **Behavioral Impact:**
  - Win condition checks (`checkWinConditions`) freeze, preventing the session from immediately concluding due to 0 remaining speedrunners/infected.
  - Allows developers to manually test compass tracking behavior, nether/end portal tracking updates, map reset routines, spawn pedestal mechanics, and administrative commands without triggering session conclusion.
  - Full game conclusion can still be manually triggered via administrative commands or dragon death.

---

## 📊 Scoreboard & Lifecycle Management

Local sidebar scoreboards follow PaperMC 26.2 standards:

- **Margin Number Suppression:**
  ```java
  objective.numberFormat(NumberFormat.blank());
  ```
- **Post-Teleport Assignment:** Scoreboards are assigned to players (`player.setScoreboard(board)`) strictly after world teleportation and chunk loading are complete to prevent main-thread hangs.
- **Teardown Cleanup:** On match conclusion (`MinigameConcludeEvent`), scoreboards are unassigned and players are safely restored to the main server scoreboard:
  ```java
  player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
  objective.unregister();
  ```

---

## 🔑 Commands & Permission Nodes

### Administrative Commands
| Command | Permission | Description |
|:---|:---|:---|
| `/infected reload` | `infectedmanhunt.admin` | Reloads `config.yml` and `arena.yml` settings. |
| `/infected status` | `infectedmanhunt.admin` | Displays current active sessions and participant roles. |
| `/infected end <world>` | `infectedmanhunt.admin` | Forcefully concludes the session in the specified world. |

### Permission Nodes
- `infectedmanhunt.admin`: Grants access to administrative configuration reload and session force-end commands. (Default: `op`)
- `infectedmanhunt.user`: Basic permission granted to all participating minigame players. (Default: `true`)

---

## ⚙️ Configuration Schemas

### `config.yml`
Configures global start behavior, tracking parameters, respawn modes, and team display styling.
- `start.teleport_to_spawn`: Teleport players on start (`true`/`false`).
- `start.announce_titles`: Show screen titles on start (`true`/`false`).
- `tracking.update_ticks`: Compass update tick frequency (Default: `20`).
- `tracking.notify_last_known`: Alert hunters when tracking last portal entrance (`true`/`false`).
- `teams.speedrunner_color` / `teams.infected_color`: Color formats for team chat and display.

### `arena.yml`
Defines vector coordinates for team spawns, pedestals, time limits, and fall thresholds.
- `arena.fall_threshold_y`: Y-coordinate threshold for void fall protection (Default: `-64.0`).
- `arena.spawns`: Pedestal and team spawn vectors (`x`, `y`, `z`, `yaw`, `pitch`, `world`).

---

## 🛠 Building & Deployment

### Environment Prerequisites
- **JDK 25**
- **PaperMC 26.2**
- **RonlabGameAssistant (`rga-core`) 1.13.0-SNAPSHOT+**

### Compilation
```bash
mvn clean package
```
The resulting artifact `target/infectedmanhunt-1.0.0-SNAPSHOT.jar` should be deployed to the Paper server's `plugins/` directory alongside `RonlabGameAssistant.jar`.
