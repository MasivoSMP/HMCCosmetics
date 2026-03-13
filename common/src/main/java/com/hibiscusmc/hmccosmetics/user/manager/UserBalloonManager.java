package com.hibiscusmc.hmccosmetics.user.manager;

import com.hibiscusmc.hmccosmetics.cosmetic.types.CosmeticBalloonType;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import com.hibiscusmc.hmccosmetics.util.packets.HMCCPacketManager;
import lombok.Getter;
import me.lojosho.hibiscuscommons.nms.NMSHandlers;
import me.lojosho.hibiscuscommons.util.ServerUtils;
import me.lojosho.hibiscuscommons.util.packets.PacketManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class UserBalloonManager {

    private static final Vector LEAD_OFFSET = new Vector(0, 0.8, 0);
    private static final int VIEWER_REFRESH_INTERVAL_TICKS = 4;

    private final CosmeticUser user;
    @Getter
    private BalloonType balloonType;
    private CosmeticBalloonType cosmeticBalloonType;
    @Getter
    private final UserBalloonPufferfish pufferfish;
    private final UserEntity displayEntity;
    private final int displayEntityId;
    private final UUID displayUuid;
    private Location location;
    private Vector velocity = new Vector();
    private double pitchRotationRadians;
    private double angularPitchVelocity;
    private double lastSentPitchRotationRadians = Double.NaN;
    private int viewerRefreshTick = VIEWER_REFRESH_INTERVAL_TICKS;
    private boolean viewerRefreshRequired = true;

    public UserBalloonManager(CosmeticUser user, @NotNull Location location) {
        this.user = user;
        this.pufferfish = new UserBalloonPufferfish(
            user.getUniqueId(),
            NMSHandlers.getHandler().getUtilHandler().getNextEntityId(),
            UUID.randomUUID()
        );
        this.displayEntityId = ServerUtils.getNextEntityId();
        this.displayUuid = UUID.randomUUID();
        this.displayEntity = new UserEntity(user.getUniqueId());
        this.displayEntity.setIds(List.of(displayEntityId));
        this.location = location.clone();
    }

    public void spawnModel(@NotNull CosmeticBalloonType cosmeticBalloonType, @Nullable Color color) {
        // ModelEngine removed: only item-based balloons render now.
        balloonType = cosmeticBalloonType.getItem() != null ? BalloonType.ITEM : BalloonType.NONE;
        this.cosmeticBalloonType = cosmeticBalloonType;
        MessagesUtil.sendDebugMessages("balloontype is " + balloonType);
    }

    public void remove() {
        if (balloonType == BalloonType.ITEM) {
            HMCCPacketManager.sendEntityDestroyPacket(displayEntityId, displayEntity.getViewers());
            displayEntity.getViewers().clear();
        }

        pufferfish.destroyPufferfish();
        cosmeticBalloonType = null;
        velocity.zero();
        pitchRotationRadians = 0.0D;
        angularPitchVelocity = 0.0D;
        lastSentPitchRotationRadians = Double.NaN;
        viewerRefreshRequired = true;
        viewerRefreshTick = VIEWER_REFRESH_INTERVAL_TICKS;
        MessagesUtil.sendDebugMessages("Balloon Entity Removed");
    }

    public void addPlayerToModel(final CosmeticUser user, final CosmeticBalloonType cosmeticBalloonType) {
        addPlayerToModel(user, cosmeticBalloonType, null);
    }

    public void addPlayerToModel(final CosmeticUser user, final CosmeticBalloonType cosmeticBalloonType, @Nullable Color color) {
        if (balloonType != BalloonType.ITEM) return;
        Player viewer = user.getPlayer();
        if (viewer == null) return;

        Location current = getLocation();
        if (current == null) return;

        spawnDisplay(current, List.of(viewer));
        if (cosmeticBalloonType.isShowLead() && !user.isHidden()) {
            addViewers(pufferfish, List.of(viewer));
            pufferfish.spawnPufferfish(current, List.of(viewer));
            HMCCPacketManager.sendLeashPacket(getPufferfishBalloonId(), viewer.getEntityId(), List.of(viewer));
        }
    }

    public void removePlayerFromModel(final Player viewer) {
        if (viewer == null) return;
        HMCCPacketManager.sendEntityDestroyPacket(displayEntityId, List.of(viewer));
        displayEntity.getViewers().remove(viewer);
        HMCCPacketManager.sendEntityDestroyPacket(getPufferfishBalloonId(), List.of(viewer));
        pufferfish.getViewers().remove(viewer);
    }

    public void spawnDisplay(@NotNull Location location, @NotNull List<Player> viewers) {
        if (balloonType != BalloonType.ITEM || viewers.isEmpty() || cosmeticBalloonType == null) return;
        addViewers(displayEntity, viewers);
        HMCCPacketManager.spawnInvisibleArmorstand(displayEntityId, location, displayUuid, viewers);
        PacketManager.equipmentSlotUpdate(
            displayEntityId,
            EquipmentSlot.HEAD,
            user.getUserCosmeticItem(cosmeticBalloonType),
            viewers
        );
    }

    public void teleportDisplay(@NotNull Location location) {
        displayEntity.teleport(location);
    }

    public List<Player> refreshDisplayViewers(@NotNull Location location) {
        return displayEntity.refreshViewers(location);
    }

    public List<Player> refreshLeadViewers(@NotNull Location location) {
        return pufferfish.refreshViewers(location);
    }

    public boolean isDisplayViewer(@NotNull Player viewer) {
        return displayEntity.getViewers().contains(viewer);
    }

    public boolean isLeadViewer(@NotNull Player viewer) {
        return pufferfish.getViewers().contains(viewer);
    }

    public int getPufferfishBalloonId() {
        return pufferfish.getPufferFishEntityId();
    }

    public UUID getPufferfishBalloonUniqueId() {
        return pufferfish.getUuid();
    }

    public int getDisplayEntityId() {
        return displayEntityId;
    }

    public UUID getDisplayUuid() {
        return displayUuid;
    }

    public @Nullable Location getLocation() {
        return location == null ? null : location.clone();
    }

    public void setLocation(@NotNull Location location) {
        this.location = location.clone();
    }

    public Vector getVelocity() {
        return velocity.clone();
    }

    public void setVelocity(@NotNull Vector vector) {
        this.velocity = vector.clone();
    }

    public double getPitchRotationRadians() {
        return pitchRotationRadians;
    }

    public void setPitchRotationRadians(double pitchRotationRadians) {
        this.pitchRotationRadians = pitchRotationRadians;
    }

    public double getAngularPitchVelocity() {
        return angularPitchVelocity;
    }

    public void setAngularPitchVelocity(double angularPitchVelocity) {
        this.angularPitchVelocity = angularPitchVelocity;
    }

    public void resetPhysicsState(@NotNull Location location) {
        this.location = location.clone();
        this.velocity.zero();
        this.pitchRotationRadians = 0.0D;
        this.angularPitchVelocity = 0.0D;
        this.lastSentPitchRotationRadians = Double.NaN;
        forceViewerRefresh();
    }

    public void forceViewerRefresh() {
        viewerRefreshRequired = true;
        viewerRefreshTick = VIEWER_REFRESH_INTERVAL_TICKS;
    }

    public boolean shouldRefreshPhysicsViewers() {
        if (viewerRefreshRequired || displayEntity.getViewers().isEmpty() || (hasLead() && pufferfish.getViewers().isEmpty())) {
            viewerRefreshRequired = false;
            viewerRefreshTick = 0;
            return true;
        }

        viewerRefreshTick++;
        if (viewerRefreshTick >= VIEWER_REFRESH_INTERVAL_TICKS) {
            viewerRefreshTick = 0;
            return true;
        }
        return false;
    }

    public void syncPackets(@NotNull Location displayLocation, int holderEntityId, boolean hidden, boolean refreshViewers) {
        setLocation(displayLocation);

        List<Player> newViewers = List.of();
        if (refreshViewers) {
            newViewers = refreshDisplayViewers(displayLocation);
            if (!newViewers.isEmpty()) {
                spawnDisplay(displayLocation, newViewers);
            }
        }
        if (!displayEntity.getViewers().isEmpty()) {
            teleportDisplay(displayLocation);
            if (shouldSendHeadPose(newViewers)) {
                HMCCPacketManager.sendArmorStandHeadPose(displayEntityId, pitchRotationRadians, displayEntity.getViewers());
                lastSentPitchRotationRadians = pitchRotationRadians;
            }
        }

        if (!hasLead()) {
            return;
        }

        Location leadLocation = getLeadLocation(displayLocation);
        if (hidden) {
            if (!pufferfish.getViewers().isEmpty()) {
                pufferfish.hidePufferfish();
            }
            return;
        }

        if (refreshViewers) {
            List<Player> newLeadViewers = refreshLeadViewers(leadLocation);
            if (!newLeadViewers.isEmpty()) {
                pufferfish.spawnPufferfish(leadLocation, newLeadViewers);
            }
        }

        if (!pufferfish.getViewers().isEmpty()) {
            pufferfish.teleport(leadLocation);
            HMCCPacketManager.sendLeashPacket(getPufferfishBalloonId(), holderEntityId, pufferfish.getViewers());
        }
    }

    private boolean shouldSendHeadPose(List<Player> newViewers) {
        if (!newViewers.isEmpty()) {
            return true;
        }
        if (Double.isNaN(lastSentPitchRotationRadians)) {
            return true;
        }
        return Math.abs(lastSentPitchRotationRadians - pitchRotationRadians) >= 0.01D;
    }

    public void sendRemoveLeashPacket(List<Player> viewer) {
        HMCCPacketManager.sendLeashPacket(getPufferfishBalloonId(), -1, viewer);
    }

    public void sendRemoveLeashPacket() {
        Location current = getLocation();
        if (current == null) return;
        HMCCPacketManager.sendLeashPacket(getPufferfishBalloonId(), -1, current);
    }

    public void sendLeashPacket(int entityId) {
        if (cosmeticBalloonType == null || !cosmeticBalloonType.isShowLead()) return;
        Location current = getLocation();
        if (current == null) return;
        HMCCPacketManager.sendLeashPacket(getPufferfishBalloonId(), entityId, current);
    }

    public @NotNull Location getLeadLocation(@NotNull Location baseLocation) {
        return baseLocation.clone().add(LEAD_OFFSET);
    }

    public boolean hasLead() {
        return balloonType == BalloonType.ITEM && cosmeticBalloonType != null && cosmeticBalloonType.isShowLead();
    }

    public enum BalloonType {
        ITEM,
        NONE
    }

    private void addViewers(UserEntity entity, List<Player> viewers) {
        for (Player viewer : viewers) {
            if (!entity.getViewers().contains(viewer)) {
                entity.getViewers().add(viewer);
            }
        }
    }
}
