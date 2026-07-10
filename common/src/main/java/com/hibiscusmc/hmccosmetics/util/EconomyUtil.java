package com.hibiscusmc.hmccosmetics.util;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import java.lang.reflect.Method;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public final class EconomyUtil {

    private static Economy economy;
    private static Vault2Access vault2;
    private static Object counterpartyRegistry;

    private EconomyUtil() {
    }

    public static void setup() {
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
        vault2 = Vault2Access.resolve();
        registerCounterparty();
    }

    public static boolean isAvailable() {
        return economy != null;
    }

    public static boolean has(@NotNull Player player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public static boolean withdraw(@NotNull Player player, double amount) {
        if (vault2 != null) {
            try {
                vault2.ensureAccount(player);
                Object response = vault2.withdraw.invoke(vault2.provider, HMCCosmeticsPlugin.getInstance().getName(), player.getUniqueId(), BigDecimal.valueOf(amount));
                return (boolean) response.getClass().getMethod("transactionSuccess").invoke(response);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                HMCCosmeticsPlugin.getInstance().getLogger().warning("VaultUnlocked transaction failed; falling back to Vault v1: " + exception.getMessage());
                vault2 = null;
            }
        }
        return economy != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public static @NotNull String formatPrice(double amount) {
        return BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString();
    }

    public static void close() {
        Plugin plugin = HMCCosmeticsPlugin.getInstance();
        if (counterpartyRegistry == null || plugin == null) return;
        try {
            counterpartyRegistry.getClass().getMethod("unregister", Plugin.class).invoke(counterpartyRegistry, plugin);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("Could not unregister the MasivoEconomy counterparty: " + exception.getMessage());
        } finally {
            counterpartyRegistry = null;
        }
    }

    private static void registerCounterparty() {
        Plugin plugin = HMCCosmeticsPlugin.getInstance();
        Plugin economyPlugin = Bukkit.getPluginManager().getPlugin("MasivoEconomy");
        if (plugin == null || economyPlugin == null || !economyPlugin.isEnabled()) {
            close();
            return;
        }
        try {
            Class<?> type = Class.forName("gg.masivo.economy.api.CounterpartyRegistry", false, economyPlugin.getClass().getClassLoader());
            Object available = Bukkit.getServicesManager().load(type);
            if (counterpartyRegistry != null && counterpartyRegistry != available) close();
            counterpartyRegistry = available;
            if (available != null) {
                available.getClass().getMethod("register", Plugin.class, String.class, java.util.Collection.class)
                        .invoke(available, plugin,
                                plugin.getConfig().getString("economy.counterparty.display-name", "Cosméticos"),
                                plugin.getConfig().getStringList("economy.counterparty.legacy-source-aliases"));
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("Could not register the MasivoEconomy counterparty: " + exception.getMessage());
            close();
        }
    }

    private static final class Vault2Access {
        private final Object provider;
        private final Method withdraw;
        private final Method createAccount;

        private Vault2Access(Object provider, Method withdraw, Method createAccount) {
            this.provider = provider;
            this.withdraw = withdraw;
            this.createAccount = createAccount;
        }

        private static Vault2Access resolve() {
            try {
                Class<?> type = Class.forName("net.milkbowl.vault2.economy.Economy");
                RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(type);
                if (registration == null || registration.getProvider() == null) return null;
                Method createAccount;
                try {
                    createAccount = type.getMethod("createAccount", UUID.class, String.class);
                } catch (NoSuchMethodException ignored) {
                    createAccount = null;
                }
                return new Vault2Access(registration.getProvider(), type.getMethod("withdraw", String.class, UUID.class, BigDecimal.class), createAccount);
            } catch (ClassNotFoundException | NoSuchMethodException exception) {
                return null;
            }
        }

        private void ensureAccount(OfflinePlayer player) throws ReflectiveOperationException {
            if (createAccount == null) return;
            String name = player.getName();
            createAccount.invoke(provider, player.getUniqueId(), name == null || name.isBlank() ? player.getUniqueId().toString() : name);
        }
    }
}
