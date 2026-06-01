package com.hibiscusmc.hmccosmetics.api.events;

import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when cosmetics are shown from a player. This event is only called when a cosmetic user has a hidden reason and that reason is being used to show cosmetics.
 */
public class PlayerCosmeticShowEvent extends PlayerCosmeticEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private boolean cancel = false;
    private final CosmeticUser.HiddenReason reason;

    public PlayerCosmeticShowEvent(@NotNull CosmeticUser who, @NotNull CosmeticUser.HiddenReason reason) {
        super(who);
        this.reason = reason;
    }


    /**
     * Gets the {@link CosmeticUser.HiddenReason} as to why cosmetics are being shown for the player.
     *
     * @return the reason why cosmetics are being shown for the player
     */
    public @NotNull CosmeticUser.HiddenReason getReason() {
        return reason;
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