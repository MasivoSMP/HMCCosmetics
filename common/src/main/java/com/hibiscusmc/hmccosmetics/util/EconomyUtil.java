package com.hibiscusmc.hmccosmetics.util;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public final class EconomyUtil {

    private static Economy economy;

    private EconomyUtil() {
    }

    public static void setup() {
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
    }

    public static boolean isAvailable() {
        return economy != null;
    }

    public static boolean has(@NotNull Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public static @Nullable EconomyResponse withdraw(@NotNull Player player, double amount) {
        if (economy == null) return null;
        return economy.withdrawPlayer(player, amount);
    }

    public static @NotNull String formatPrice(double amount) {
        return BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString();
    }
}
