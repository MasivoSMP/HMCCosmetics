package com.hibiscusmc.hmccosmetics.gui;

import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticHolder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MenuSession {

    private final Menu menu;
    private final CosmeticHolder cosmeticHolder;
    private UUID externalSessionId;
    private int page;

    public MenuSession(@NotNull Menu menu, @NotNull CosmeticHolder cosmeticHolder) {
        this.menu = menu;
        this.cosmeticHolder = cosmeticHolder;
    }

    @NotNull
    public Menu getMenu() {
        return menu;
    }

    @NotNull
    public CosmeticHolder getCosmeticHolder() {
        return cosmeticHolder;
    }

    public UUID getExternalSessionId() {
        return externalSessionId;
    }

    public void setExternalSessionId(UUID externalSessionId) {
        this.externalSessionId = externalSessionId;
    }

    public int getPage() {
        return page;
    }

    public int getTotalPages() {
        return menu.getTotalPages();
    }

    public int getTotalPages(@NotNull Player viewer) {
        return menu.getTotalPages(viewer, cosmeticHolder);
    }

    public boolean nextPage() {
        return setPage(page + 1);
    }

    public boolean nextPage(@NotNull Player viewer) {
        return setPage(page + 1, getTotalPages(viewer));
    }

    public boolean previousPage() {
        return setPage(page - 1);
    }

    public boolean previousPage(@NotNull Player viewer) {
        return setPage(page - 1, getTotalPages(viewer));
    }

    public boolean setPage(int page) {
        return setPage(page, getTotalPages());
    }

    public boolean setPage(int page, int totalPages) {
        int maxPage = Math.max(0, totalPages - 1);
        int clampedPage = Math.max(0, Math.min(page, maxPage));
        boolean changed = this.page != clampedPage;
        this.page = clampedPage;
        return changed;
    }

    public void refresh(@NotNull Player viewer) {
        menu.refresh(viewer, this);
    }
}
