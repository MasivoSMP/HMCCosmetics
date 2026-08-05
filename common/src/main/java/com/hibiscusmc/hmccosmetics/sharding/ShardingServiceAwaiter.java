package com.hibiscusmc.hmccosmetics.sharding;

import gg.masivo.sharding.api.bus.CrossShardBus;
import gg.masivo.sharding.paper.api.MasivoShardingPaperService;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Defers shard-aware startup until MasivoSharding has published its Paper services. */
public final class ShardingServiceAwaiter implements Listener, AutoCloseable {
    @FunctionalInterface
    public interface Initializer {
        boolean initialize(MasivoShardingPaperService sharding, CrossShardBus bus) throws Exception;
    }

    private final JavaPlugin plugin;
    private final Initializer initializer;
    private final AtomicBoolean attemptScheduled = new AtomicBoolean();
    private final AtomicBoolean initializing = new AtomicBoolean();
    private volatile boolean initialized;
    private volatile boolean closed;

    public ShardingServiceAwaiter(JavaPlugin plugin, Initializer initializer) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.initializer = Objects.requireNonNull(initializer, "initializer");
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("Waiting for MasivoSharding Paper services.");
        scheduleAttempt();
    }

    private void scheduleAttempt() {
        if (closed || initialized || !attemptScheduled.compareAndSet(false, true)) return;
        try {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
                attemptScheduled.set(false);
                attemptInitialization();
            });
        } catch (RuntimeException failure) {
            attemptScheduled.set(false);
            fail("Could not schedule the MasivoSharding readiness check.", failure);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServiceRegister(ServiceRegisterEvent event) {
        scheduleAttempt();
    }

    private void attemptInitialization() {
        if (closed || initialized || !plugin.isEnabled()) return;
        MasivoShardingPaperService sharding = plugin.getServer().getServicesManager()
                .load(MasivoShardingPaperService.class);
        CrossShardBus bus = plugin.getServer().getServicesManager().load(CrossShardBus.class);
        if (sharding == null || bus == null || !initializing.compareAndSet(false, true)) return;
        try {
            if (initializer.initialize(sharding, bus) && plugin.isEnabled()) {
                initialized = true;
                HandlerList.unregisterAll(this);
                plugin.getLogger().info("MasivoSharding Paper services connected.");
            }
        } catch (Exception failure) {
            fail("Shard-aware initialization failed.", failure);
        } finally {
            initializing.set(false);
        }
    }

    private void fail(String message, Throwable failure) {
        plugin.getLogger().log(Level.SEVERE, message, failure);
        close();
        if (plugin.isEnabled()) plugin.getServer().getPluginManager().disablePlugin(plugin);
    }

    @Override
    public void close() {
        closed = true;
        HandlerList.unregisterAll(this);
    }
}
