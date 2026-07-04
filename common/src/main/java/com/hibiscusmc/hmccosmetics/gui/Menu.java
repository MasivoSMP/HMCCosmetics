package com.hibiscusmc.hmccosmetics.gui;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.api.events.PlayerMenuCloseEvent;
import com.hibiscusmc.hmccosmetics.api.events.PlayerMenuOpenEvent;
import com.hibiscusmc.hmccosmetics.config.Settings;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticHolder;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetics;
import com.hibiscusmc.hmccosmetics.gui.type.Type;
import com.hibiscusmc.hmccosmetics.gui.type.Types;
import com.hibiscusmc.hmccosmetics.gui.type.types.TypeCosmetic;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import lombok.Getter;
import lombok.Setter;
import me.lojosho.hibiscuscommons.HibiscusCommonsPlugin;
import me.lojosho.hibiscuscommons.config.serializer.ItemSerializer;
import me.lojosho.shaded.configurate.ConfigurationNode;
import gg.masivo.masivogui.api.MasivoGUIApi;
import gg.masivo.masivogui.api.MenuCloseContext;
import gg.masivo.masivogui.api.MenuDefinitionContext;
import gg.masivo.masivogui.api.MenuKey;
import gg.masivo.masivogui.api.MenuListener;
import gg.masivo.masivogui.api.OpenRequest;
import gg.masivo.masivogui.api.OpenResult;
import gg.masivo.masivogui.api.OpenStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class Menu {

    @Getter
    private final String id;
    @Getter
    private final String title;
    @Getter
    private final int rows;
    @Getter
    private final Long cooldown;
    @Getter
    private final ConfigurationNode config;
    @Getter
    private final String permissionNode;
    private final HashMap<Integer, List<MenuItem>> items;
    private final List<MenuItem> cosmeticItems;
    @Nullable
    private final CosmeticSlot cosmeticType;
    private final boolean cosmeticTypeConfigured;
    private final List<Integer> configuredCosmeticSlots;
    private final List<Integer> cosmeticSlots;
    private final HashMap<Integer, Integer> cosmeticSlotIndexes;
    @Getter
    private final int refreshRate;
    @Getter
    private final boolean shading;

    @Getter
    @Setter
    @Nullable
    private MenuKey masivoGUIKey;

    public Menu(String id, @NotNull ConfigurationNode config) {
        this.id = config.node("id").getString(id);
        this.config = config;

        title = config.node("title").getString("chest");
        rows = config.node("rows").getInt(1);
        cooldown = config.node("click-cooldown").getLong(Settings.getDefaultMenuCooldown());
        permissionNode = config.node("permission").getString("");
        refreshRate = config.node("refresh-rate").getInt(-1);
        shading = config.node("shading").getBoolean(Settings.isDefaultShading());

        items = new HashMap<>();
        cosmeticItems = new ArrayList<>();
        String configuredCosmeticType = config.node("cosmetic-type").getString("");
        cosmeticTypeConfigured = !configuredCosmeticType.isBlank();
        cosmeticType = resolveCosmeticType(configuredCosmeticType);
        configuredCosmeticSlots = getSlots(config.node("cosmetic-slots"));
        cosmeticSlots = new ArrayList<>();
        cosmeticSlotIndexes = new HashMap<>();
        setupItems();
        setupCosmeticSlots();

        Menus.addMenu(this);
    }

    private void setupItems() {
        for (ConfigurationNode config : config.node("items").childrenMap().values()) {
            int priority = config.node("priority").getInt(1);

            Type type = Types.getDefaultType();
            if (!config.node("type").virtual()) {
                String typeId = config.node("type").getString("");
                if (Types.isType(typeId)) type = Types.getType(typeId);
            }

            if (type instanceof TypeCosmetic) continue;

            List<Integer> slots = getSlots(config.node("slots"));

            ItemStack item;
            try {
                item = ItemSerializer.INSTANCE.deserialize(ItemStack.class, config.node("item"));
            } catch (Exception e) {
                MessagesUtil.sendDebugMessages("Unable to get valid item for " + config.key().toString() + " " + e.getMessage());
                continue;
            }

            if (item == null) {
                MessagesUtil.sendDebugMessages("Something went wrong with the item creation for " + config.key().toString());
                continue;
            }

            MenuItem menuItem = new MenuItem(slots, item, type, priority, config);

            if (slots.isEmpty()) {
                MessagesUtil.sendDebugMessages("Slot is empty for " + config.key().toString());
                continue;
            }

            for (Integer slot : slots) {
                if (items.containsKey(slot)) {
                    List<MenuItem> menuItems = items.get(slot);
                    menuItems.add(menuItem);
                    menuItems.sort(priorityCompare);
                    items.put(slot, menuItems);
                } else {
                    items.put(slot, new ArrayList<>(List.of(menuItem)));
                }
            }
        }

        setupCosmeticItems();
    }

    private void setupCosmeticItems() {
        if (configuredCosmeticSlots.isEmpty()) return;

        Type type = Types.getType("cosmetic");
        if (!(type instanceof TypeCosmetic)) {
            MessagesUtil.sendDebugMessages("Unable to populate cosmetics in menu " + getId() + " because the cosmetic menu type is unavailable.", Level.WARNING);
            return;
        }

        for (Cosmetic cosmetic : Cosmetics.values()) {
            if (!cosmetic.isShowInMenu()) continue;
            if (!matchesCosmeticType(cosmetic)) continue;

            ItemStack item = cosmetic.getItem();
            if (item == null) {
                MessagesUtil.sendDebugMessages("Unable to create menu item for " + cosmetic.getId() + " because it has no display item.", Level.WARNING);
                continue;
            }

            ConfigurationNode itemConfig = createCosmeticMenuItemConfig(cosmetic);
            if (itemConfig == null) continue;

            cosmeticItems.add(new MenuItem(Collections.emptyList(), item, type, 1, itemConfig));
        }
    }

    @Nullable
    private ConfigurationNode createCosmeticMenuItemConfig(@NotNull Cosmetic cosmetic) {
        ConfigurationNode cosmeticConfig = cosmetic.getConfig();
        if (cosmeticConfig == null) return null;

        ConfigurationNode itemConfig = cosmeticConfig.copy();
        try {
            itemConfig.node("type").set("cosmetic");
            itemConfig.node("cosmetic").set(cosmetic.getId());
            itemConfig.node("visible").set(cosmetic.isVisibleInMenu());
        } catch (Exception e) {
            MessagesUtil.sendDebugMessages("Unable to create menu config for " + cosmetic.getId() + ": " + e.getMessage(), Level.WARNING);
            return null;
        }

        return itemConfig;
    }

    private void setupCosmeticSlots() {
        cosmeticSlots.clear();
        cosmeticSlotIndexes.clear();

        int menuSize = rows * 9;
        for (Integer slot : configuredCosmeticSlots) {
            if (slot < 0 || slot >= menuSize) {
                MessagesUtil.sendDebugMessages("Ignoring cosmetic slot " + slot + " in menu " + getId() + " because it is outside the menu size.", Level.WARNING);
                continue;
            }

            if (items.containsKey(slot)) {
                MessagesUtil.sendDebugMessages("Ignoring cosmetic slot " + slot + " in menu " + getId() + " because a normal menu item already uses it.", Level.WARNING);
                continue;
            }

            if (cosmeticSlotIndexes.containsKey(slot)) continue;

            cosmeticSlotIndexes.put(slot, cosmeticSlots.size());
            cosmeticSlots.add(slot);
        }
    }

    public void openMenu(CosmeticUser user) {
        openMenu(user, false);
    }

    public void openMenu(@NotNull CosmeticUser user, boolean ignorePermission) {
        Player player = user.getPlayer();
        if (player == null) return;
        openMenu(player, user, ignorePermission);
    }

    public void openMenu(@NotNull Player viewer, @NotNull CosmeticHolder cosmeticHolder) {
        openMenu(viewer, cosmeticHolder, false);
    }

    public void openMenu(@NotNull Player viewer, @NotNull CosmeticHolder cosmeticHolder, boolean ignorePermission) {
        if (!ignorePermission && !permissionNode.isEmpty()) {
            if (!viewer.hasPermission(permissionNode) && !viewer.isOp()) {
                MessagesUtil.sendMessage(viewer, "no-permission");
                return;
            }
        }

        MasivoGUIApi api = Menus.getMasivoGUIApi();
        if (api == null || masivoGUIKey == null) {
            MessagesUtil.sendDebugMessages("MasivoGUI is not initialized for menu " + getId(), Level.WARNING);
            MessagesUtil.sendMessage(viewer, "invalid-menu");
            return;
        }

        Menus.markMenuOpenRequested();

        Runnable openTask = () -> {
            MenuSession session = new MenuSession(this, cosmeticHolder);
            Menus.setSession(viewer.getUniqueId(), session);

            OpenRequest request = OpenRequest.builder()
                    .sessionValue(Menus.getCosmeticsPageKey(), String.valueOf(session.getPage()))
                    .listener(new MenuListener() {
                        @Override
                        public void onOpen(gg.masivo.masivogui.api.MenuSession openedSession) {
                            session.setExternalSessionId(openedSession.id());
                        }

                        @Override
                        public void onClose(MenuCloseContext closeContext) {
                            if (cosmeticHolder instanceof CosmeticUser user) {
                                PlayerMenuCloseEvent closeEvent = new PlayerMenuCloseEvent(user, Menu.this, Menus.mapCloseReason(closeContext.reason()));
                                HMCCosmeticsPlugin.getInstance().getScheduler()
                                        .runAtEntity(viewer, () -> Bukkit.getPluginManager().callEvent(closeEvent));
                            }

                            Menus.removeSession(viewer.getUniqueId(), closeContext.session().id());
                        }
                    })
                    .build();

            OpenResult result = api.open(masivoGUIKey, viewer, request);
            if (result.sessionId() != null) {
                session.setExternalSessionId(result.sessionId());
            }

            if (result.successful()) return;

            MessagesUtil.sendDebugMessages("Failed to open MasivoGUI menu " + getId() + " for " + viewer.getName()
                    + " (status=" + result.status() + ", message='" + result.message() + "')", Level.WARNING);
            Menus.removeSession(viewer.getUniqueId(), session);
            if (result.status() == OpenStatus.MENU_NOT_FOUND || result.status() == OpenStatus.FAILED) {
                MessagesUtil.sendMessage(viewer, "invalid-menu");
            }
        };

        if (cosmeticHolder instanceof CosmeticUser user) {
            PlayerMenuOpenEvent event = new PlayerMenuOpenEvent(user, this);
            HMCCosmeticsPlugin.getInstance().getScheduler().runAtEntity(viewer, () -> {
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) openTask.run();
            });
            return;
        }

        HMCCosmeticsPlugin.getInstance().getScheduler().runAtEntity(viewer, openTask);
    }

    public void refresh(@NotNull Player viewer, @NotNull MenuSession session) {
        Menus.refresh(viewer, session);
    }

    public int getTotalPages() {
        return getTotalPages(cosmeticItems);
    }

    public int getTotalPages(@NotNull Player viewer, @NotNull CosmeticHolder cosmeticHolder) {
        return getTotalPages(getVisibleCosmeticItems(viewer));
    }

    private int getTotalPages(@NotNull List<MenuItem> cosmeticMenuItems) {
        if (cosmeticSlots.isEmpty() || cosmeticMenuItems.isEmpty()) return 1;
        return Math.max(1, (int) Math.ceil((double) cosmeticMenuItems.size() / cosmeticSlots.size()));
    }

    public boolean handleMenuClick(@NotNull Player viewer, @NotNull MenuSession session, int slot, @NotNull ClickType clickType) {
        CosmeticHolder cosmeticHolder = session.getCosmeticHolder();
        List<MenuItem> visibleCosmeticItems = getVisibleCosmeticItems(viewer);
        session.setPage(session.getPage(), getTotalPages(visibleCosmeticItems));

        RenderedSlot renderedSlot = resolveRenderedSlot(viewer, cosmeticHolder, session, slot, visibleCosmeticItems);
        if (renderedSlot == null) return false;

        if (Settings.isMenuClickCooldown()) {
            UUID uuid = viewer.getUniqueId();
            Long userCooldown = Menus.getCooldown(uuid);
            if (userCooldown != 0 && (System.currentTimeMillis() - userCooldown <= getCooldown())) {
                MessagesUtil.sendDebugMessages("Cooldown for " + viewer.getUniqueId() + " System time: " + System.currentTimeMillis() + " Cooldown: " + userCooldown + " Difference: " + (System.currentTimeMillis() - userCooldown));
                MessagesUtil.sendMessage(viewer, "on-click-cooldown");
                return false;
            }
            Menus.addCooldown(uuid, System.currentTimeMillis());
        }

        Type type = renderedSlot.menuItem().type();
        if (type != null) {
            type.run(viewer, cosmeticHolder, renderedSlot.menuItem().itemConfig(), clickType);
        }

        return true;
    }

    public @NotNull YamlConfiguration buildMasivoGUIConfiguration(@NotNull MenuDefinitionContext context) throws IOException {
        Player viewer = context.player();
        if (viewer == null) {
            return createBaseMasivoGUIConfiguration();
        }

        MenuSession session = Menus.getSession(viewer.getUniqueId());
        if (session == null || session.getMenu() != this) {
            CosmeticUser user = CosmeticUsers.getUser(viewer);
            if (user == null) throw new IOException("Cosmetic user is unavailable for " + viewer.getName());
            session = new MenuSession(this, user);
            Menus.setSession(viewer.getUniqueId(), session);
        }

        syncPageFromMasivoGUIContext(context, session, viewer);

        RenderData renderData = buildStaticRenderData(viewer, session.getCosmeticHolder(), session);

        YamlConfiguration generated = createBaseMasivoGUIConfiguration();
        generated.set("title", renderData.title());

        ConfigurationSection layoutSection = generated.getConfigurationSection("layout");
        ConfigurationSection itemsSection = generated.getConfigurationSection("items");
        if (layoutSection == null || itemsSection == null) {
            throw new IOException("Unable to initialize MasivoGUI layout/items sections for menu " + getId());
        }

        for (Map.Entry<Integer, RenderedSlot> entry : renderData.renderedSlots().entrySet()) {
            int slot = entry.getKey();
            RenderedSlot renderedSlot = entry.getValue();

            String itemId = "slot_" + slot;
            layoutSection.set(String.valueOf(slot), List.of(itemId));

            ConfigurationSection itemSection = itemsSection.createSection(itemId);
            writeInventoryItem(itemSection, renderedSlot.itemStack());
            writeClickActions(itemSection, slot);
        }

        writeCosmeticRepeatItem(itemsSection);

        return generated;
    }

    private void syncPageFromMasivoGUIContext(@NotNull MenuDefinitionContext context,
                                              @NotNull MenuSession session,
                                              @NotNull Player viewer) {
        String rawPage = context.sessionValue(Menus.getCosmeticsPageKey());
        if (rawPage == null || rawPage.isBlank()) return;

        try {
            session.setPage(Integer.parseInt(rawPage), session.getTotalPages(viewer));
        } catch (NumberFormatException ignored) {
        }
    }

    private @NotNull YamlConfiguration createBaseMasivoGUIConfiguration() {
        YamlConfiguration generated = new YamlConfiguration();
        generated.set("type", "inventory");
        generated.set("rows", rows);
        generated.set("title", title);
        generated.set("update-delay", Math.max(0, refreshRate));
        generated.createSection("layout");
        generated.createSection("items");
        return generated;
    }

    private @NotNull RenderData buildStaticRenderData(@NotNull Player viewer, @NotNull CosmeticHolder cosmeticHolder, @NotNull MenuSession session) {
        List<MenuItem> visibleCosmeticItems = getVisibleCosmeticItems(viewer);
        session.setPage(session.getPage(), getTotalPages(visibleCosmeticItems));

        String finalTitle = shading
                ? buildShadedTitle(viewer, cosmeticHolder, session, visibleCosmeticItems)
                : title;

        int menuSize = rows * 9;
        Map<Integer, RenderedSlot> renderedSlots = new HashMap<>();
        for (int slot = 0; slot < menuSize; slot++) {
            RenderedSlot renderedSlot = resolveStaticRenderedSlot(viewer, cosmeticHolder, slot);
            if (renderedSlot != null) renderedSlots.put(slot, renderedSlot);
        }

        return new RenderData(finalTitle, renderedSlots);
    }

    @Nullable
    private RenderedSlot resolveStaticRenderedSlot(@NotNull Player viewer,
                                                   @NotNull CosmeticHolder cosmeticHolder,
                                                   int slot) {
        List<MenuItem> slotItems = items.get(slot);
        if (slotItems == null || slotItems.isEmpty()) return null;

        for (MenuItem item : slotItems) {
            if (!shouldShowMenuItem(viewer, item)) continue;
            Type type = item.type();
            ItemStack modifiedItem = getMenuItem(viewer, cosmeticHolder, type, item.itemConfig(), item.item().clone(), slot);
            if (modifiedItem.getType().isAir()) continue;
            return new RenderedSlot(item, modifiedItem);
        }

        return null;
    }

    private void writeCosmeticRepeatItem(@NotNull ConfigurationSection itemsSection) {
        if (cosmeticSlots.isEmpty() || cosmeticItems.isEmpty()) return;

        ConfigurationSection itemSection = itemsSection.createSection("hmcc_cosmetics");
        ConfigurationSection repeatSection = itemSection.createSection("repeat");
        repeatSection.set("provider", Menus.getCosmeticsListProviderId());
        repeatSection.set("slots", cosmeticSlotsToLayout());
        repeatSection.set("page-key", Menus.getCosmeticsPageKey());
        writeClickActions(itemSection, -1);
    }

    @NotNull
    private List<String> cosmeticSlotsToLayout() {
        List<String> layout = new ArrayList<>();
        for (Integer slot : cosmeticSlots) {
            layout.add(String.valueOf(slot));
        }
        return layout;
    }

    private @NotNull String buildShadedTitle(@NotNull Player viewer,
                                             @NotNull CosmeticHolder cosmeticHolder,
                                             @NotNull MenuSession session,
                                             @NotNull List<MenuItem> visibleCosmeticItems) {
        StringBuilder shadedTitle = new StringBuilder(this.title);
        int row = 0;

        for (int slot = 0; slot < rows * 9; slot++) {
            if (slot % 9 == 0) {
                if (row == 0) {
                    shadedTitle.append(Settings.getFirstRowShift());
                } else {
                    shadedTitle.append(Settings.getSequentRowShift());
                }
                row += 1;
            } else {
                shadedTitle.append(Settings.getIndividualColumnShift());
            }

            boolean occupied = false;
            MenuItem primaryItem = getPrimaryMenuItem(viewer, slot, session, visibleCosmeticItems);
            if (primaryItem != null && primaryItem.type() instanceof TypeCosmetic) {
                Cosmetic cosmetic = Cosmetics.getCosmetic(primaryItem.itemConfig().node("cosmetic").getString(""));
                if (cosmetic != null) {
                    if (cosmeticHolder.hasCosmeticInSlot(cosmetic)) {
                        shadedTitle.append(Settings.getEquippedCosmeticColor());
                    } else if (cosmeticHolder.canEquipCosmetic(cosmetic, true)) {
                        shadedTitle.append(Settings.getEquipableCosmeticColor());
                    } else {
                        shadedTitle.append(Settings.getLockedCosmeticColor());
                    }
                    occupied = true;
                }
            }

            if (occupied) {
                shadedTitle.append(Settings.getBackground().replace("<row>", String.valueOf(row)));
            } else {
                shadedTitle.append(Settings.getClearBackground().replace("<row>", String.valueOf(row)));
            }
        }

        return shadedTitle.toString();
    }

    @Nullable
    private RenderedSlot resolveRenderedSlot(@NotNull Player viewer,
                                             @NotNull CosmeticHolder cosmeticHolder,
                                             @NotNull MenuSession session,
                                             int slot,
                                             @NotNull List<MenuItem> visibleCosmeticItems) {
        List<MenuItem> menuItems = getMenuItems(viewer, slot, session, visibleCosmeticItems);
        if (menuItems.isEmpty()) return null;

        for (MenuItem item : menuItems) {
            Type type = item.type();
            ItemStack modifiedItem = getMenuItem(viewer, cosmeticHolder, type, item.itemConfig(), item.item().clone(), slot);
            if (modifiedItem.getType().isAir()) continue;
            return new RenderedSlot(item, modifiedItem);
        }

        return null;
    }

    private void writeClickActions(@NotNull ConfigurationSection itemSection, int slot) {
        writeClickAction(itemSection, "left-click", slot, ClickType.LEFT);
        writeClickAction(itemSection, "right-click", slot, ClickType.RIGHT);
        writeClickAction(itemSection, "shift-left-click", slot, ClickType.SHIFT_LEFT);
        writeClickAction(itemSection, "shift-right-click", slot, ClickType.SHIFT_RIGHT);
    }

    private void writeClickAction(@NotNull ConfigurationSection section, @NotNull String key, int slot, @NotNull ClickType clickType) {
        ConfigurationSection clickSection = section.createSection(key);
        clickSection.set("commands", List.of(buildClickActionCommand(slot, clickType)));
    }

    @NotNull
    private String buildClickActionCommand(int slot, @NotNull ClickType clickType) {
        String slotValue = slot < 0 ? "{slot}" : String.valueOf(slot);
        return "[" + Menus.getMasivoGUINamespace() + ":" + Menus.getMenuClickActionId() + "] slot=" + slotValue + " click=" + clickType.name();
    }

    private void writeInventoryItem(@NotNull ConfigurationSection section, @NotNull ItemStack itemStack) {
        section.set("material", itemStack.getType().name());

        if (itemStack.getAmount() != 1) {
            section.set("stack", String.valueOf(itemStack.getAmount()));
        }

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) return;

        if (HibiscusCommonsPlugin.isOnPaper()) {
            if (itemMeta.hasDisplayName() && itemMeta.displayName() != null) {
                section.set("name", MiniMessage.miniMessage().serialize(itemMeta.displayName()));
            }

            if (itemMeta.hasLore() && itemMeta.lore() != null) {
                List<String> lore = new ArrayList<>();
                for (Component line : itemMeta.lore()) {
                    lore.add(MiniMessage.miniMessage().serialize(line));
                }
                section.set("lore", lore);
            }
        } else {
            if (itemMeta.hasDisplayName()) {
                section.set("name", itemMeta.getDisplayName());
            }

            if (itemMeta.hasLore() && itemMeta.getLore() != null) {
                section.set("lore", new ArrayList<>(itemMeta.getLore()));
            }
        }

        if (itemMeta.hasCustomModelData()) {
            section.set("custom-model-data", String.valueOf(itemMeta.getCustomModelData()));
        }

        if (itemMeta.hasEnchants()) {
            List<String> enchantments = new ArrayList<>();
            itemMeta.getEnchants().forEach((enchantment, level) ->
                    enchantments.add(enchantment.getKey().asString() + " " + level));
            section.set("enchantments", enchantments);
        }

        if (itemMeta instanceof Damageable damageable && damageable.getDamage() > 0) {
            section.set("damage", String.valueOf(damageable.getDamage()));
        }

        String itemModel = readMetaMethodAsString(itemMeta, "hasItemModel", "getItemModel");
        if (itemModel != null && !itemModel.isBlank()) {
            section.set("item-model", itemModel);
        }
    }

    @Nullable
    private String readMetaMethodAsString(@NotNull ItemMeta itemMeta, @NotNull String hasMethod, @NotNull String getMethod) {
        try {
            Method has = itemMeta.getClass().getMethod(hasMethod);
            Object hasValue = has.invoke(itemMeta);
            if (!(hasValue instanceof Boolean boolValue) || !boolValue) return null;

            Method get = itemMeta.getClass().getMethod(getMethod);
            Object value = get.invoke(itemMeta);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean matchesCosmeticType(@NotNull Cosmetic cosmetic) {
        if (!cosmeticTypeConfigured) return true;
        if (cosmeticType == null) return false;
        return cosmeticType.equals(cosmetic.getSlot());
    }

    @Nullable
    private CosmeticSlot resolveCosmeticType(@Nullable String rawType) {
        if (rawType == null || rawType.isBlank()) return null;

        String normalized = rawType.trim().toUpperCase(Locale.ROOT);
        CosmeticSlot exact = CosmeticSlot.valueOf(normalized);
        if (exact != null) return exact;

        CosmeticSlot alias = switch (normalized) {
            case "HAT", "HATS", "HELMET", "HELMETS" -> CosmeticSlot.HELMET;
            case "CHESTPLATE", "CHESTPLATES", "CHEST", "CHESTS" -> CosmeticSlot.CHESTPLATE;
            case "LEGGING", "LEGGINGS", "PANTS", "PANT", "TROUSERS" -> CosmeticSlot.LEGGINGS;
            case "BOOT", "BOOTS", "SHOE", "SHOES" -> CosmeticSlot.BOOTS;
            case "HAND", "HANDS", "OFFHAND", "OFFHANDS" -> CosmeticSlot.OFFHAND;
            case "MAINHAND", "MAINHANDS" -> CosmeticSlot.MAINHAND;
            case "BACKPACK", "BACKPACKS" -> CosmeticSlot.BACKPACK;
            case "BALLOON", "BALLOONS" -> CosmeticSlot.BALLOON;
            default -> null;
        };

        if (alias == null) {
            MessagesUtil.sendDebugMessages("Invalid cosmetic-type '" + rawType + "' in menu " + getId() + ". Valid values are cosmetic slot names or aliases such as hats, hands, backpacks, or balloons.", Level.WARNING);
        }

        return alias;
    }

    @NotNull
    private List<MenuItem> getMenuItems(@NotNull Player viewer, int slot, @NotNull MenuSession session, @NotNull List<MenuItem> visibleCosmeticItems) {
        if (items.containsKey(slot)) {
            List<MenuItem> slotItems = items.get(slot);
            if (slotItems == null || slotItems.isEmpty()) return Collections.emptyList();

            List<MenuItem> visibleItems = null;
            for (MenuItem item : slotItems) {
                if (!shouldShowMenuItem(viewer, item)) continue;
                if (visibleItems == null) visibleItems = new ArrayList<>();
                visibleItems.add(item);
            }

            if (visibleItems == null) return Collections.emptyList();
            return visibleItems;
        }

        MenuItem cosmeticItem = getCosmeticMenuItem(slot, session.getPage(), visibleCosmeticItems);
        if (cosmeticItem == null) return Collections.emptyList();
        return Collections.singletonList(cosmeticItem);
    }

    @Nullable
    private MenuItem getPrimaryMenuItem(@NotNull Player viewer, int slot, @NotNull MenuSession session, @NotNull List<MenuItem> visibleCosmeticItems) {
        List<MenuItem> menuItems = getMenuItems(viewer, slot, session, visibleCosmeticItems);
        if (menuItems.isEmpty()) return null;
        return menuItems.get(0);
    }

    @Nullable
    private MenuItem getCosmeticMenuItem(int slot, int page, @NotNull List<MenuItem> visibleCosmeticItems) {
        Integer slotIndex = cosmeticSlotIndexes.get(slot);
        if (slotIndex == null || cosmeticSlots.isEmpty()) return null;

        int cosmeticIndex = (page * cosmeticSlots.size()) + slotIndex;
        if (cosmeticIndex < 0 || cosmeticIndex >= visibleCosmeticItems.size()) return null;
        return visibleCosmeticItems.get(cosmeticIndex);
    }

    @NotNull
    private List<MenuItem> getVisibleCosmeticItems(@NotNull Player viewer) {
        if (cosmeticItems.isEmpty()) return Collections.emptyList();

        List<MenuItem> visibleItems = new ArrayList<>();
        for (MenuItem item : cosmeticItems) {
            if (shouldShowMenuItem(viewer, item)) {
                visibleItems.add(item);
            }
        }
        return visibleItems;
    }

    public int getVisibleCosmeticItemCount(@NotNull Player viewer) {
        return getVisibleCosmeticItems(viewer).size();
    }

    @Nullable
    public ItemStack createCosmeticListItem(@NotNull Player viewer,
                                            @NotNull CosmeticHolder cosmeticHolder,
                                            int absoluteIndex,
                                            int slot) {
        List<MenuItem> visibleCosmeticItems = getVisibleCosmeticItems(viewer);
        if (absoluteIndex < 0 || absoluteIndex >= visibleCosmeticItems.size()) return null;

        MenuItem item = visibleCosmeticItems.get(absoluteIndex);
        Type type = item.type();
        ItemStack modifiedItem = getMenuItem(viewer, cosmeticHolder, type, item.itemConfig(), item.item().clone(), slot);
        if (modifiedItem.getType().isAir()) return null;
        return modifiedItem;
    }

    private boolean shouldShowMenuItem(@NotNull Player viewer, @NotNull MenuItem item) {
        if (!(item.type() instanceof TypeCosmetic)) return true;

        ConfigurationNode itemConfig = item.itemConfig();
        if (itemConfig.node("visible").getBoolean(true)) return true;

        String cosmeticId = itemConfig.node("cosmetic").getString("");
        if (cosmeticId.isBlank()) return true;

        Cosmetic cosmetic = Cosmetics.getCosmetic(cosmeticId);
        if (cosmetic == null) return true;

        CosmeticUser user = CosmeticUsers.getUser(viewer);
        if (user != null) return user.canEquipCosmetic(cosmetic, true);

        if (!cosmetic.requiresPermission()) return true;
        return cosmetic.hasPermission(viewer::hasPermission);
    }

    @NotNull
    private List<Integer> getSlots(@NotNull ConfigurationNode slotNode) {
        if (slotNode.virtual()) return new ArrayList<>();

        List<String> slotStrings = new ArrayList<>();
        if (!slotNode.childrenList().isEmpty()) {
            for (ConfigurationNode child : slotNode.childrenList()) {
                Object valueObject = child.raw();
                String value = valueObject == null ? null : String.valueOf(valueObject);
                if (value != null) slotStrings.add(value);
            }
        } else {
            Object valueObject = slotNode.raw();
            String value = valueObject == null ? null : String.valueOf(valueObject);
            if (value != null) slotStrings.add(value);
        }

        return getSlots(slotStrings);
    }

    @NotNull
    private List<Integer> getSlots(@NotNull List<String> slotString) {
        List<Integer> slots = new ArrayList<>();

        for (String a : slotString) {
            if (a == null) continue;

            String slotValue = a.replace(" ", "");
            if (slotValue.isEmpty()) continue;

            try {
                if (slotValue.contains("-")) {
                    String[] split = slotValue.split("-", 2);
                    int min = Integer.parseInt(split[0]);
                    int max = Integer.parseInt(split[1]);
                    slots.addAll(getSlots(min, max));
                } else {
                    slots.add(Integer.valueOf(slotValue));
                }
            } catch (NumberFormatException e) {
                MessagesUtil.sendDebugMessages("Invalid slot value '" + a + "' in menu " + getId(), Level.WARNING);
            }
        }

        return slots;
    }

    @NotNull
    private List<Integer> getSlots(int small, int max) {
        List<Integer> slots = new ArrayList<>();
        for (int i = small; i <= max; i++) slots.add(i);
        return slots;
    }

    @Contract("_, _, _, _, _, _ -> param4")
    @NotNull
    private ItemStack getMenuItem(Player viewer, CosmeticHolder cosmeticHolder, Type type, ConfigurationNode config, ItemStack itemStack, int slot) {
        if (type == null || !itemStack.hasItemMeta()) return itemStack;
        return type.setItem(viewer, cosmeticHolder, config, itemStack, slot);
    }

    public boolean canOpen(Player player) {
        if (permissionNode.isEmpty()) return true;
        return player.isOp() || player.hasPermission(permissionNode);
    }

    public static Comparator<MenuItem> priorityCompare = Comparator.comparing(MenuItem::priority).reversed();

    private record RenderedSlot(@NotNull MenuItem menuItem, @NotNull ItemStack itemStack) {
    }

    private record RenderData(@NotNull String title, @NotNull Map<Integer, RenderedSlot> renderedSlots) {
    }
}
