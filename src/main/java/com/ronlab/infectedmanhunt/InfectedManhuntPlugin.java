package com.ronlab.infectedmanhunt;

import com.ronlab.infectedmanhunt.game.InfectedManager;
import com.ronlab.infectedmanhunt.listeners.InfectedListener;
import com.ronlab.infectedmanhunt.tracking.TrackerManager;
import com.ronlab.rga.api.RGASessionControl;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class InfectedManhuntPlugin extends JavaPlugin {

    private static InfectedManhuntPlugin instance;

    private InfectedManager infectedManager;
    private TrackerManager trackerManager;
    private RGASessionControl sessionControl;

    @Override
    public void onEnable() {
        instance = this;

        // Retrieve JIT spectator API contract from RGA Core
        Plugin rgaPlugin = getServer().getPluginManager().getPlugin("RonlabGameAssistant");
        if (rgaPlugin instanceof RGASessionControl sc) {
            this.sessionControl = sc;
            getLogger().info("Successfully bound to RGASessionControl from RonlabGameAssistant.");
        } else {
            getLogger().warning("RonlabGameAssistant plugin not found or does not implement RGASessionControl.");
        }

        this.infectedManager = new InfectedManager(getLogger());
        this.trackerManager = new TrackerManager(this, infectedManager, sessionControl);
        this.trackerManager.start();

        InfectedListener listener = new InfectedListener(infectedManager, trackerManager, sessionControl);
        getServer().getPluginManager().registerEvents(listener, this);

        getLogger().info("InfectedManhunt Companion Plugin enabled (Paper 26.2 / Java 25).");
    }

    @Override
    public void onDisable() {
        if (trackerManager != null) {
            trackerManager.stop();
        }
        getLogger().info("InfectedManhunt Companion Plugin disabled.");
    }

    public static InfectedManhuntPlugin getInstance() {
        return instance;
    }

    public InfectedManager getInfectedManager() {
        return infectedManager;
    }

    public TrackerManager getTrackerManager() {
        return trackerManager;
    }

    public RGASessionControl getSessionControl() {
        return sessionControl;
    }
}
