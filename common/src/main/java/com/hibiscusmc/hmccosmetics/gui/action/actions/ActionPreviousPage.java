package com.hibiscusmc.hmccosmetics.gui.action.actions;

import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticHolder;
import com.hibiscusmc.hmccosmetics.gui.MenuSession;
import com.hibiscusmc.hmccosmetics.gui.Menus;
import com.hibiscusmc.hmccosmetics.gui.action.Action;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import org.bukkit.entity.Player;

public class ActionPreviousPage extends Action {

    public ActionPreviousPage() {
        super("previous_page");
    }

    @Override
    public void run(Player viewer, CosmeticHolder cosmeticHolder, String raw) {
        MenuSession session = Menus.getSession(viewer.getUniqueId());
        if (session == null) return;

        if (session.previousPage(viewer)) {
            session.refresh(viewer);
        }
    }

    @Override
    public void run(CosmeticUser user, String raw) {
        final var player = user.getPlayer();
        if (player == null) return;
        run(player, user, raw);
    }
}
