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
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import lombok.Getter;
import me.lojosho.hibiscuscommons.config.serializer.ItemSerializer;
import me.lojosho.hibiscuscommons.hooks.Hooks;
import me.lojosho.hibiscuscommons.scheduler.TaskHandle;
import me.lojosho.hibiscuscommons.util.AdventureUtils;
import me.lojosho.shaded.configurate.ConfigurationNode;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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

            if (type instanceof TypeCosmetic && slots.isEmpty() && !configuredCosmeticSlots.isEmpty()) {
                if (!matchesCosmeticType(menuItem)) {
                    continue;
                }
                cosmeticItems.add(menuItem);
                continue;
            }

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
        final Component component = AdventureUtils.MINI_MESSAGE.deserialize(Hooks.processPlaceholders(viewer, this.title));
        Gui gui = Gui.gui()
                .title(component)
                .rows(rows)
                .type(GuiType.CHEST)
                .inventory((title, owner, type) -> Bukkit.createInventory(owner, rows * 9, title))
                .create();
        MenuSession session = new MenuSession(this, gui, cosmeticHolder);

        gui.setDefaultClickAction(event -> event.setCancelled(true));

        AtomicReference<TaskHandle> refreshTask = new AtomicReference<>(TaskHandle.NONE);
        gui.setOpenGuiAction(event -> {
            Menus.setSession(viewer.getUniqueId(), session);
            Runnable run = () -> {
                if (gui.getInventory().getViewers().isEmpty()) {
                    TaskHandle task = refreshTask.getAndSet(TaskHandle.NONE);
                    task.cancel();
                }

                updateMenu(viewer, cosmeticHolder, gui, session);
            };

            if (refreshRate != -1) {
                TaskHandle task = HMCCosmeticsPlugin.getInstance().getScheduler()
                    .runAtEntityAtFixedRate(viewer, run, 0, refreshRate);
                TaskHandle previous = refreshTask.getAndSet(task);
                previous.cancel();
            } else {
                HMCCosmeticsPlugin.getInstance().getScheduler().runAtEntity(viewer, run);
            }
        });

        gui.setCloseGuiAction(event -> {
            if (cosmeticHolder instanceof CosmeticUser user) {
                PlayerMenuCloseEvent closeEvent = new PlayerMenuCloseEvent(user, this, event.getReason());
                HMCCosmeticsPlugin.getInstance().getScheduler()
                    .runAtEntity(viewer, () -> Bukkit.getPluginManager().callEvent(closeEvent));
            }

            Menus.removeSession(viewer.getUniqueId(), gui);
            TaskHandle task = refreshTask.getAndSet(TaskHandle.NONE);
            task.cancel();
        });

        Runnable openGuiTask = () -> {
            Menus.setSession(viewer.getUniqueId(), session);
            gui.open(viewer);
            updateMenu(viewer, cosmeticHolder, gui, session); // fixes shading? I know I do this twice but it's easier than writing a whole new class to deal with this shit
        };

        // API
        if (cosmeticHolder instanceof CosmeticUser user) {
            PlayerMenuOpenEvent event = new PlayerMenuOpenEvent(user, this);
            HMCCosmeticsPlugin.getInstance().getScheduler().runAtEntity(viewer, () -> {
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    openGuiTask.run();
                }
            });
        }
        // Internal
        else {
            HMCCosmeticsPlugin.getInstance().getScheduler().runAtEntity(viewer, openGuiTask);
        }
    }

    public void refresh(@NotNull Player viewer, @NotNull MenuSession session) {
        updateMenu(viewer, session.getCosmeticHolder(), session.getGui(), session);
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

    private boolean matchesCosmeticType(@NotNull MenuItem item) {
        if (!cosmeticTypeConfigured) return true;
        if (cosmeticType == null) return false;

        String cosmeticId = item.itemConfig().node("cosmetic").getString("");
        Cosmetic cosmetic = Cosmetics.getCosmetic(cosmeticId);
        if (cosmetic == null) {
            MessagesUtil.sendDebugMessages("Unable to resolve cosmetic '" + cosmeticId + "' for type filtering in menu " + getId(), Level.WARNING);
            return false;
        }

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

    private void updateMenu(Player viewer, CosmeticHolder cosmeticHolder, Gui gui, MenuSession session) {
        List<MenuItem> visibleCosmeticItems = getVisibleCosmeticItems(viewer);
        session.setPage(session.getPage(), getTotalPages(visibleCosmeticItems));
        StringBuilder title = new StringBuilder(this.title);

        int row = 0;
        if (shading) {
            for (int i = 0; i < gui.getInventory().getSize(); i++) {
                // Handles the title
                if (i % 9 == 0) {
                    if (row == 0) {
                        title.append(Settings.getFirstRowShift()); // Goes back to the start of the gui
                    } else {
                        title.append(Settings.getSequentRowShift());
                    }
                    row += 1;
                } else {
                    title.append(Settings.getIndividualColumnShift()); // Goes to the next slot
                }

                boolean occupied = false;
                MenuItem item = getPrimaryMenuItem(viewer, i, session, visibleCosmeticItems);
                if (item != null) {
                    updateItem(viewer, cosmeticHolder, gui, session, i, visibleCosmeticItems);

                    if (item.type() instanceof TypeCosmetic) {
                        Cosmetic cosmetic = Cosmetics.getCosmetic(item.itemConfig().node("cosmetic").getString(""));
                        if (cosmetic == null) continue;
                        if (cosmeticHolder.hasCosmeticInSlot(cosmetic)) {
                            title.append(Settings.getEquippedCosmeticColor());
                        } else {
                            if (cosmeticHolder.canEquipCosmetic(cosmetic, true)) {
                                title.append(Settings.getEquipableCosmeticColor());
                            } else {
                                title.append(Settings.getLockedCosmeticColor());
                            }
                        }
                        occupied = true;
                    }
                } else {
                    clearSlot(gui, i);
                }
                if (occupied) {
                    title.append(Settings.getBackground().replaceAll("<row>", String.valueOf(row)));
                } else {
                    title.append(Settings.getClearBackground().replaceAll("<row>", String.valueOf(row)));
                }
            }
            MessagesUtil.sendDebugMessages("Updated menu with title " + title);
            gui.updateTitle(AdventureUtils.MINI_MESSAGE.deserialize(Hooks.processPlaceholders(viewer, title.toString())));
        } else {
            for (int i = 0; i < gui.getInventory().getSize(); i++) {
                updateItem(viewer, cosmeticHolder, gui, session, i, visibleCosmeticItems);
            }
        }
    }

    private void updateItem(Player viewer, CosmeticHolder cosmeticHolder, Gui gui, MenuSession session, int slot, @NotNull List<MenuItem> visibleCosmeticItems) {
        List<MenuItem> menuItems = getMenuItems(viewer, slot, session, visibleCosmeticItems);
        if (menuItems.isEmpty()) {
            clearSlot(gui, slot);
            return;
        }

        for (MenuItem item : menuItems) {
            Type type = item.type();
            ItemStack modifiedItem = getMenuItem(viewer, cosmeticHolder, type, item.itemConfig(), item.item().clone(), slot);
            if (modifiedItem.getType().isAir()) continue;
            GuiItem guiItem = ItemBuilder.from(modifiedItem).asGuiItem();
            guiItem.setAction(event -> {
                UUID uuid = viewer.getUniqueId();
                if (Settings.isMenuClickCooldown()) {
                    Long userCooldown = Menus.getCooldown(uuid);
                    if (userCooldown != 0 && (System.currentTimeMillis() - Menus.getCooldown(uuid) <= getCooldown())) {
                        MessagesUtil.sendDebugMessages("Cooldown for " + viewer.getUniqueId() + " System time: " + System.currentTimeMillis() + " Cooldown: " + Menus.getCooldown(viewer.getUniqueId()) + " Difference: " + (System.currentTimeMillis() - Menus.getCooldown(viewer.getUniqueId())));
                        MessagesUtil.sendMessage(viewer, "on-click-cooldown");
                        return;
                    } else {
                        Menus.addCooldown(uuid, System.currentTimeMillis());
                    }
                }
                MessagesUtil.sendDebugMessages("Updated Menu Item in slot number " + slot);
                final ClickType clickType = event.getClick();
                if (type != null) type.run(viewer, cosmeticHolder, item.itemConfig(), clickType);
                updateMenu(viewer, cosmeticHolder, gui, session);
            });

            MessagesUtil.sendDebugMessages("Set an item in slot " + slot + " in the menu of " + getId());
            gui.updateItem(slot, guiItem);
            return;
        }

        clearSlot(gui, slot);
    }

    private void clearSlot(@NotNull Gui gui, int slot) {
        if (slot < 0 || slot >= gui.getInventory().getSize()) {
            return;
        }

        gui.getGuiItems().remove(slot);
        gui.getInventory().setItem(slot, null);
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

    private MenuItem getPrimaryMenuItem(@NotNull Player viewer, int slot, @NotNull MenuSession session, @NotNull List<MenuItem> visibleCosmeticItems) {
        List<MenuItem> menuItems = getMenuItems(viewer, slot, session, visibleCosmeticItems);
        if (menuItems.isEmpty()) return null;
        return menuItems.get(0);
    }

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

    private boolean shouldShowMenuItem(@NotNull Player viewer, @NotNull MenuItem item) {
        if (!(item.type() instanceof TypeCosmetic)) return true;

        ConfigurationNode itemConfig = item.itemConfig();
        if (itemConfig.node("visible").getBoolean(true)) return true;

        String cosmeticId = itemConfig.node("cosmetic").getString("");
        if (cosmeticId.isBlank()) return true;

        Cosmetic cosmetic = Cosmetics.getCosmetic(cosmeticId);
        if (cosmetic == null) return true;
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
        if (!itemStack.hasItemMeta()) return itemStack;
        return type.setItem(viewer, cosmeticHolder, config, itemStack, slot);
    }

    public boolean canOpen(Player player) {
        if (permissionNode.isEmpty()) return true;
        return player.isOp() || player.hasPermission(permissionNode);
    }

    public static Comparator<MenuItem> priorityCompare = Comparator.comparing(MenuItem::priority).reversed();
}
