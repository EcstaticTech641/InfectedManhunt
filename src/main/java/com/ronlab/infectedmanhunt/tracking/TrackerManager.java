package com.ronlab.infectedmanhunt.tracking;

import com.ronlab.infectedmanhunt.Role;
import com.ronlab.infectedmanhunt.game.InfectedManager;
import com.ronlab.infectedmanhunt.game.InfectedSession;
import com.ronlab.rga.api.RGASessionControl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Optimized tracker task that updates hunter compasses pointing to target Speedrunners,
 * filtering out any players flagged as spectators via {@link RGASessionControl}.
 */
public class TrackerManager {

    private final Plugin plugin;
    private final InfectedManager infectedManager;
    private final RGASessionControl sessionControl;
    private BukkitTask trackingTask;

    public TrackerManager(Plugin plugin, InfectedManager infectedManager, RGASessionControl sessionControl) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.infectedManager = Objects.requireNonNull(infectedManager, "infectedManager cannot be null");
        this.sessionControl = sessionControl;
    }

    public void start() {
        stop();
        trackingTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllCompasses, 20L, 20L);
    }

    public void stop() {
        if (trackingTask != null) {
            trackingTask.cancel();
            trackingTask = null;
        }
    }

    private void updateAllCompasses() {
        for (InfectedSession session : infectedManager.getActiveSessions().values()) {
            World world = Bukkit.getWorld(session.getWorldName());
            if (world == null) continue;

            List<UUID> hunterUuids = session.getInfectedUuids();
            List<UUID> activeRunnerUuids = getActiveSpeedrunners(session);

            for (UUID hunterId : hunterUuids) {
                Player hunter = Bukkit.getPlayer(hunterId);
                if (hunter == null || !hunter.isOnline()) continue;

                // Compass Loop Optimization: Skip spectators
                if (sessionControl != null && sessionControl.isSpectator(hunter)) {
                    stripCompass(hunter);
                    continue;
                }

                giveCompassIfMissing(hunter);
                updateHunterCompass(hunter, session, activeRunnerUuids);
            }

            // Strip compasses from active runners
            for (UUID runnerId : session.getSpeedrunnerUuids()) {
                Player runner = Bukkit.getPlayer(runnerId);
                if (runner != null && runner.isOnline()) {
                    stripCompass(runner);
                }
            }
        }
    }

    public void cycleTarget(Player hunter) {
        if (hunter == null || !hunter.isOnline()) return;
        if (sessionControl != null && sessionControl.isSpectator(hunter)) return;

        InfectedSession session = infectedManager.getSessionForPlayer(hunter);
        if (session == null || !session.isInfected(hunter.getUniqueId())) return;

        List<UUID> activeRunners = getActiveSpeedrunners(session);
        if (activeRunners.isEmpty()) {
            hunter.sendMessage(Component.text("[Infected] No active speedrunners left to track.", NamedTextColor.YELLOW));
            return;
        }

        UUID currentTarget = session.getHunterTarget(hunter.getUniqueId());
        int idx = currentTarget == null ? -1 : activeRunners.indexOf(currentTarget);
        int nextIdx = (idx + 1) % activeRunners.size();
        UUID nextTarget = activeRunners.get(nextIdx);

        session.setHunterTarget(hunter.getUniqueId(), nextTarget);

        Player targetPlayer = Bukkit.getPlayer(nextTarget);
        String name = targetPlayer != null ? targetPlayer.getName() : nextTarget.toString().substring(0, 8);
        hunter.sendMessage(Component.text("[Infected] Now tracking: ", NamedTextColor.YELLOW)
                .append(Component.text(name, NamedTextColor.WHITE)));

        updateHunterCompass(hunter, session, activeRunners);
    }

    private List<UUID> getActiveSpeedrunners(InfectedSession session) {
        List<UUID> activeRunners = new ArrayList<>();
        for (UUID runnerId : session.getSpeedrunnerUuids()) {
            Player p = Bukkit.getPlayer(runnerId);
            if (p != null && p.isOnline()) {
                // Filter target Speedrunners through RGASessionControl.isSpectator(player)
                if (sessionControl != null && sessionControl.isSpectator(p)) {
                    continue;
                }
                activeRunners.add(runnerId);
            }
        }
        return List.copyOf(activeRunners);
    }

    private void updateHunterCompass(Player hunter, InfectedSession session, List<UUID> activeRunners) {
        if (activeRunners.isEmpty()) return;

        UUID targetId = session.getHunterTarget(hunter.getUniqueId());
        if (targetId == null || !activeRunners.contains(targetId)) {
            targetId = findNearestSpeedrunner(hunter, activeRunners);
            if (targetId != null) {
                session.setHunterTarget(hunter.getUniqueId(), targetId);
            }
        }

        if (targetId == null) return;

        Player target = Bukkit.getPlayer(targetId);
        Location loc = resolveTargetLocation(hunter, session, targetId, target);
        if (loc != null) {
            updateCompassMetadata(hunter, loc);
        }
    }

    private UUID findNearestSpeedrunner(Player hunter, List<UUID> activeRunners) {
        Location hLoc = hunter.getLocation();
        World hWorld = hLoc.getWorld();
        if (hWorld == null) return null;

        double nearestDistSq = Double.MAX_VALUE;
        UUID nearestId = null;
        UUID fallbackId = null;

        for (UUID id : activeRunners) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            if (fallbackId == null) fallbackId = id;

            if (hWorld.equals(p.getWorld())) {
                double distSq = p.getLocation().distanceSquared(hLoc);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearestId = id;
                }
            }
        }
        return nearestId != null ? nearestId : fallbackId;
    }

    private Location resolveTargetLocation(Player hunter, InfectedSession session, UUID targetId, Player target) {
        World hWorld = hunter.getWorld();
        if (target != null && target.isOnline()) {
            World tWorld = target.getWorld();
            if (tWorld.equals(hWorld)) {
                return target.getLocation();
            }
            if (tWorld.getEnvironment() == World.Environment.THE_END) {
                Location portal = session.getLastEndPortalLocation(targetId);
                if (portal != null) return portal;
            }
            if (tWorld.getEnvironment() == World.Environment.NETHER && hWorld.getEnvironment() == World.Environment.NORMAL) {
                Location overworld = session.getLastOverworldLocation(targetId);
                if (overworld != null) return overworld;
            }
        }

        Location overworld = session.getLastOverworldLocation(targetId);
        return overworld != null ? overworld : hWorld.getSpawnLocation();
    }

    private void updateCompassMetadata(Player hunter, Location targetLoc) {
        for (ItemStack item : hunter.getInventory().getContents()) {
            applyLodestoneMeta(item, targetLoc);
        }
        applyLodestoneMeta(hunter.getInventory().getItemInOffHand(), targetLoc);
    }

    private void applyLodestoneMeta(ItemStack item, Location targetLoc) {
        if (item == null || item.getType() != Material.COMPASS) return;
        if (item.getItemMeta() instanceof CompassMeta meta) {
            meta.setLodestone(targetLoc);
            meta.setLodestoneTracked(false);
            item.setItemMeta(meta);
        }
    }

    private void giveCompassIfMissing(Player hunter) {
        if (!hunter.getInventory().contains(Material.COMPASS) &&
                (hunter.getInventory().getItemInOffHand() == null || hunter.getInventory().getItemInOffHand().getType() != Material.COMPASS)) {
            hunter.getInventory().addItem(new ItemStack(Material.COMPASS, 1));
        }
    }

    private void stripCompass(Player player) {
        if (player == null) return;
        if (player.getInventory().getItemInOffHand() != null && player.getInventory().getItemInOffHand().getType() == Material.COMPASS) {
            player.getInventory().setItemInOffHand(null);
        }
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() == Material.COMPASS) {
                player.getInventory().setItem(i, null);
            }
        }
    }
}
