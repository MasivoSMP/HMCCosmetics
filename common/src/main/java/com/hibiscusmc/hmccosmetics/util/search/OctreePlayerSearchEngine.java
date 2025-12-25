package com.hibiscusmc.hmccosmetics.util.search;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.util.Octree;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OctreePlayerSearchEngine extends PlayerSearchEngine {

    private final Map<UUID, Octree<Player>> worldOctrees = new ConcurrentHashMap<>();
    private final Map<UUID, Octree.Point3D> playerPositions = new ConcurrentHashMap<>();
    private final Map<UUID, Object> worldLocks = new ConcurrentHashMap<>();

    private final int WORLD_HALF_SIZE;

    public OctreePlayerSearchEngine(@NotNull HMCCosmeticsPlugin instance) {
        super(instance);
        WORLD_HALF_SIZE = instance.getServer().getMaxWorldSize();
    }

    private Octree<Player> getOrCreateOctree(World world) {
        return worldOctrees.computeIfAbsent(world.getUID(), $ -> {
            Octree.BoundingBox worldBoundary = new Octree.BoundingBox(
                    new Octree.Point3D(0, 160, 0), WORLD_HALF_SIZE
            );
            return new Octree<>(worldBoundary);
        });
    }

    private Object getWorldLock(World world) {
        return worldLocks.computeIfAbsent(world.getUID(), $ -> new Object());
    }

    private Octree.Point3D toPoint3D(Location location) {
        return new Octree.Point3D(location.getX(), location.getY(), location.getZ());
    }

    public boolean addPlayer(Player player) {
        Octree<Player> octree = getOrCreateOctree(player.getWorld());
        Octree.Point3D point = toPoint3D(player.getLocation());
        synchronized (getWorldLock(player.getWorld())) {
            if (octree.insert(point, player)) {
                playerPositions.put(player.getUniqueId(), point);
                return true;
            }
        }
        return false;
    }

    public boolean removePlayer(Player player) {
        Octree<Player> octree = worldOctrees.get(player.getWorld().getUID());
        if (octree == null) return false;

        synchronized (getWorldLock(player.getWorld())) {
            Octree.Point3D point = playerPositions.remove(player.getUniqueId());
            if (point != null) return octree.remove(point, player);
        }

        return false;
    }

    public void updatePlayerPosition(Player player) {
        removePlayer(player);
        addPlayer(player);
    }

    @Override
    public List<Player> getPlayersInRange(Location location, double range) {
        Octree<Player> octree = worldOctrees.get(location.getWorld().getUID());
        if (octree == null) return Collections.emptyList();

        Octree.Point3D point = toPoint3D(location);
        Octree.BoundingBox searchArea = new Octree.BoundingBox(point, range);

        synchronized (getWorldLock(location.getWorld())) {
            return octree.queryRange(searchArea)
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();
        }
    }


    public void clear() {
        worldOctrees.clear();
        playerPositions.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.hasChangedBlock()) updatePlayerPosition(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        updatePlayerPosition(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        addPlayer(event.getPlayer());
    }

}
