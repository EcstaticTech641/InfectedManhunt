package com.ronlab.infectedmanhunt.game;

import com.ronlab.infectedmanhunt.Role;
import org.bukkit.Location;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe container representing an active Infected Manhunt minigame session.
 */
public class InfectedSession {

    private final String minigameId;
    private final String minigameName;
    private final String worldName;
    private final ConcurrentHashMap<UUID, Role> roles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> lastOverworldLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Location> lastEndPortalLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> hunterTargets = new ConcurrentHashMap<>();

    public InfectedSession(String minigameId, String minigameName, String worldName, List<UUID> playerUuids) {
        this.minigameId = Objects.requireNonNull(minigameId, "minigameId cannot be null");
        this.minigameName = Objects.requireNonNull(minigameName, "minigameName cannot be null");
        this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");

        if (playerUuids != null && !playerUuids.isEmpty()) {
            // First player is designated initial Infected hunter, remaining are Speedrunners
            boolean first = true;
            for (UUID uuid : playerUuids) {
                if (uuid != null) {
                    roles.put(uuid, first ? Role.INFECTED : Role.SPEEDRUNNER);
                    first = false;
                }
            }
        }
    }

    public String getMinigameId() {
        return minigameId;
    }

    public String getMinigameName() {
        return minigameName;
    }

    public String getWorldName() {
        return worldName;
    }

    public Role getRole(UUID playerUuid) {
        if (playerUuid == null) return null;
        return roles.get(playerUuid);
    }

    public void setRole(UUID playerUuid, Role role) {
        if (playerUuid == null || role == null) return;
        roles.put(playerUuid, role);
    }

    public boolean isSpeedrunner(UUID playerUuid) {
        return getRole(playerUuid) == Role.SPEEDRUNNER;
    }

    public boolean isInfected(UUID playerUuid) {
        return getRole(playerUuid) == Role.INFECTED;
    }

    public List<UUID> getSpeedrunnerUuids() {
        List<UUID> list = roles.entrySet().stream()
                .filter(entry -> entry.getValue() == Role.SPEEDRUNNER)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return List.copyOf(list);
    }

    public List<UUID> getInfectedUuids() {
        List<UUID> list = roles.entrySet().stream()
                .filter(entry -> entry.getValue() == Role.INFECTED)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return List.copyOf(list);
    }

    public List<UUID> getParticipantUuids() {
        return List.copyOf(roles.keySet());
    }

    public Map<UUID, Role> getRoleMap() {
        return Map.copyOf(roles);
    }

    public void updateLastOverworldLocation(UUID runnerId, Location location) {
        if (runnerId != null && location != null) {
            lastOverworldLocations.put(runnerId, location.clone());
        }
    }

    public void updateLastEndPortalLocation(UUID runnerId, Location location) {
        if (runnerId != null && location != null) {
            lastEndPortalLocations.put(runnerId, location.clone());
        }
    }

    public Location getLastOverworldLocation(UUID runnerId) {
        if (runnerId == null) return null;
        Location loc = lastOverworldLocations.get(runnerId);
        return loc != null ? loc.clone() : null;
    }

    public Location getLastEndPortalLocation(UUID runnerId) {
        if (runnerId == null) return null;
        Location loc = lastEndPortalLocations.get(runnerId);
        return loc != null ? loc.clone() : null;
    }

    public UUID getHunterTarget(UUID hunterId) {
        if (hunterId == null) return null;
        return hunterTargets.get(hunterId);
    }

    public void setHunterTarget(UUID hunterId, UUID targetId) {
        if (hunterId == null) return;
        if (targetId == null) {
            hunterTargets.remove(hunterId);
        } else {
            hunterTargets.put(hunterId, targetId);
        }
    }
}
