package com.hibiscusmc.hmccosmetics.util;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import gg.masivo.economy.api.observatory.EconomyActivityRequest;
import gg.masivo.economy.api.observatory.EconomyActivityRequests;
import gg.masivo.economy.api.observatory.EconomyActivityResult;
import gg.masivo.economy.api.observatory.EconomyEndpoint;
import gg.masivo.economy.api.observatory.EconomyObservatoryClient;
import gg.masivo.economy.api.observatory.EconomySystemDescriptor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class EconomyUtil {
    private static Economy economy;
    private static EconomyObservatoryClient observatory;

    private EconomyUtil() { }

    public static void setup() {
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
        if (observatory != null) observatory.close();
        HMCCosmeticsPlugin plugin = HMCCosmeticsPlugin.getInstance();
        observatory = EconomyObservatoryClient.connect(plugin, new EconomySystemDescriptor("hmc_cosmetics", "Cosmeticos",
                plugin.getConfig().getStringList("economy.counterparty.legacy-source-aliases"),
                Map.of("cosmetic.purchase", "Compra de cosmetico")),
                plugin.getConfig().getString("economy.counterparty.display-name", "Cosmeticos"),
                plugin.getConfig().getStringList("economy.counterparty.legacy-source-aliases"));
    }

    public static boolean isAvailable() { return observatory != null || economy != null; }

    public static boolean has(@NotNull Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public static boolean withdraw(@NotNull Player player, double amount, @NotNull String operationId,
                                   @NotNull String cosmeticId, @NotNull String cosmeticType, @NotNull String display) {
        BigDecimal money = EconomyActivityRequests.money(BigDecimal.valueOf(amount));
        EconomyActivityRequest request = EconomyActivityRequests.sink("hmc_cosmetics", "cosmetic.purchase",
                operationId + ".payment", player.getUniqueId(), player.getName(), money,
                EconomyEndpoint.system("cosmetic_catalog", "Catalogo de cosmeticos"), Map.of(
                "cosmetic_id", cosmeticId, "cosmetic_type", cosmeticType, "display", bounded(display)));
        EconomyActivityResult result = observatory.execute(request);
        if (result.success()) return true;
        if (!result.fallbackAllowed()) {
            HMCCosmeticsPlugin.getInstance().getLogger().warning("Observatory rejected cosmetic payment '"
                    + operationId + "': " + result.message());
            return false;
        }
        return economy != null && economy.withdrawPlayer(player, money.doubleValue()).transactionSuccess();
    }

    public static @NotNull String formatPrice(double amount) {
        return BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString();
    }

    public static void close() {
        if (observatory != null) observatory.close();
        observatory = null;
    }

    private static String bounded(String value) {
        return value.substring(0, Math.min(value.length(), 256));
    }
}
