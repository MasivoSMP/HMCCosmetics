package com.hibiscusmc.hmccosmetics.command;

import com.hibiscusmc.hmccolor.HMCColorConfig;
import com.hibiscusmc.hmccolor.HMCColorContextKt;
import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.Settings;
import com.hibiscusmc.hmccosmetics.config.section.Wardrobe;
import com.hibiscusmc.hmccosmetics.config.section.WardrobeLocation;
import com.hibiscusmc.hmccosmetics.config.WardrobeSettings;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetics;
import com.hibiscusmc.hmccosmetics.database.Database;
import com.hibiscusmc.hmccosmetics.gui.Menu;
import com.hibiscusmc.hmccosmetics.gui.Menus;
import com.hibiscusmc.hmccosmetics.gui.special.DyeMenuProvider;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import com.hibiscusmc.hmccosmetics.util.HMCCServerUtils;
import me.lojosho.hibiscuscommons.hooks.Hooks;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CosmeticCommand implements CommandExecutor {

    // cosmetics apply cosmetics playerName
    //             0      1        2

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player playerSender && !Bukkit.isOwnedByCurrentRegion(playerSender)) {
            HMCCosmeticsPlugin.getInstance().getScheduler()
                .runAtEntity(playerSender, () -> onCommand(sender, command, label, args));
            return true;
        }

        boolean silent = false;
        boolean console = false;

        if (!(sender instanceof Player)) {
            console = true;
        }

        if (args.length == 0) {
            if (console) {
                return true;
            }
            if (!sender.hasPermission("hmccosmetics.cmd.default")) {
                MessagesUtil.sendMessage(sender, "no-permission");
                return true;
            }

            CosmeticUser user = CosmeticUsers.getUser(((Player) sender).getUniqueId());
            Menu menu = Menus.getDefaultMenu();

            if (user == null) {
                MessagesUtil.sendMessage(sender, "invalid-player");
                return true;
            }

            if (menu == null) {
                MessagesUtil.sendMessage(sender, "invalid-menu");
                return true;
            }

            menu.openMenu(user);
            return true;
        }
        Player player = sender instanceof Player ? (Player) sender : null;

        String firstArgs = args[0].toLowerCase();

        if (sender.hasPermission("HMCCosmetics.cmd.silent") || sender.isOp()) {
            for (String singleArg : args) {
                if (singleArg.equalsIgnoreCase("-s")) {
                    silent = true;
                    break;
                }
            }
        }

        switch (firstArgs) {
            case ("reload") -> {
                if (!sender.hasPermission("HMCCosmetics.cmd.reload") && !sender.isOp()) {
                    if (!silent) MessagesUtil.sendMessage(sender, "no-permission");
                    return true;
                }
                HMCCosmeticsPlugin.setup();
                if (!silent) MessagesUtil.sendMessage(sender, "reloaded");
                return true;
            }
            case ("apply") -> {
                if (!sender.hasPermission("hmccosmetics.cmd.apply")) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-permission"));
                    return true;
                }
                Cosmetic cosmetic;
                Color color = null;

                if (sender instanceof Player) player = ((Player) sender).getPlayer();
                if (sender.hasPermission("hmccosmetics.cmd.apply.other")) {
                    if (args.length >= 3) player = Bukkit.getPlayer(args[2]);
                }

                if (sender.hasPermission("hmccosmetics.cmd.apply.color")) {
                    if (args.length >= 4) {
                        // TODO: Add sub-color support somehow... (and make this neater)
                        String textColor = args[3];
                        if (!textColor.contains("#") && Hooks.isActiveHook("HMCColor")) {
                            HMCColorConfig.Colors colors = HMCColorContextKt.getHmcColor().getConfig().getColors().get(textColor);
                            if (colors != null) {
                                color = colors.getBaseColor().getColor();
                            }
                        } else {
                            color = HMCCServerUtils.hex2Rgb(textColor);
                        }
                    }
                }

                if (args.length == 1) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "not-enough-args"));
                    return true;
                }

                cosmetic = Cosmetics.getCosmetic(args[1]);

                if (cosmetic == null) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-cosmetic"));
                    return true;
                }

                if (player == null) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                    return true;
                }

                Player target = player;
                Cosmetic selectedCosmetic = cosmetic;
                Color selectedColor = color;
                boolean isSilent = silent;
                boolean isConsole = console;
                runOnPlayer(target, () -> {
                    CosmeticUser user = CosmeticUsers.getUser(target);
                    if (user == null) {
                        if (!isSilent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                        return;
                    }

                    if (!isConsole && !user.hasCosmeticPermission(selectedCosmetic)) {
                        if (!isSilent) MessagesUtil.sendMessage(target, "no-cosmetic-permission");
                        return;
                    }

                    if (!isConsole && !user.canUseCosmetic(selectedCosmetic)) {
                        if (!isSilent) MessagesUtil.sendMessage(target, "no-cosmetic-purchase");
                        return;
                    }

                    TagResolver placeholders =
                            TagResolver.resolver(Placeholder.parsed("cosmetic", selectedCosmetic.getId()),
                                    TagResolver.resolver(Placeholder.parsed("player", target.getName())),
                                    TagResolver.resolver(Placeholder.parsed("cosmeticslot", selectedCosmetic.getSlot().toString())));

                    if (!isSilent) MessagesUtil.sendMessage(target, "equip-cosmetic", placeholders);

                    user.addCosmetic(selectedCosmetic, selectedColor);
                    user.updateCosmetic(selectedCosmetic.getSlot());
                });
                return true;
            }
            case ("unapply") -> {
                if (!sender.hasPermission("hmccosmetics.cmd.unapply")) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-permission"));
                    return true;
                }
                if (args.length == 1) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "not-enough-args"));
                    return true;
                }

                if (sender instanceof Player) player = ((Player) sender).getPlayer();
                if (sender.hasPermission("hmccosmetics.cmd.unapply.other")) {
                    if (args.length >= 3) player = Bukkit.getPlayer(args[2]);
                }

                if (player == null) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                    return true;
                }

                Player target = player;
                boolean isSilent = silent;
                runOnPlayer(target, () -> {
                    CosmeticUser user = CosmeticUsers.getUser(target);
                    if (user == null) {
                        if (!isSilent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                        return;
                    }

                    Set<CosmeticSlot> cosmeticSlots;

                    if (args[1].equalsIgnoreCase("all")) {
                        cosmeticSlots = user.getSlotsWithCosmetics();
                    } else {
                        String rawSlot = args[1].toUpperCase();
                        if (!CosmeticSlot.contains(rawSlot)) {
                            if (!isSilent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-slot"));
                            return;
                        }
                        cosmeticSlots = Set.of(CosmeticSlot.valueOf(rawSlot));
                    }

                    for (CosmeticSlot cosmeticSlot : cosmeticSlots) {
                        if (user.getCosmetic(cosmeticSlot) == null) {
                            if (!isSilent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-cosmetic-slot"));
                            continue;
                        }

                        TagResolver placeholders =
                                TagResolver.resolver(Placeholder.parsed("cosmetic", user.getCosmetic(cosmeticSlot).getId()),
                                        TagResolver.resolver(Placeholder.parsed("player", target.getName())),
                                        TagResolver.resolver(Placeholder.parsed("cosmeticslot", cosmeticSlot.toString())));

                        if (!isSilent) MessagesUtil.sendMessage(target, "unequip-cosmetic", placeholders);

                        user.removeCosmeticSlot(cosmeticSlot);
                        user.updateCosmetic(cosmeticSlot);
                    }
                });
                return true;
            }
            case ("wardrobes") -> {
                if (sender instanceof Player) player = ((Player) sender).getPlayer();

                if (args.length == 1) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "not-enough-args"));
                    return true;
                }

                if (sender.hasPermission("hmccosmetics.cmd.wardrobe.other")) {
                    if (args.length >= 3) player = Bukkit.getPlayer(args[2]);
                }

                if (!sender.hasPermission("hmccosmetics.cmd.wardrobe")) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-permission"));
                    return true;
                }

                if (player == null) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                    return true;
                }

                if (!WardrobeSettings.getWardrobeNames().contains(args[1])) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-wardrobes"));
                    return true;
                }
                Wardrobe wardrobe = WardrobeSettings.getWardrobe(args[1]);

                Player target = player;
                boolean isSilent = silent;
                runOnPlayer(target, () -> {
                    CosmeticUser user = CosmeticUsers.getUser(target);
                    if (user == null) {
                        if (!isSilent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                        return;
                    }
                    if (user.isInWardrobe()) {
                        user.leaveWardrobe(false);
                    } else {
                        user.enterWardrobe(wardrobe, false);
                    }
                });
                return true;
            }
            // cosmetic menu exampleMenu playerName
            case ("menu") -> {
                if (!sender.hasPermission("hmccosmetics.cmd.menu")) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-permission"));
                    return true;
                }
                Menu menu;
                if (args.length == 1) {
                    menu = Menus.getDefaultMenu();
                } else {
                    menu = Menus.getMenu(args[1]);
                }

                if (sender instanceof Player) player = ((Player) sender).getPlayer();
                if (sender.hasPermission("hmccosmetics.cmd.menu.other")) {
                    if (args.length >= 3) player = Bukkit.getPlayer(args[2]);
                }

                if (player == null) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                    return true;
                }

                if (menu == null) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-menu"));
                    return true;
                }

                Player target = player;
                boolean isSilent = silent;
                runOnPlayer(target, () -> {
                    CosmeticUser user = CosmeticUsers.getUser(target);
                    if (user == null) {
                        if (!isSilent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                        return;
                    }
                    menu.openMenu(user);
                });
                return true;
            }
            case ("dataclear") -> {
                if (args.length == 1) return true;
                OfflinePlayer selectedPlayer = Bukkit.getOfflinePlayer(args[1]);
                if (!sender.hasPermission("hmccosmetics.cmd.dataclear") && !sender.isOp()) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-permission"));
                    return true;
                }
                Database.clearData(selectedPlayer.getUniqueId());
                runOnSender(sender, () -> sender.sendMessage("Cleared data for " + selectedPlayer.getName()));
                return true;
            }
            case ("dye") -> {
                if (player == null) return true;
                CosmeticUser user = CosmeticUsers.getUser(player);
                if (user == null) return true;
                if (!sender.hasPermission("hmccosmetics.cmd.dye") && !sender.isOp()) {
                    if (!silent) MessagesUtil.sendMessage(sender, "no-permission");
                    return true;
                }

                if (args.length == 1) {
                    if (!silent) MessagesUtil.sendMessage(player, "not-enough-args");
                    return true;
                }

                final String rawSlot = args[1];
                if (!CosmeticSlot.contains(rawSlot)) {
                    if (!silent) MessagesUtil.sendMessage(player, "invalid-slot");
                    return true;
                }
                final CosmeticSlot slot = CosmeticSlot.valueOf(rawSlot); // This is checked above. While IDEs may say the slot might be null, it will not be.
                final Cosmetic cosmetic = user.getCosmetic(slot);
                if (cosmetic == null) {
                    if (!silent) MessagesUtil.sendMessage(player, "invalid-slot");
                    return true;
                }

                if (args.length >= 3) {
                    if (args[2].isEmpty()) {
                        if (!silent) MessagesUtil.sendMessage(player, "invalid-color");
                        return true;
                    }
                    Color color = HMCCServerUtils.hex2Rgb(args[2]);
                    if (color == null) {
                        if (!silent) MessagesUtil.sendMessage(player, "invalid-color");
                        return true;
                    }
                    user.addCosmetic(cosmetic, color); // #FFFFFF
                } else {
                    if (DyeMenuProvider.hasMenuProvider()) {
                        DyeMenuProvider.openMenu(player, user, cosmetic);
                    } else {
                        if (!silent) MessagesUtil.sendMessage(player, "invalid-color");
                    }
                }
            }
            case ("setwardrobesetting") -> {
                if (!sender.hasPermission("hmccosmetics.cmd.setwardrobesetting")) {
                    if (!silent) MessagesUtil.sendMessage(sender, "no-permission");
                    return true;
                }

                if (player == null) return true;

                if (args.length < 3) {
                    if (!silent) MessagesUtil.sendMessage(player, "not-enough-args");
                    return true;
                }
                Wardrobe wardrobe = WardrobeSettings.getWardrobe(args[1]);
                if (wardrobe == null) {
                    //wardrobe = new Wardrobe(args[1], new WardrobeLocation(null, null, null), null, -1, null);
                    //WardrobeSettings.addWardrobe(wardrobe);
                    MessagesUtil.sendMessage(player, "no-wardrobes");
                    return true;
                }

                if (args[2].equalsIgnoreCase("npclocation")) {
                    WardrobeSettings.setNPCLocation(wardrobe, player.getLocation());
                    if (!silent) MessagesUtil.sendMessage(player, "set-wardrobe-location");
                    return true;
                }

                if (args[2].equalsIgnoreCase("viewerlocation")) {
                    WardrobeSettings.setViewerLocation(wardrobe, player.getEyeLocation());
                    if (!silent) MessagesUtil.sendMessage(player, "set-wardrobe-viewing");
                    return true;
                }

                if (args[2].equalsIgnoreCase("leavelocation")) {
                    WardrobeSettings.setLeaveLocation(wardrobe, player.getLocation());
                    if (!silent) MessagesUtil.sendMessage(player, "set-wardrobe-leaving");
                    return true;
                }

                if (args.length >= 4) {
                    if (args[2].equalsIgnoreCase("permission")) {
                        WardrobeSettings.setWardrobePermission(wardrobe, args[3]);
                        if (!silent) MessagesUtil.sendMessage(player, "set-wardrobe-permission");
                        return true;
                    }
                    if (args[2].equalsIgnoreCase("distance")) {
                        WardrobeSettings.setWardrobeDistance(wardrobe, Integer.parseInt(args[3]));
                        if (!silent) MessagesUtil.sendMessage(player, "set-wardrobe-distance");
                        return true;
                    }
                    if (args[2].equalsIgnoreCase("defaultmenu")) {
                        WardrobeSettings.setWardrobeDefaultMenu(wardrobe, args[3]);
                        if (!silent) MessagesUtil.sendMessage(player, "set-wardrobe-menu");
                        return true;
                    }
                }
            }
            case ("dump") -> {
                if (player == null) return true;
                CosmeticUser user = CosmeticUsers.getUser(player);
                if (user == null) return true;
                if (!sender.hasPermission("HMCCosmetic.cmd.dump") && !sender.isOp()) {
                    if (!silent) MessagesUtil.sendMessage(sender, "no-permission");
                    return true;
                }
                player.sendMessage("Passengers -> " + player.getPassengers());
                if (user.hasCosmeticInSlot(CosmeticSlot.BACKPACK)) {
                    player.sendMessage("Backpack Location -> " + user.getUserBackpackManager().getEntityManager().getLocation());
                }
                player.sendMessage("Cosmetic Passengers -> " + user.getUserBackpackManager().getAreaEffectEntityId());
                player.sendMessage("Cosmetics -> " + user.getCosmetics());
                player.sendMessage("EntityId -> " + player.getEntityId());
                return true;
            }
            case ("hide") -> {
                if (sender instanceof Player) player = ((Player) sender).getPlayer();
                if (sender.hasPermission("hmccosmetics.cmd.hide.other")) {
                    if (args.length >= 2) player = Bukkit.getPlayer(args[1]);
                }

                if (!sender.hasPermission("hmccosmetics.cmd.hide")) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-permission"));
                    return true;
                }

                if (player == null) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                    return true;
                }

                if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "hide-cosmetic"));
                Player target = player;
                boolean isSilent = silent;
                runOnPlayer(target, () -> {
                    CosmeticUser user = CosmeticUsers.getUser(target);
                    if (user == null) {
                        if (!isSilent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                        return;
                    }
                    user.hideCosmetics(CosmeticUser.HiddenReason.COMMAND);
                });
                return true;
            }
            case ("show") -> {
                if (sender instanceof Player) player = ((Player) sender).getPlayer();
                if (sender.hasPermission("hmccosmetics.cmd.show.other")) {
                    if (args.length >= 2) player = Bukkit.getPlayer(args[1]);
                }

                if (!sender.hasPermission("hmccosmetics.cmd.show")) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-permission"));
                    return true;
                }

                if (player == null) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                    return true;
                }

                if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "show-cosmetic"));
                Player target = player;
                boolean isSilent = silent;
                runOnPlayer(target, () -> {
                    CosmeticUser user = CosmeticUsers.getUser(target);
                    if (user == null) {
                        if (!isSilent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                        return;
                    }
                    user.showCosmetics(CosmeticUser.HiddenReason.COMMAND);
                });
                return true;
            }
            case ("debug") -> {
                if (!sender.hasPermission("hmccosmetics.cmd.debug")) {
                    if (!silent) MessagesUtil.sendMessage(sender, "no-permission");
                    return true;
                }

                if (Settings.isDebugMode()) {
                    Settings.setDebugMode(false);
                    if (!silent) MessagesUtil.sendMessage(sender, "debug-disabled");
                } else {
                    Settings.setDebugMode(true);
                    if (!silent) MessagesUtil.sendMessage(sender, "debug-enabled");
                }
            }
            case "disableall" -> {
                if (!sender.hasPermission("hmccosmetics.cmd.disableall")) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-permission"));
                    return true;
                }
                if (args.length == 1) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "not-enough-args"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("true")) {
                    Settings.setAllPlayersHidden(true);
                    for (CosmeticUser user : CosmeticUsers.values()) {
                        Player target = user.getPlayer();
                        if (target == null) continue;
                        runOnPlayer(target, () -> user.hideCosmetics(CosmeticUser.HiddenReason.DISABLED));
                    }
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "disabled-all"));
                } else if (args[1].equalsIgnoreCase("false")) {
                    Settings.setAllPlayersHidden(false);
                    for (CosmeticUser user : CosmeticUsers.values()) {
                        Player target = user.getPlayer();
                        if (target == null) continue;
                        runOnPlayer(target, () -> user.showCosmetics(CosmeticUser.HiddenReason.DISABLED));
                    }
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "enabled-all"));
                } else {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-args"));
                }
                return true;
            }

            case "hiddenreasons" -> {
                if (!sender.hasPermission("hmccosmetics.cmd.hiddenreasons")) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-permission"));
                    return true;
                }
                if (args.length >= 2) {
                    player = Bukkit.getPlayer(args[1]);
                }
                if (player == null) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                    return true;
                }
                Player target = player;
                boolean isSilent = silent;
                runOnPlayer(target, () -> {
                    CosmeticUser user = CosmeticUsers.getUser(target);
                    if (user == null) {
                        if (!isSilent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                        return;
                    }
                    runOnSender(sender, () -> sender.sendMessage(user.getHiddenReasons().toString()));
                });
                return true;
            }

            case "clearhiddenreasons" -> {
                if (!sender.hasPermission("hmccosmetics.cmd.clearhiddenreasons")) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "no-permission"));
                    return true;
                }
                if (args.length >= 2) {
                    player = Bukkit.getPlayer(args[1]);
                }
                if (player == null) {
                    if (!silent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                    return true;
                }
                Player target = player;
                boolean isSilent = silent;
                runOnPlayer(target, () -> {
                    CosmeticUser user = CosmeticUsers.getUser(target);
                    if (user == null) {
                        if (!isSilent) runOnSender(sender, () -> MessagesUtil.sendMessage(sender, "invalid-player"));
                        return;
                    }
                    user.clearHiddenReasons();
                });
                return true;
            }
        }
        return true;
    }

    private static void runOnPlayer(@NotNull Player player, @NotNull Runnable action) {
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            action.run();
            return;
        }
        HMCCosmeticsPlugin.getInstance().getScheduler().runAtEntity(player, action);
    }

    private static void runOnSender(@NotNull CommandSender sender, @NotNull Runnable action) {
        if (sender instanceof Player player) {
            runOnPlayer(player, action);
            return;
        }
        action.run();
    }
}
