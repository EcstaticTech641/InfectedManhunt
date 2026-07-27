package com.ronlab.infectedmanhunt.game;

import com.ronlab.rga.api.event.RGAGameRequestConcludeEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages all active Infected Manhunt sessions across RGA worlds.
 */
public class InfectedManager {

    private final Logger logger;
    private final ConcurrentHashMap<String, InfectedSession> activeSessions = new ConcurrentHashMap<>();

    public InfectedManager(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    public InfectedSession startSession(String minigameId, String minigameName, String worldName, List<UUID> playerUuids) {
        InfectedSession session = new InfectedSession(minigameId, minigameName, worldName, playerUuids);
        activeSessions.put(worldName, session);
        logger.info("Initialized InfectedManhunt session for world: " + worldName + " with " + playerUuids.size() + " player(s).");
        return session;
    }

    public InfectedSession getSessionByWorld(String worldName) {
        if (worldName == null) return null;
        return activeSessions.get(worldName);
    }

    public InfectedSession getSessionForPlayer(org.bukkit.entity.Player player) {
        if (player == null || player.getWorld() == null) return null;
        return getSessionByWorld(player.getWorld().getName());
    }

    public Map<String, InfectedSession> getActiveSessions() {
        return Map.copyOf(activeSessions);
    }

    public void removeSession(String worldName) {
        if (worldName != null) {
            activeSessions.remove(worldName);
        }
    }

    /**
     * Gracefully requests RGA Core to conclude the specified minigame session.
     * Constructs and dispatches {@link RGAGameRequestConcludeEvent} and invokes RGA teardown.
     */
    public void concludeSession(String worldName, String reason, boolean speedrunnersWin) {
        InfectedSession session = activeSessions.remove(worldName);
        if (session == null) return;

        Map<UUID, Number> scores = new HashMap<>();
        for (UUID id : session.getParticipantUuids()) {
            boolean isRunner = session.isSpeedrunner(id);
            scores.put(id, (isRunner && speedrunnersWin) ? 100 : 0);
        }

        Map<UUID, Number> immutableScores = Map.copyOf(scores);

        // Construct and dispatch RGAGameRequestConcludeEvent via Bukkit Event API
        RGAGameRequestConcludeEvent concludeEvent = new RGAGameRequestConcludeEvent(
                session.getMinigameId(),
                session.getMinigameName(),
                worldName,
                session.getParticipantUuids(),
                reason,
                immutableScores
        );
        Bukkit.getPluginManager().callEvent(concludeEvent);

        // Attempt direct call to RGA Core requestSessionConclude if available on main plugin
        Plugin rgaPlugin = Bukkit.getPluginManager().getPlugin("RonlabGameAssistant");
        if (rgaPlugin != null) {
            try {
                Method method = rgaPlugin.getClass().getMethod("requestSessionConclude", String.class, String.class, Map.class);
                method.invoke(rgaPlugin, worldName, reason, immutableScores);
            } catch (Exception ignored) {
                // Ignore if method not found or invocation error; event was already fired above
            }
        }

        logger.info("Requested RGA session conclusion for world: " + worldName + " (" + reason + ")");
    }
}
