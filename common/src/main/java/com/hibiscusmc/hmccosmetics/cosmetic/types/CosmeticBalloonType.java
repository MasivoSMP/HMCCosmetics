package com.hibiscusmc.hmccosmetics.cosmetic.types;

import com.hibiscusmc.hmccosmetics.config.Settings;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.behavior.CosmeticMovementBehavior;
import com.hibiscusmc.hmccosmetics.cosmetic.behavior.CosmeticUpdateBehavior;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.manager.UserBalloonManager;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import com.hibiscusmc.hmccosmetics.util.packets.HMCCPacketManager;
import lombok.Getter;
import me.lojosho.shaded.configurate.ConfigurationNode;
import me.lojosho.shaded.configurate.serialize.SerializationException;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MainHand;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CosmeticBalloonType extends Cosmetic implements CosmeticUpdateBehavior, CosmeticMovementBehavior {

    private static final double BALLOON_RADIUS = 1.0;
    private static final double BALLOON_MOMENT_OF_INERTIA = 0.4D * BALLOON_RADIUS * BALLOON_RADIUS;

    @Getter
    private final String modelName;
    @Getter
    private List<String> dyeableParts;
    @Getter
    private final boolean showLead;
    @Getter
    private Vector balloonOffset;

    public CosmeticBalloonType(String id, ConfigurationNode config) {
        super(id, config);

        String modelId = config.node("model").getString();
        showLead = config.node("show-lead").getBoolean(true);

        ConfigurationNode balloonOffsetNode = config.node("balloon-offset");
        if (balloonOffsetNode.virtual())
            balloonOffset = Settings.getBalloonOffset();
        else
            balloonOffset = Settings.loadVector(balloonOffsetNode);

        try {
            if (!config.node("dyeable-parts").virtual()) {
                dyeableParts = config.node("dyeable-parts").getList(String.class);
            }
        } catch (SerializationException e) {
            // Seriously?
            throw new RuntimeException(e);
        }
        if (modelId != null) modelId = modelId.toLowerCase(); // ME only accepts lowercase
        this.modelName = modelId;
    }

    @Override
    public void dispatchUpdate(@NotNull CosmeticUser user) {
        if (Settings.isBalloonPhysics()) return;

        Entity entity = Bukkit.getEntity(user.getUniqueId());
        UserBalloonManager userBalloonManager = user.getBalloonManager();

        if (entity == null || userBalloonManager == null) return;
        if (user.isInWardrobe()) return;
        if (userBalloonManager.getBalloonType() == UserBalloonManager.BalloonType.NONE) return;

        Location newLocation = entity.getLocation();
        newLocation = newLocation.clone().add(getBalloonOffset());
        if (Settings.isBalloonHeadForward()) newLocation.setPitch(0);

        List<Player> newViewers = userBalloonManager.refreshDisplayViewers(newLocation);
        if (!newViewers.isEmpty()) {
            userBalloonManager.spawnDisplay(newLocation, newViewers);
        }

        if (!user.isHidden() && showLead) {
            Location leadLocation = userBalloonManager.getLeadLocation(newLocation);
            List<Player> sendTo = userBalloonManager.refreshLeadViewers(leadLocation);
            if (!sendTo.isEmpty()) {
                userBalloonManager.getPufferfish().spawnPufferfish(leadLocation, sendTo);
                HMCCPacketManager.sendLeashPacket(userBalloonManager.getPufferfishBalloonId(), entity.getEntityId(), sendTo);
            }
        }
    }

    @Override
    public void dispatchMove(@NotNull CosmeticUser user, @NotNull Location from, @NotNull Location to) {
        if (Settings.isBalloonPhysics()) return;

        updateLegacyMovement(user);
    }

    public void updatePhysics(@NotNull CosmeticUser user, float deltaTime) {
        Entity entity = Bukkit.getEntity(user.getUniqueId());
        UserBalloonManager userBalloonManager = user.getBalloonManager();

        if (entity == null || userBalloonManager == null) return;
        if (user.isInWardrobe()) return;
        if (userBalloonManager.getBalloonType() == UserBalloonManager.BalloonType.NONE) return;

        Location playerLocation = entity.getLocation();
        Location spawnLocation = getInitialBalloonLocation(playerLocation, entity instanceof Player player ? player : null);
        Location currentLocation = userBalloonManager.getLocation();
        if (currentLocation == null || currentLocation.getWorld() != playerLocation.getWorld()) {
            userBalloonManager.resetPhysicsState(spawnLocation);
            userBalloonManager.syncPackets(spawnLocation, entity.getEntityId(), user.isHidden(), true);
            return;
        }

        Vector anchor = getAnchorPoint(playerLocation, entity instanceof Player player ? player : null);
        Vector currentPosition = currentLocation.toVector();
        Vector toBalloon = currentPosition.clone().subtract(anchor);
        double distance = toBalloon.length();
        double restingLength = Math.max(0.001D, getBalloonOffset().length());

        Vector force = new Vector(0, Settings.getBalloonPhysicsBuoyancy(), 0);
        Vector tensionForce = new Vector();
        if (distance > restingLength) {
            Vector direction = toBalloon.clone().multiply(1.0D / distance);
            double stretch = distance - restingLength;
            tensionForce = direction.multiply(-Settings.getBalloonPhysicsStringStiffness() * stretch);
            force.add(tensionForce);
        }

        Vector velocity = userBalloonManager.getVelocity();
        double dampingFactor = Math.exp(-Settings.getBalloonPhysicsLinearDampingPerSecond() * deltaTime);
        velocity.multiply(dampingFactor).add(force.multiply(deltaTime));

        Vector newPosition = currentPosition.add(velocity.clone().multiply(deltaTime));
        Vector rope = newPosition.clone().subtract(anchor);
        double ropeLength = rope.length();
        if (ropeLength > restingLength) {
            newPosition = anchor.clone().add(rope.multiply(restingLength / ropeLength));
            velocity = newPosition.clone().subtract(currentPosition).multiply(1.0D / Math.max(deltaTime, 0.0001D));
        }

        Location newLocation = newPosition.toLocation(playerLocation.getWorld());
        facePlayer(newLocation, playerLocation.toVector());
        userBalloonManager.setVelocity(velocity);
        updatePitchTilt(userBalloonManager, newPosition, playerLocation.toVector(), tensionForce, deltaTime);

        boolean refreshViewers = userBalloonManager.shouldRefreshPhysicsViewers();
        userBalloonManager.syncPackets(newLocation, entity.getEntityId(), user.isHidden(), refreshViewers);

        MessagesUtil.sendDebugMessages("Balloon Physics Update for " + user.getEntity().getName());
        MessagesUtil.sendDebugMessages("Balloon anchor is " + anchor);
        MessagesUtil.sendDebugMessages("Balloon location set to " + newLocation);
        MessagesUtil.sendDebugMessages("Balloon velocity set to " + velocity);
    }

    public boolean isDyeablePart(String name) {
        // If player does not define parts, dye whole model
        if (dyeableParts == null) return true;
        if (dyeableParts.isEmpty()) return true;
        return dyeableParts.contains(name);
    }

    public @NotNull Location getInitialBalloonLocation(@NotNull Location playerLocation, Player player) {
        Location balloonLocation = playerLocation.clone().add(getBalloonOffset());
        if (Settings.isBalloonHeadForward()) {
            balloonLocation.setPitch(0);
        } else {
            balloonLocation.setPitch(playerLocation.getPitch());
        }
        return balloonLocation;
    }

    private void updateLegacyMovement(@NotNull CosmeticUser user) {
        Entity entity = Bukkit.getEntity(user.getUniqueId());
        UserBalloonManager userBalloonManager = user.getBalloonManager();

        if (entity == null || userBalloonManager == null) return;
        if (user.isInWardrobe()) return;
        if (userBalloonManager.getBalloonType() == UserBalloonManager.BalloonType.NONE) return;

        Location newLocation = getInitialBalloonLocation(entity.getLocation(), entity instanceof Player player ? player : null);
        Location currentLocation = userBalloonManager.getLocation();
        if (currentLocation == null) {
            currentLocation = newLocation;
        }

        Vector velocity = newLocation.toVector().subtract(currentLocation.toVector());
        userBalloonManager.setLocation(newLocation);
        userBalloonManager.setVelocity(velocity.multiply(1.1));

        MessagesUtil.sendDebugMessages("Balloon Cosmetic Update for " + user.getEntity().getName());
        MessagesUtil.sendDebugMessages("Ballon previous location is " + currentLocation);
        MessagesUtil.sendDebugMessages("Balloon location set to " + newLocation);
        MessagesUtil.sendDebugMessages("Balloon velocity set to " + velocity);

        List<Player> newViewers = userBalloonManager.refreshDisplayViewers(newLocation);
        if (!newViewers.isEmpty()) {
            userBalloonManager.spawnDisplay(newLocation, newViewers);
        }
        userBalloonManager.teleportDisplay(newLocation);

        if (!user.isHidden() && showLead) {
            Location leadLocation = userBalloonManager.getLeadLocation(newLocation);
            List<Player> leadViewers = userBalloonManager.refreshLeadViewers(leadLocation);
            if (!leadViewers.isEmpty()) {
                userBalloonManager.getPufferfish().spawnPufferfish(leadLocation, leadViewers);
            }
            userBalloonManager.getPufferfish().teleport(leadLocation);
            HMCCPacketManager.sendLeashPacket(userBalloonManager.getPufferfishBalloonId(), entity.getEntityId(), userBalloonManager.getPufferfish().getViewers());
        }
    }

    private @NotNull Vector getAnchorPoint(@NotNull Location playerLocation, Player player) {
        Vector localOffset = Settings.getBalloonPhysicsHandOffset().clone();
        if (player != null && player.getMainHand() == MainHand.LEFT) {
            localOffset.setX(-localOffset.getX());
        }

        double yawRadians = Math.toRadians(-playerLocation.getYaw());
        double cos = Math.cos(yawRadians);
        double sin = Math.sin(yawRadians);
        double rotatedX = localOffset.getX() * cos - localOffset.getZ() * sin;
        double rotatedZ = localOffset.getX() * sin + localOffset.getZ() * cos;

        return playerLocation.toVector().add(new Vector(rotatedX, localOffset.getY(), rotatedZ));
    }

    private void updatePitchTilt(
        @NotNull UserBalloonManager userBalloonManager,
        @NotNull Vector balloonPosition,
        @NotNull Vector playerPosition,
        @NotNull Vector tensionForce,
        float deltaTime
    ) {
        double currentPitch = userBalloonManager.getPitchRotationRadians();
        double angularVelocity = userBalloonManager.getAngularPitchVelocity();
        double maxPitchRadians = Math.toRadians(Settings.getBalloonPhysicsMaxPitchDegrees());

        Vector forward = playerPosition.clone().subtract(balloonPosition);
        forward.setY(0);
        double forwardLength = forward.length();
        double torque = 0.0D;
        if (forwardLength > 0.000001D && tensionForce.lengthSquared() > 0.000001D) {
            forward.multiply(1.0D / forwardLength);

            Vector right = new Vector(-forward.getZ(), 0.0D, forward.getX());
            Vector attachmentOffset = forward.clone()
                .multiply(Settings.getBalloonPhysicsPitchAttachmentForwardOffset())
                .add(new Vector(0.0D, -Settings.getBalloonPhysicsPitchAttachmentDownOffset(), 0.0D));
            torque = attachmentOffset.getCrossProduct(tensionForce).dot(right) * Settings.getBalloonPhysicsPitchTorqueScale();
        }

        double restoringTorque = -Settings.getBalloonPhysicsPitchRestoreStiffness() * currentPitch;
        double angularAcceleration = (torque + restoringTorque) / BALLOON_MOMENT_OF_INERTIA;
        angularVelocity += angularAcceleration * deltaTime;
        angularVelocity *= Math.exp(-Settings.getBalloonPhysicsPitchDampingPerSecond() * deltaTime);
        currentPitch += angularVelocity * deltaTime;

        if (currentPitch > maxPitchRadians) {
            currentPitch = maxPitchRadians;
            if (angularVelocity > 0.0D) angularVelocity = 0.0D;
        } else if (currentPitch < -maxPitchRadians) {
            currentPitch = -maxPitchRadians;
            if (angularVelocity < 0.0D) angularVelocity = 0.0D;
        }

        userBalloonManager.setAngularPitchVelocity(angularVelocity);
        userBalloonManager.setPitchRotationRadians(currentPitch);
    }

    private void facePlayer(@NotNull Location balloonLocation, @NotNull Vector playerPosition) {
        Vector direction = playerPosition.subtract(balloonLocation.toVector());
        double dx = direction.getX();
        double dz = direction.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90F;
        if (yaw < -180F) yaw += 360F;
        if (yaw >= 180F) yaw -= 360F;
        balloonLocation.setYaw(yaw);
        balloonLocation.setPitch(0);
    }
}
