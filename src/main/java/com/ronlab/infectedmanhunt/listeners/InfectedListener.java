package com.ronlab.infectedmanhunt.listeners;

import com.ronlab.infectedmanhunt.Role;
import com.ronlab.infectedmanhunt.game.InfectedManager;
import com.ronlab.infectedmanhunt.game.InfectedSession;
import com.ronlab.infectedmanhunt.tracking.TrackerManager;
import com.ronlab.rga.api.event.MinigameStartEvent;
import com.ronlab.rga.api.RGASessionControl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lifecycle and gameplay listener for Infected Manhunt companion plugin.
 */
public class InfectedListener implements Listener {

    private final InfectedManager infectedManager;
    private final TrackerManager trackerManager;
    private final RGASessionControl sessionControl;

    public InfectedListener(InfectedManager infectedManager, TrackerManager trackerManager, RGASessionControl sessionControl) {
        this.infectedManager = Objects.requireNonNull(infectedManager, "infectedManager cannot be null");
        this.trackerManager = Objects.requireNonNull(trackerManager, "trackerManager cannot be null");
        this.sessionControl = sessionControl;
    }

    // ── Directive 3: Lifecycle Listener ────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMinigameStart(MinigameStartEvent event) {
        String id = event.getMinigameId();
        if (!"infectedmanhunt".equalsIgnoreCase(id) && !"infected_manhunt".equalsIgnoreCase(id)) {
            return;
        }

        List<UUID> players = event.getPlayerUuids();
        if (players == null || players.isEmpty()) {
            return;
        }

        InfectedSession session = infectedManager.startSession(id, event.getMinigameName(), event.getWorldName(), players);

        // Announce start
        List<UUID> infectedList = session.getInfectedUuids();
        for (UUID hunterId : infectedList) {
            Player hunter = Bukkit.getPlayer(hunterId);
            if (hunter != null && hunter.isOnline()) {
                hunter.showTitle(Title.title(
                        Component.text("YOU ARE INFECTED!", NamedTextColor.RED),
                        Component.text("Hunt the speedrunners", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
                ));
            }
        }

        for (UUID runnerId : session.getSpeedrunnerUuids()) {
            Player runner = Bukkit.getPlayer(runnerId);
            if (runner != null && runner.isOnline()) {
                runner.showTitle(Title.title(
                        Component.text("SPEEDRUNNERS", NamedTextColor.GREEN),
                        Component.text("Beat the dragon before you are infected", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
                ));
            }
        }
    }

    // ── Directive 3: Tag / Infection Interception ──────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvPTag(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        InfectedSession session = infectedManager.getSessionForPlayer(victim);
        if (session == null) return;

        // Skip if spectator
        if (sessionControl != null && (sessionControl.isSpectator(damager) || sessionControl.isSpectator(victim))) {
            return;
        }

        if (session.isInfected(damager.getUniqueId()) && session.isSpeedrunner(victim.getUniqueId())) {
            // Infect Speedrunner upon PvP tag
            session.setRole(victim.getUniqueId(), Role.INFECTED);

            victim.showTitle(Title.title(
                    Component.text("INFECTED!", NamedTextColor.RED),
                    Component.text("You were tagged by " + damager.getName(), NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));

            damager.sendMessage(Component.text("[Infected] You infected " + victim.getName() + "!", NamedTextColor.RED));

            // Check if all speedrunners are infected
            checkWinConditions(session);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        InfectedSession session = infectedManager.getSessionForPlayer(victim);
        if (session == null) return;

        // Remove tracking compass drops
        event.getDrops().removeIf(item -> item != null && item.getType() == Material.COMPASS);

        if (session.isSpeedrunner(victim.getUniqueId())) {
            // Convert to Infected or promote to spectator using RGASessionControl
            session.setRole(victim.getUniqueId(), Role.INFECTED);

            if (sessionControl != null) {
                // If victim should be shifted out of active play, call RGASessionControl.setSpectator
                // sessionControl.setSpectator(victim, true);
            }

            victim.sendMessage(Component.text("[Infected] You died and joined the Infected Hunters!", NamedTextColor.RED));
            checkWinConditions(session);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDragonDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon)) return;

        InfectedSession session = infectedManager.getSessionByWorld(dragon.getWorld().getName());
        if (session == null) return;

        // Speedrunners slain dragon -> Speedrunners win!
        infectedManager.concludeSession(session.getWorldName(), "Speedrunners slain the Ender Dragon!", true);
    }

    private void checkWinConditions(InfectedSession session) {
        List<UUID> activeRunners = session.getSpeedrunnerUuids().stream()
                .filter(uuid -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null || !p.isOnline()) return false;
                    return sessionControl == null || !sessionControl.isSpectator(p);
                }).toList();

        if (activeRunners.isEmpty()) {
            // All speedrunners infected -> Infected win!
            infectedManager.concludeSession(session.getWorldName(), "All Speedrunners have been infected!", false);
        }
    }

    // ── Directive 3: Compass Interactions & Movement ─────────────

    @EventHandler
    public void onCompassUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.COMPASS) return;

        Player player = event.getPlayer();
        InfectedSession session = infectedManager.getSessionForPlayer(player);
        if (session == null || !session.isInfected(player.getUniqueId())) return;

        if (sessionControl != null && sessionControl.isSpectator(player)) return;

        event.setCancelled(true);
        trackerManager.cycleTarget(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftCompass(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InfectedSession session = infectedManager.getSessionForPlayer(player);
        if (session == null || !session.isSpeedrunner(player.getUniqueId())) return;

        if (event.getRecipe() != null && event.getRecipe().getResult().getType() == Material.COMPASS) {
            event.setCancelled(true);
            player.sendMessage(Component.text("[Infected] Speedrunners cannot craft compasses.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onDropCompass(PlayerDropItemEvent event) {
        InfectedSession session = infectedManager.getSessionForPlayer(event.getPlayer());
        if (session == null) return;

        ItemStack stack = event.getItemDrop().getItemStack();
        if (stack.getType() == Material.COMPASS) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickupCompass(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        InfectedSession session = infectedManager.getSessionForPlayer(player);
        if (session == null) return;

        if (event.getItem().getItemStack().getType() == Material.COMPASS && !session.isInfected(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        InfectedSession session = infectedManager.getSessionForPlayer(player);
        if (session == null || !session.isSpeedrunner(player.getUniqueId())) return;

        Location from = event.getFrom();
        if (from.getWorld() != null && from.getWorld().getEnvironment() == World.Environment.NORMAL) {
            session.updateLastOverworldLocation(player.getUniqueId(), from);
        }
        if (event.getTo() != null && event.getTo().getWorld() != null && event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            session.updateLastEndPortalLocation(player.getUniqueId(), from);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        InfectedSession session = infectedManager.getSessionForPlayer(player);
        if (session == null || !session.isSpeedrunner(player.getUniqueId())) return;

        if (player.getWorld().getEnvironment() == World.Environment.NORMAL) {
            session.updateLastOverworldLocation(player.getUniqueId(), player.getLocation());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player player = event.getPlayer();
        InfectedSession session = infectedManager.getSessionForPlayer(player);
        if (session == null || !session.isSpeedrunner(player.getUniqueId())) return;

        Location to = event.getTo();
        if (to.getWorld() != null && to.getWorld().getEnvironment() == World.Environment.NORMAL) {
            session.updateLastOverworldLocation(player.getUniqueId(), to);
        }
    }
}
