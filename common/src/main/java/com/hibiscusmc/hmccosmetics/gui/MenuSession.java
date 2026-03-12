package com.hibiscusmc.hmccosmetics.gui;

import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticHolder;
import dev.triumphteam.gui.guis.Gui;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MenuSession {

    private final Menu menu;
    private final Gui gui;
    private final CosmeticHolder cosmeticHolder;
    private int page;

    public MenuSession(@NotNull Menu menu, @NotNull Gui gui, @NotNull CosmeticHolder cosmeticHolder) {
        this.menu = menu;
        this.gui = gui;
        this.cosmeticHolder = cosmeticHolder;
    }

    @NotNull
    public Menu getMenu() {
        return menu;
    }

    @NotNull
    public Gui getGui() {
        return gui;
    }

    @NotNull
    public CosmeticHolder getCosmeticHolder() {
        return cosmeticHolder;
    }

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return menu.getTotalPages();
    }

    public boolean nextPage() {
        return setPage(page + 1);
    }

    public boolean previousPage() {
        return setPage(page - 1);
    }

    public boolean setPage(int page) {
        int maxPage = Math.max(0, getTotalPages() - 1);
        int clampedPage = Math.max(0, Math.min(page, maxPage));
        boolean changed = this.page != clampedPage;
        this.page = clampedPage;
        return changed;
    }

    public void refresh(@NotNull Player viewer) {
        menu.refresh(viewer, this);
    }
}
