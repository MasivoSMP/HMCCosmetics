package com.hibiscusmc.hmccosmetics.task;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.Settings;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import com.hibiscusmc.hmccosmetics.cosmetic.types.CosmeticBalloonType;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import com.hibiscusmc.hmccosmetics.user.manager.UserBalloonManager;
import me.lojosho.hibiscuscommons.scheduler.TaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public enum BalloonPhysicsTask {
    INSTANCE;

    private static final float FIRST_TICK_DELTA_SECONDS = 0.05F;
    private static final float MIN_DELTA_SECONDS = 0.01F;
    private static final float MAX_DELTA_SECONDS = 0.10F;

    private final Map<UUID, TaskHandle> tasks = new ConcurrentHashMap<>();

    public void register(CosmeticUser user) {
        unregister(user.getUniqueId());

        if (!Settings.isBalloonPhysics()) return;

        Player player = user.getPlayer();
        UserBalloonManager balloonManager = user.getBalloonManager();
        if (player == null || balloonManager == null) return;
        if (user.isHidden() || user.isInWardrobe()) return;
        if (balloonManager.getBalloonType() != UserBalloonManager.BalloonType.ITEM) return;

        PhysicsTicker ticker = new PhysicsTicker(user.getUniqueId());
        TaskHandle handle = HMCCosmeticsPlugin.getInstance().getScheduler()
            .runAtEntityAtFixedRate(player, ticker, 1, 1);
        tasks.put(user.getUniqueId(), handle);
    }

    public void unregister(CosmeticUser user) {
        unregister(user.getUniqueId());
    }

    public void unregister(UUID userId) {
        TaskHandle existing = tasks.remove(userId);
        if (existing != null) {
            existing.cancel();
        }
    }

    public void reload() {
        stop();
        if (!Settings.isBalloonPhysics()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            HMCCosmeticsPlugin.getInstance().getScheduler().runAtEntity(player, () -> {
                CosmeticUser user = CosmeticUsers.getUser(player);
                if (user == null) return;
                register(user);
            });
        }
    }

    public void stop() {
        for (TaskHandle handle : tasks.values()) {
            handle.cancel();
        }
        tasks.clear();
    }

    private final class PhysicsTicker implements Runnable {
        private final UUID userId;
        private long lastTickNanos = -1L;

        private PhysicsTicker(UUID userId) {
            this.userId = userId;
        }

        @Override
        public void run() {
            CosmeticUser user = CosmeticUsers.getUser(userId);
            if (user == null) {
                unregister(userId);
                return;
            }

            Player player = user.getPlayer();
            UserBalloonManager balloonManager = user.getBalloonManager();
            Cosmetic cosmetic = user.getCosmetic(CosmeticSlot.BALLOON);
            if (player == null || balloonManager == null || !(cosmetic instanceof CosmeticBalloonType balloonType)) {
                unregister(userId);
                return;
            }
            if (user.isHidden() || user.isInWardrobe()) {
                unregister(userId);
                return;
            }
            if (balloonManager.getBalloonType() != UserBalloonManager.BalloonType.ITEM) {
                unregister(userId);
                return;
            }

            long now = System.nanoTime();
            float deltaTime = FIRST_TICK_DELTA_SECONDS;
            if (lastTickNanos != -1L) {
                deltaTime = (now - lastTickNanos) / 1_000_000_000.0F;
                deltaTime = Math.max(MIN_DELTA_SECONDS, Math.min(MAX_DELTA_SECONDS, deltaTime));
            }
            lastTickNanos = now;

            balloonType.updatePhysics(user, deltaTime);
        }
    }
}
