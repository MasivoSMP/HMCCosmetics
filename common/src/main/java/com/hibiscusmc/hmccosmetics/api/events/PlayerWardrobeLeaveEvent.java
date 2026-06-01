package com.hibiscusmc.hmccosmetics.api.events;

import com.hibiscusmc.hmccosmetics.config.section.Wardrobe;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.manager.UserWardrobeManager;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player leaves their {@link Wardrobe}.
 */
public class PlayerWardrobeLeaveEvent extends PlayerCosmeticEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final UserWardrobeManager wardrobeManager;
    private boolean cancel = false;

    public PlayerWardrobeLeaveEvent(@NotNull CosmeticUser who, @NotNull UserWardrobeManager wardrobe) {
        super(who);
        this.wardrobeManager = wardrobe;
    }

    /**
     * The instance of the wardrobe a player is attempting to exit from.
     * @return
     */
    public @NotNull UserWardrobeManager getWardrobeManager() {
        return wardrobeManager;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
