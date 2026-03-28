package com.hibiscusmc.hmccosmetics.gui.type.types;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.Settings;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticHolder;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetics;
import com.hibiscusmc.hmccosmetics.cosmetic.types.CosmeticArmorType;
import com.hibiscusmc.hmccosmetics.gui.action.Actions;
import com.hibiscusmc.hmccosmetics.gui.special.DyeMenuProvider;
import com.hibiscusmc.hmccosmetics.gui.type.Type;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.util.EconomyUtil;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import me.lojosho.hibiscuscommons.HibiscusCommonsPlugin;
import me.lojosho.hibiscuscommons.config.serializer.ItemSerializer;
import me.lojosho.hibiscuscommons.hooks.Hooks;
import me.lojosho.shaded.configurate.ConfigurationNode;
import me.lojosho.shaded.configurate.serialize.SerializationException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TypeCosmetic extends Type {

    public TypeCosmetic(String id) {
        super(id);
    }

    public TypeCosmetic() {
        super("cosmetic");
    }

    @Override
    public void run(Player viewer, CosmeticHolder cosmeticHolder, ConfigurationNode config, ClickType clickType) {
        MessagesUtil.sendDebugMessages("Running Cosmetic Click Type");
        if (config.node("cosmetic").virtual()) {
            MessagesUtil.sendDebugMessages("Cosmetic Config Field Virtual");
            return;
        }
        String cosmeticName = config.node("cosmetic").getString();
        Cosmetic cosmetic = Cosmetics.getCosmetic(cosmeticName);
        if (cosmetic == null) {
            MessagesUtil.sendDebugMessages("No Cosmetic Found");
            MessagesUtil.sendMessage(viewer, "invalid-cosmetic");
            return;
        }

        CosmeticUser user = CosmeticHolder.ensureSingleCosmeticUser(viewer, cosmeticHolder);
        boolean canEquipCosmetic = cosmeticHolder.canEquipCosmetic(cosmetic);
        if (!canEquipCosmetic) {
            if (!user.hasCosmeticPermission(cosmetic)) {
                MessagesUtil.sendDebugMessages("No Cosmetic Permission");
                MessagesUtil.sendMessage(viewer, "no-cosmetic-permission");
            } else if (cosmetic.requiresAdvancement()) {
                MessagesUtil.sendDebugMessages("Missing Cosmetic Advancement");
                MessagesUtil.sendMessage(
                    viewer,
                    "no-cosmetic-advancement",
                    TagResolver.resolver(Placeholder.component("advancement", resolveAdvancementChatDisplay(cosmetic)))
                );
            }
            return;
        }

        boolean isUnEquippingCosmetic = false;
        if (cosmeticHolder.getCosmetic(cosmetic.getSlot()) == cosmetic) isUnEquippingCosmetic = true;

        String dyeClick = Settings.getCosmeticDyeClickType();
        String requiredClick;
        if (isUnEquippingCosmetic) requiredClick = Settings.getCosmeticUnEquipClickType();
        else requiredClick = Settings.getCosmeticEquipClickType();

        MessagesUtil.sendDebugMessages("Required click type: " + requiredClick);
        MessagesUtil.sendDebugMessages("Click type: " + clickType.name());

        final boolean isRequiredClick = requiredClick.equalsIgnoreCase("ANY") || requiredClick.equalsIgnoreCase(clickType.name());
        final boolean isDyeClick = dyeClick.equalsIgnoreCase("ANY") || dyeClick.equalsIgnoreCase(clickType.name());
        final boolean opensDyeMenu = cosmetic.isDyeable() && isDyeClick && DyeMenuProvider.hasMenuProvider();

        if (!isRequiredClick) isUnEquippingCosmetic = false;

        List<String> actionStrings = new ArrayList<>();
        ConfigurationNode actionConfig = config.node("actions");

        MessagesUtil.sendDebugMessages("Running Actions");

        try {
            if (!actionConfig.node("any").virtual()) actionStrings.addAll(actionConfig.node("any").getList(String.class));

            if (clickType != null) {
                if (clickType.isLeftClick()) {
                    if (!actionConfig.node("left-click").virtual()) actionStrings.addAll(actionConfig.node("left-click").getList(String.class));
                }
                if (clickType.isRightClick()) {
                    if (!actionConfig.node("right-click").virtual()) actionStrings.addAll(actionConfig.node("right-click").getList(String.class));
                }
                if (clickType.equals(ClickType.SHIFT_LEFT)) {
                    if (!actionConfig.node("shift-left-click").virtual()) actionStrings.addAll(actionConfig.node("shift-left-click").getList(String.class));
                }
                if (clickType.equals(ClickType.SHIFT_RIGHT)) {
                    if (!actionConfig.node("shift-right-click").virtual()) actionStrings.addAll(actionConfig.node("shift-right-click").getList(String.class));
                }
            }

            if (isUnEquippingCosmetic) {
                if (!actionConfig.node("on-unequip").virtual()) actionStrings.addAll(actionConfig.node("on-unequip").getList(String.class));
                MessagesUtil.sendDebugMessages("on-unequip");
                cosmeticHolder.removeCosmeticSlot(cosmetic);
                Settings.playCosmeticToggleSound(viewer, false);
            } else {
                if (!user.canUseCosmetic(cosmetic)) {
                    if ((isRequiredClick || opensDyeMenu) && cosmetic.requiresPurchase() && user.hasCosmeticPermission(cosmetic)) {
                        if (!user.purchaseCosmetic(cosmetic, getMenuCosmeticName(viewer, config, cosmetic))) return;
                    } else {
                        return;
                    }
                }

                if (!actionConfig.node("on-equip").virtual()) actionStrings.addAll(actionConfig.node("on-equip").getList(String.class));
                MessagesUtil.sendDebugMessages("on-equip");
                MessagesUtil.sendDebugMessages("Preparing for on-equip with the following checks:");
                MessagesUtil.sendDebugMessages("CosmeticDyeable? " + cosmetic.isDyeable() + " / isDyeClick? " + isDyeClick + " / isHMCColorActive? " + Hooks.isActiveHook("HMCColor"));
                // TODO: Redo this
                if (opensDyeMenu) {
                    DyeMenuProvider.openMenu(viewer, cosmeticHolder, cosmetic);
                } else if (isRequiredClick) {
                    cosmeticHolder.addCosmetic(cosmetic);
                    Settings.playCosmeticToggleSound(viewer, true);
                }
            }

            Actions.runActions(viewer, cosmeticHolder, actionStrings);

        } catch (SerializationException e) {
            e.printStackTrace();
        }
        // Fixes issue with offhand cosmetics not appearing. Yes, I know this is dumb
        Runnable run = () -> cosmeticHolder.updateCosmetic(cosmetic.getSlot());
        if (cosmetic instanceof CosmeticArmorType) {
            if (((CosmeticArmorType) cosmetic).getEquipSlot().equals(EquipmentSlot.OFF_HAND)) {
                HMCCosmeticsPlugin.getInstance().getScheduler().runAtEntityLater(viewer, run, 1);
            }
        }
        run.run();
        MessagesUtil.sendDebugMessages("Finished Type Click Run");
    }

    @Override
    public void run(CosmeticUser user, @NotNull ConfigurationNode config, ClickType clickType) {
        run(user.getPlayer(), user, config, clickType);
    }

    @Override
    public ItemStack setItem(CosmeticUser user, ConfigurationNode config, ItemStack itemStack, int slot) {
        return setItem(user.getPlayer(), user, config, itemStack, slot);
    }

    @Override
    public ItemStack setItem(@NotNull Player viewer, @NotNull CosmeticHolder cosmeticHolder, @NotNull ConfigurationNode config, @NotNull ItemStack itemStack, int slot) {
        if (config.node("cosmetic").virtual()) {
            return finalizeItem(viewer, cosmeticHolder, null, itemStack);
        }
        String cosmeticName = config.node("cosmetic").getString();
        Cosmetic cosmetic = Cosmetics.getCosmetic(cosmeticName);
        if (cosmetic == null) {
            return finalizeItem(viewer, cosmeticHolder, null, itemStack);
        }

        if (cosmeticHolder.hasCosmeticInSlot(cosmetic) && (!config.node("equipped-item").virtual() || !config.node("locked-equipped-item").virtual())) {
            MessagesUtil.sendDebugMessages("GUI Equipped Item");
            ConfigurationNode equippedItem = config.node(cosmeticHolder.canEquipCosmetic(cosmetic, true) && !config.node("equipped-item").virtual() ? "equipped-item" : "locked-equipped-item");
            try {
                if (equippedItem.node("material").virtual()) equippedItem.node("material").set(config.node("item", "material").getString());
            } catch (SerializationException e) {
                // Nothing >:)
            }
            try {
                itemStack = ItemSerializer.INSTANCE.deserialize(ItemStack.class, equippedItem);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
            return finalizeItem(viewer, cosmeticHolder, cosmetic, itemStack);
        }

        if (!cosmeticHolder.canEquipCosmetic(cosmetic, true) && !config.node("locked-item").virtual()) {
            MessagesUtil.sendDebugMessages("GUI Locked Item");
            ConfigurationNode lockedItem = config.node("locked-item");
            try {
                if (lockedItem.node("material").virtual()) lockedItem.node("material").set(config.node("item", "material").getString());
            } catch (SerializationException e) {
                // Nothing >:)
            }
            try {
                itemStack = ItemSerializer.INSTANCE.deserialize(ItemStack.class, lockedItem);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
            return finalizeItem(viewer, cosmeticHolder, cosmetic, itemStack);
        }
        return finalizeItem(viewer, cosmeticHolder, cosmetic, itemStack);
    }

    private @NotNull ItemStack finalizeItem(@NotNull Player viewer, @NotNull CosmeticHolder cosmeticHolder, Cosmetic cosmetic, @NotNull ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            MessagesUtil.sendDebugMessages("ItemStack has no ItemMeta?");
            return itemStack;
        }

        applyConfiguredCosmeticLore(itemMeta, cosmetic);

        if (cosmetic != null && cosmeticHolder instanceof CosmeticUser user && shouldAppendPurchaseLore(user, cosmetic)) {
            appendPurchaseLore(itemMeta, cosmetic.getPrice());
        }

        itemStack.setItemMeta(processItemMeta(viewer, itemMeta));
        return itemStack;
    }

    private boolean shouldAppendPurchaseLore(@NotNull CosmeticUser user, @NotNull Cosmetic cosmetic) {
        return cosmetic.requiresPurchase()
            && user.canEquipCosmetic(cosmetic, true)
            && !user.hasPurchasedCosmetic(cosmetic);
    }

    private void appendPurchaseLore(@NotNull ItemMeta itemMeta, double price) {
        List<String> purchaseLore = Settings.getPurchaseLore();
        if (purchaseLore.isEmpty()) return;

        String formattedPrice = EconomyUtil.formatPrice(price);
        if (HibiscusCommonsPlugin.isOnPaper()) {
            List<Component> lore = itemMeta.hasLore() && itemMeta.lore() != null
                ? new ArrayList<>(itemMeta.lore())
                : new ArrayList<>();
            for (String line : purchaseLore) {
                lore.add(MiniMessage.miniMessage()
                    .deserialize(line.replace("%price%", formattedPrice))
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
            itemMeta.lore(lore);
            return;
        }

        List<String> lore = itemMeta.hasLore() && itemMeta.getLore() != null
            ? new ArrayList<>(itemMeta.getLore())
            : new ArrayList<>();
        for (String line : purchaseLore) {
            lore.add(line.replace("%price%", formattedPrice));
        }
        itemMeta.setLore(lore);
    }

    private void applyConfiguredCosmeticLore(@NotNull ItemMeta itemMeta, @Nullable Cosmetic cosmetic) {
        if (cosmetic == null) return;

        List<String> templateLore = resolveConfiguredCosmeticLore(cosmetic);
        if (templateLore.isEmpty()) return;

        List<String> existingLore = getRawLore(itemMeta);
        List<String> combinedLore = new ArrayList<>(templateLore);
        combinedLore.addAll(existingLore);
        setRawLore(itemMeta, combinedLore, cosmetic);
    }

    private @NotNull List<String> resolveConfiguredCosmeticLore(@NotNull Cosmetic cosmetic) {
        ConfigurationNode cosmeticConfig = cosmetic.getConfig();

        if (cosmeticConfig != null && !cosmeticConfig.node("custom-lore").virtual()) {
            try {
                return renderCosmeticLoreLines(cosmetic, cosmeticConfig.node("custom-lore").getList(String.class), cosmeticConfig);
            } catch (Exception ignored) {
                return new ArrayList<>();
            }
        }

        if (!Settings.isCosmeticLoreEnabled()) return new ArrayList<>();
        return renderCosmeticLoreLines(cosmetic, Settings.getCosmeticLoreLines(), cosmeticConfig);
    }

    private @NotNull List<String> renderCosmeticLoreLines(@NotNull Cosmetic cosmetic,
                                                           @Nullable List<String> lines,
                                                           @Nullable ConfigurationNode cosmeticConfig) {
        if (lines == null || lines.isEmpty()) return new ArrayList<>();

        String allowedWith = getMetadataValue(cosmeticConfig, "allowed-with");
        String madeBy = getMetadataValue(cosmeticConfig, "made-by");
        Component advancementDisplay = resolveAdvancementDisplay(cosmetic);
        if (allowedWith != null) {
            allowedWith = Settings.resolveAllowedWithDisplay(allowedWith);
        }

        List<String> renderedLines = new ArrayList<>();
        for (String line : lines) {
            if (line == null) continue;
            if (line.contains("{allowed-with}") && (allowedWith == null || allowedWith.isBlank())) continue;
            if (line.contains("{made-by}") && (madeBy == null || madeBy.isBlank())) continue;
            if (line.contains("{advancement}") && advancementDisplay == null) continue;

            String renderedLine = line
                .replace("{cosmetic}", cosmetic.getId())
                .replace("{allowed-with}", allowedWith == null ? "" : allowedWith)
                .replace("{made-by}", madeBy == null ? "" : madeBy);

            if (!HibiscusCommonsPlugin.isOnPaper()) {
                String advancementReplacement = advancementDisplay == null
                    ? ""
                    : PlainTextComponentSerializer.plainText().serialize(advancementDisplay);
                renderedLine = renderedLine.replace("{advancement}", advancementReplacement);
            }

            renderedLines.add(renderedLine);
        }
        return renderedLines;
    }

    private @Nullable Component resolveAdvancementDisplay(@NotNull Cosmetic cosmetic) {
        if (!cosmetic.requiresAdvancement()) return null;

        Advancement advancement = cosmetic.resolveAdvancement();
        if (advancement == null) return null;

        io.papermc.paper.advancement.AdvancementDisplay display = advancement.getDisplay();
        if (display == null) return null;
        return display.title().colorIfAbsent(display.frame().color());
    }

    private @NotNull Component resolveAdvancementChatDisplay(@NotNull Cosmetic cosmetic) {
        if (!cosmetic.requiresAdvancement()) return Component.text("Unknown");

        Advancement advancement = cosmetic.resolveAdvancement();
        if (advancement == null) {
            String fallback = cosmetic.getAdvancement();
            return Component.text((fallback == null || fallback.isBlank()) ? "Unknown" : fallback);
        }

        return advancement.displayName();
    }

    private @Nullable String getMetadataValue(@Nullable ConfigurationNode cosmeticConfig, @NotNull String path) {
        if (cosmeticConfig == null) return null;
        String value = cosmeticConfig.node(path).getString();
        if (value == null || value.isBlank()) return null;
        return value;
    }

    private @NotNull List<String> getRawLore(@NotNull ItemMeta itemMeta) {
        if (HibiscusCommonsPlugin.isOnPaper()) {
            if (!itemMeta.hasLore() || itemMeta.lore() == null) return new ArrayList<>();
            List<String> lore = new ArrayList<>();
            for (Component line : itemMeta.lore()) {
                lore.add(MiniMessage.miniMessage().serialize(line));
            }
            return lore;
        }

        if (!itemMeta.hasLore() || itemMeta.getLore() == null) return new ArrayList<>();
        return new ArrayList<>(itemMeta.getLore());
    }

    private void setRawLore(@NotNull ItemMeta itemMeta, @NotNull List<String> loreLines, @NotNull Cosmetic cosmetic) {
        if (HibiscusCommonsPlugin.isOnPaper()) {
            Component advancementDisplay = resolveAdvancementDisplay(cosmetic);
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                Component deserialized;
                if (line.contains("{advancement}") && advancementDisplay != null) {
                    String parsed = line.replace("{advancement}", "<hmcc_advancement>");
                    deserialized = MiniMessage.miniMessage().deserialize(parsed, Placeholder.component("hmcc_advancement", advancementDisplay));
                } else {
                    deserialized = MiniMessage.miniMessage().deserialize(line);
                }
                lore.add(deserialized.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
            }
            itemMeta.lore(lore);
            return;
        }

        itemMeta.setLore(new ArrayList<>(loreLines));
    }

    private @NotNull String getMenuCosmeticName(@NotNull Player viewer, @NotNull ConfigurationNode config, @NotNull Cosmetic cosmetic) {
        String rawName = config.node("item", "name").getString();
        if (rawName == null || rawName.isBlank()) return cosmetic.getId();
        return MessagesUtil.processStringNoKeyString(viewer, rawName);
    }
}
