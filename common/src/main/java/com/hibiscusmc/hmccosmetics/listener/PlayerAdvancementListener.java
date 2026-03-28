package com.hibiscusmc.hmccosmetics.listener;

import com.hibiscusmc.hmccosmetics.api.events.PlayerLoadEvent;
import com.hibiscusmc.hmccosmetics.api.events.PlayerMenuOpenEvent;
import com.hibiscusmc.hmccosmetics.database.Database;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class PlayerAdvancementListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLoad(PlayerLoadEvent event) {
        CosmeticUser user = event.getUser();
        user.invalidateAdvancementRequirementCache();
        user.syncAdvancementUnlocks(true);

        if (user.enforceEquippedCosmeticRequirements()) {
            user.updateCosmetic();
            Database.save(user);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMenuOpen(PlayerMenuOpenEvent event) {
        CosmeticUser user = event.getUser();
        user.invalidateAdvancementRequirementCache();
        user.syncAdvancementUnlocks(false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        CosmeticUser user = CosmeticUsers.getUser(event.getPlayer());
        if (user == null) return;
        user.handleCompletedAdvancement(event.getAdvancement());
    }
}
