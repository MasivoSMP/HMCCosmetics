package com.hibiscusmc.hmccosmetics.gui;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.Settings;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import me.lojosho.shaded.configurate.CommentedConfigurationNode;
import me.lojosho.shaded.configurate.ConfigurateException;
import me.lojosho.shaded.configurate.yaml.YamlConfigurationLoader;
import me.rockyhawk.commandpanels.api.v1.ActionResult;
import me.rockyhawk.commandpanels.api.v1.CommandPanelsApi;
import me.rockyhawk.commandpanels.api.v1.MenuActionContext;
import me.rockyhawk.commandpanels.api.v1.MenuCloseReason;
import me.rockyhawk.commandpanels.api.v1.MenuDefinitionSource;
import me.rockyhawk.commandpanels.api.v1.MenuKey;
import me.rockyhawk.commandpanels.api.v1.MenuRegistration;
import me.rockyhawk.commandpanels.api.v1.MenuRegistrationOptions;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

public final class Menus {

    private static final String MENU_CLICK_ACTION_ID = "menu_click";

    private static final HashMap<String, Menu> MENUS = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, MenuSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<String, MenuRegistration> COMMAND_PANELS_REGISTRATIONS = new ConcurrentHashMap<>();
    private static final Map<String, MenuKey> COMMAND_PANELS_KEYS = new ConcurrentHashMap<>();
    private static final ThreadLocal<MenuActionContext> ACTION_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<ActionExecutionState> ACTION_EXECUTION_STATE = new ThreadLocal<>();

    @Nullable
    private static volatile CommandPanelsApi commandPanelsApi;
    private static volatile String commandPanelsNamespace = "hmccosmetics";

    private Menus() {
    }

    public static void addMenu(@NotNull Menu menu) {
        MENUS.put(menu.getId().toUpperCase(Locale.ROOT), menu);
    }

    @Nullable
    public static Menu getMenu(@NotNull String id) {
        return MENUS.get(id.toUpperCase(Locale.ROOT));
    }

    @Contract(pure = true)
    @NotNull
    public static Collection<Menu> getMenu() {
        return MENUS.values();
    }

    public static boolean hasMenu(@NotNull String id) {
        return MENUS.containsKey(id.toUpperCase(Locale.ROOT));
    }

    public static boolean hasMenu(@NotNull Menu menu) {
        return MENUS.containsValue(menu);
    }

    public static boolean hasDefaultMenu() {
        return MENUS.containsKey(Settings.getDefaultMenu().toUpperCase(Locale.ROOT));
    }

    @Nullable
    public static Menu getDefaultMenu() {
        return Menus.getMenu(Settings.getDefaultMenu());
    }

    @NotNull
    public static List<String> getMenuNames() {
        List<String> names = new ArrayList<>();
        for (Menu menu : MENUS.values()) {
            names.add(menu.getId());
        }
        return names;
    }

    public static Collection<Menu> values() {
        return MENUS.values();
    }

    public static void addCooldown(UUID uuid, long time) {
        COOLDOWNS.put(uuid, time);
    }

    public static Long getCooldown(UUID uuid) {
        return COOLDOWNS.getOrDefault(uuid, 0L);
    }

    public static void removeCooldown(UUID uuid) {
        COOLDOWNS.remove(uuid);
    }

    public static void setSession(@NotNull UUID uuid, @NotNull MenuSession session) {
        SESSIONS.put(uuid, session);
    }

    @Nullable
    public static MenuSession getSession(@NotNull UUID uuid) {
        return SESSIONS.get(uuid);
    }

    public static void removeSession(@NotNull UUID uuid) {
        SESSIONS.remove(uuid);
    }

    public static void removeSession(@NotNull UUID uuid, @NotNull MenuSession expected) {
        SESSIONS.computeIfPresent(uuid, (ignored, session) -> session == expected ? null : session);
    }

    public static void removeSession(@NotNull UUID uuid, @Nullable UUID expectedExternalSessionId) {
        SESSIONS.computeIfPresent(uuid, (ignored, session) -> {
            if (expectedExternalSessionId == null) return null;
            return expectedExternalSessionId.equals(session.getExternalSessionId()) ? null : session;
        });
    }

    @Nullable
    public static CommandPanelsApi getCommandPanelsApi() {
        return commandPanelsApi;
    }

    @NotNull
    public static String getCommandPanelsNamespace() {
        return commandPanelsNamespace;
    }

    @NotNull
    public static String getMenuClickActionId() {
        return MENU_CLICK_ACTION_ID;
    }

    @Nullable
    public static MenuKey getMenuKey(@NotNull Menu menu) {
        return COMMAND_PANELS_KEYS.get(menu.getId().toUpperCase(Locale.ROOT));
    }

    public static boolean refresh(@NotNull Player viewer, @NotNull MenuSession session) {
        if (SESSIONS.get(viewer.getUniqueId()) != session) return false;

        CommandPanelsApi api = commandPanelsApi;
        if (api == null) return false;

        boolean refreshed = api.refresh(viewer);
        if (refreshed) {
            markRefreshRequested();
        }
        return refreshed;
    }

    public static boolean closeActiveMenu(@NotNull Player viewer) {
        MenuActionContext actionContext = ACTION_CONTEXT.get();
        if (actionContext != null && actionContext.player() != null
                && viewer.getUniqueId().equals(actionContext.player().getUniqueId())) {
            markCloseRequested();
            return actionContext.close();
        }

        viewer.closeInventory();
        return true;
    }

    static void markMenuOpenRequested() {
        ActionExecutionState state = ACTION_EXECUTION_STATE.get();
        if (state != null) state.openRequested = true;
    }

    public static @NotNull InventoryCloseEvent.Reason mapCloseReason(@NotNull MenuCloseReason reason) {
        return switch (reason) {
            case CLIENT_CLOSE -> resolveInventoryCloseReason("PLAYER", InventoryCloseEvent.Reason.UNKNOWN);
            case REPLACED, REFRESH -> resolveInventoryCloseReason("OPEN_NEW", resolveInventoryCloseReason("PLUGIN", InventoryCloseEvent.Reason.UNKNOWN));
            case QUIT -> resolveInventoryCloseReason("DISCONNECT", resolveInventoryCloseReason("PLAYER", InventoryCloseEvent.Reason.UNKNOWN));
            case DEATH -> resolveInventoryCloseReason("DEATH", resolveInventoryCloseReason("PLAYER", InventoryCloseEvent.Reason.UNKNOWN));
            case TELEPORT -> resolveInventoryCloseReason("TELEPORT", resolveInventoryCloseReason("PLAYER", InventoryCloseEvent.Reason.UNKNOWN));
            case DISABLE -> resolveInventoryCloseReason("UNLOADED", resolveInventoryCloseReason("PLUGIN", InventoryCloseEvent.Reason.UNKNOWN));
            case CONDITION_FAILED, INVALID_CLICK, COMPLETED, UNKNOWN -> resolveInventoryCloseReason("PLUGIN", InventoryCloseEvent.Reason.UNKNOWN);
        };
    }

    public static void setup() {
        unregisterCommandPanelsMenus();

        MENUS.clear();
        COOLDOWNS.clear();
        SESSIONS.clear();
        COMMAND_PANELS_REGISTRATIONS.clear();
        COMMAND_PANELS_KEYS.clear();

        File menuFolder = new File(HMCCosmeticsPlugin.getInstance().getDataFolder() + "/menus");
        if (!menuFolder.exists()) menuFolder.mkdir();

        try (Stream<Path> walkStream = Files.walk(menuFolder.toPath())) {
            walkStream.filter(p -> p.toFile().isFile()).forEach(child -> {
                if (!child.toString().endsWith("yml") && !child.toString().endsWith("yaml")) return;

                MessagesUtil.sendDebugMessages("Scanning " + child);
                YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(child).build();
                CommentedConfigurationNode root;
                try {
                    root = loader.load();
                } catch (ConfigurateException e) {
                    throw new RuntimeException(e);
                }

                try {
                    new Menu(FilenameUtils.removeExtension(child.getFileName().toString()), root);
                } catch (Exception e) {
                    MessagesUtil.sendDebugMessages("Unable to create menu in " + child.getFileName().toString(), Level.WARNING);
                    if (Settings.isDebugMode()) e.printStackTrace();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan menu files", e);
        }

        setupCommandPanels();
    }

    private static void setupCommandPanels() {
        HMCCosmeticsPlugin plugin = HMCCosmeticsPlugin.getInstance();
        CommandPanelsApi api = Bukkit.getServicesManager().load(CommandPanelsApi.class);
        if (api == null) {
            throw new IllegalStateException("CommandPanels API service is unavailable. Ensure CommandPanels is installed and enabled.");
        }

        commandPanelsApi = api;
        commandPanelsNamespace = normalizeNamespace(plugin.getName());
        api.unregisterMenus(plugin);

        var actionRegistration = api.registerActionHandler(plugin, MENU_CLICK_ACTION_ID, Menus::handleMenuClickAction);
        if (!actionRegistration.successful()) {
            throw new IllegalStateException("Failed to register CommandPanels action handler '" + MENU_CLICK_ACTION_ID + "': " + actionRegistration.errors());
        }

        for (Menu menu : MENUS.values()) {
            MenuRegistration registration = api.registerMenu(
                    plugin,
                    MenuDefinitionSource.dynamic(menu.getId(), menu::buildCommandPanelsConfiguration),
                    MenuRegistrationOptions.defaults()
            );

            if (!registration.successful()) {
                MessagesUtil.sendDebugMessages("Failed to register CommandPanels menu '" + menu.getId() + "': " + registration.errors(), Level.WARNING);
                continue;
            }

            for (String warning : registration.warnings()) {
                MessagesUtil.sendDebugMessages("CommandPanels registration warning for '" + menu.getId() + "': " + warning, Level.WARNING);
            }

            menu.setCommandPanelsKey(registration.key());
            COMMAND_PANELS_REGISTRATIONS.put(menu.getId().toUpperCase(Locale.ROOT), registration);
            COMMAND_PANELS_KEYS.put(menu.getId().toUpperCase(Locale.ROOT), registration.key());
        }
    }

    private static void unregisterCommandPanelsMenus() {
        CommandPanelsApi api = commandPanelsApi;
        if (api != null) {
            try {
                api.unregisterMenus(HMCCosmeticsPlugin.getInstance());
            } catch (Exception ignored) {
            }
        }
    }

    private static ActionResult handleMenuClickAction(@NotNull MenuActionContext context, @NotNull String argument) {
        Player viewer = context.player();
        if (viewer == null) return ActionResult.failure("Viewer is unavailable");

        ParsedClickAction parsedClick = parseClickAction(argument);
        if (parsedClick == null) return ActionResult.failure("Unable to parse click action argument: " + argument);

        Menu menu = getMenu(context.session().key().menuId());
        if (menu == null) return ActionResult.failure("Unknown HMCC menu: " + context.session().key().menuId());

        MenuSession session = SESSIONS.get(viewer.getUniqueId());
        if (session == null || session.getMenu() != menu) {
            CosmeticUser user = CosmeticUsers.getUser(viewer);
            if (user == null) return ActionResult.failure("Cosmetic user is not loaded");
            session = new MenuSession(menu, user);
            setSession(viewer.getUniqueId(), session);
        }

        beginActionContext(context);
        try {
            boolean executed = menu.handleMenuClick(viewer, session, parsedClick.slot(), parsedClick.clickType());
            if (!executed) return ActionResult.success();

            MenuSession currentSession = SESSIONS.get(viewer.getUniqueId());
            if (currentSession != session) return ActionResult.success();
            if (wasCloseRequested() || wasRefreshRequested() || wasMenuOpenRequested()) return ActionResult.success();

            context.refresh();
            return ActionResult.success();
        } catch (Exception e) {
            MessagesUtil.sendDebugMessages("Failed to handle HMCC menu click: " + e.getMessage(), Level.WARNING);
            if (Settings.isDebugMode()) e.printStackTrace();
            return ActionResult.failure("Exception while handling click");
        } finally {
            endActionContext();
        }
    }

    private static void beginActionContext(@NotNull MenuActionContext context) {
        ACTION_CONTEXT.set(context);
        ACTION_EXECUTION_STATE.set(new ActionExecutionState());
    }

    private static void endActionContext() {
        ACTION_CONTEXT.remove();
        ACTION_EXECUTION_STATE.remove();
    }

    private static void markRefreshRequested() {
        ActionExecutionState state = ACTION_EXECUTION_STATE.get();
        if (state != null) state.refreshRequested = true;
    }

    private static void markCloseRequested() {
        ActionExecutionState state = ACTION_EXECUTION_STATE.get();
        if (state != null) state.closeRequested = true;
    }

    private static boolean wasRefreshRequested() {
        ActionExecutionState state = ACTION_EXECUTION_STATE.get();
        return state != null && state.refreshRequested;
    }

    private static boolean wasCloseRequested() {
        ActionExecutionState state = ACTION_EXECUTION_STATE.get();
        return state != null && state.closeRequested;
    }

    private static boolean wasMenuOpenRequested() {
        ActionExecutionState state = ACTION_EXECUTION_STATE.get();
        return state != null && state.openRequested;
    }

    @Nullable
    private static ParsedClickAction parseClickAction(@NotNull String argument) {
        if (argument.isBlank()) return null;

        int slot = -1;
        ClickType clickType = ClickType.LEFT;

        String[] tokens = argument.trim().split("\\s+");
        for (String token : tokens) {
            if (token.isBlank()) continue;

            String key = null;
            String value = null;
            if (token.contains("=")) {
                String[] split = token.split("=", 2);
                key = split[0].toLowerCase(Locale.ROOT);
                value = split.length > 1 ? split[1] : "";
            }

            if ("slot".equals(key)) {
                try {
                    slot = Integer.parseInt(value);
                } catch (NumberFormatException ignored) {
                    return null;
                }
                continue;
            }

            if ("click".equals(key)) {
                try {
                    clickType = ClickType.valueOf(value.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
                continue;
            }

            if (slot == -1) {
                try {
                    slot = Integer.parseInt(token);
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }

            try {
                clickType = ClickType.valueOf(token.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        if (slot < 0) return null;
        return new ParsedClickAction(slot, clickType);
    }

    @NotNull
    private static InventoryCloseEvent.Reason resolveInventoryCloseReason(@NotNull String name, @NotNull InventoryCloseEvent.Reason fallback) {
        try {
            return InventoryCloseEvent.Reason.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    @NotNull
    private static String normalizeNamespace(@NotNull String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "-");
    }

    private record ParsedClickAction(int slot, @NotNull ClickType clickType) {
    }

    private static final class ActionExecutionState {
        private boolean refreshRequested;
        private boolean closeRequested;
        private boolean openRequested;
    }
}
