package com.hibiscusmc.hmccosmetics.gui;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.Settings;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import gg.masivo.masivogui.api.ActionRegistration;
import gg.masivo.masivogui.api.ActionResult;
import gg.masivo.masivogui.api.ListProviderRegistration;
import gg.masivo.masivogui.api.MasivoGUIApi;
import gg.masivo.masivogui.api.MenuActionContext;
import gg.masivo.masivogui.api.MenuArguments;
import gg.masivo.masivogui.api.MenuCloseReason;
import gg.masivo.masivogui.api.MenuDefinitionSource;
import gg.masivo.masivogui.api.MenuKey;
import gg.masivo.masivogui.api.MenuListContext;
import gg.masivo.masivogui.api.MenuListItemContext;
import gg.masivo.masivogui.api.MenuRegistration;
import gg.masivo.masivogui.api.MenuRegistrationOptions;
import me.lojosho.shaded.configurate.CommentedConfigurationNode;
import me.lojosho.shaded.configurate.ConfigurateException;
import me.lojosho.shaded.configurate.yaml.YamlConfigurationLoader;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
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
    private static final String COSMETICS_LIST_PROVIDER_ID = "cosmetics";
    private static final String COSMETICS_PAGE_KEY = "hmcc_page";

    private static final HashMap<String, Menu> MENUS = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final Map<UUID, MenuSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<String, MenuRegistration> MASIVO_GUI_REGISTRATIONS = new ConcurrentHashMap<>();
    private static final Map<String, MenuKey> MASIVO_GUI_KEYS = new ConcurrentHashMap<>();
    private static final ThreadLocal<MenuActionContext> ACTION_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<ActionExecutionState> ACTION_EXECUTION_STATE = new ThreadLocal<>();

    @Nullable
    private static volatile MasivoGUIApi masivoGUIApi;
    private static volatile String masivoGUINamespace = "hmccosmetics";
    @Nullable
    private static volatile ActionRegistration menuClickActionRegistration;
    @Nullable
    private static volatile ListProviderRegistration cosmeticsListProviderRegistration;

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
    public static MasivoGUIApi getMasivoGUIApi() {
        return masivoGUIApi;
    }

    @NotNull
    public static String getMasivoGUINamespace() {
        return masivoGUINamespace;
    }

    @NotNull
    public static String getMenuClickActionId() {
        return MENU_CLICK_ACTION_ID;
    }

    @NotNull
    public static String getCosmeticsListProviderId() {
        return COSMETICS_LIST_PROVIDER_ID;
    }

    @NotNull
    public static String getCosmeticsPageKey() {
        return COSMETICS_PAGE_KEY;
    }

    @Nullable
    public static MenuKey getMenuKey(@NotNull Menu menu) {
        return MASIVO_GUI_KEYS.get(menu.getId().toUpperCase(Locale.ROOT));
    }

    public static boolean refresh(@NotNull Player viewer, @NotNull MenuSession session) {
        if (SESSIONS.get(viewer.getUniqueId()) != session) return false;

        MenuActionContext actionContext = ACTION_CONTEXT.get();
        if (actionContext != null && actionContext.player() != null
                && viewer.getUniqueId().equals(actionContext.player().getUniqueId())) {
            actionContext.setSessionValue(COSMETICS_PAGE_KEY, String.valueOf(session.getPage()));
            boolean refreshed = actionContext.refresh();
            if (refreshed) {
                markRefreshRequested();
            }
            return refreshed;
        }

        MasivoGUIApi api = masivoGUIApi;
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
        unregisterMasivoGUIMenus();

        MENUS.clear();
        COOLDOWNS.clear();
        SESSIONS.clear();
        MASIVO_GUI_REGISTRATIONS.clear();
        MASIVO_GUI_KEYS.clear();

        File menuFolder = new File(HMCCosmeticsPlugin.getInstance().getDataFolder() + "/menus");
        if (!menuFolder.exists()) menuFolder.mkdir();

        try (Stream<Path> walkStream = Files.walk(menuFolder.toPath())) {
            walkStream.filter(p -> p.toFile().isFile()).forEach(child -> {
                if (!child.toString().endsWith("yml") && !child.toString().endsWith("yaml")) return;
                if (isFunctionalMenuFile(menuFolder.toPath(), child)) return;

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

        setupMasivoGUI();
    }

    private static boolean isFunctionalMenuFile(@NotNull Path menuFolder, @NotNull Path child) {
        Path relative = menuFolder.relativize(child);
        return relative.getNameCount() > 1 && "functional".equalsIgnoreCase(relative.getName(0).toString());
    }

    private static void setupMasivoGUI() {
        HMCCosmeticsPlugin plugin = HMCCosmeticsPlugin.getInstance();
        MasivoGUIApi api = Bukkit.getServicesManager().load(MasivoGUIApi.class);
        if (api == null) {
            throw new IllegalStateException("MasivoGUI API service is unavailable. Ensure MasivoGUI is installed and enabled.");
        }

        masivoGUIApi = api;
        masivoGUINamespace = normalizeNamespace(plugin.getName());
        api.unregisterMenus(plugin);

        menuClickActionRegistration = api.registerActionHandler(plugin, MENU_CLICK_ACTION_ID, Menus::handleMenuClickAction);
        if (!menuClickActionRegistration.successful()) {
            throw new IllegalStateException("Failed to register MasivoGUI action handler '" + MENU_CLICK_ACTION_ID + "': " + menuClickActionRegistration.errors());
        }

        cosmeticsListProviderRegistration = api.registerListProvider(plugin, COSMETICS_LIST_PROVIDER_ID, new gg.masivo.masivogui.api.MenuListProvider() {
            @Override
            public int size(MenuListContext context) {
                Player player = context.player();
                if (player == null) return 0;

                Menu menu = getMenu(context.session().key().menuId());
                if (menu == null) return 0;
                return menu.getVisibleCosmeticItemCount(player);
            }

            @Override
            public ItemStack create(MenuListItemContext context) {
                Player player = context.player();
                if (player == null) return null;

                Menu menu = getMenu(context.session().key().menuId());
                if (menu == null) return null;

                MenuSession session = SESSIONS.get(player.getUniqueId());
                if (session == null || session.getMenu() != menu) {
                    CosmeticUser user = CosmeticUsers.getUser(player);
                    if (user == null) return null;
                    session = new MenuSession(menu, user);
                    setSession(player.getUniqueId(), session);
                }

                session.setPage(context.page(), session.getTotalPages(player));
                return menu.createCosmeticListItem(player, session.getCosmeticHolder(), context.absoluteIndex(), context.slot());
            }
        });
        if (!cosmeticsListProviderRegistration.successful()) {
            throw new IllegalStateException("Failed to register MasivoGUI list provider '" + COSMETICS_LIST_PROVIDER_ID + "': " + cosmeticsListProviderRegistration.errors());
        }

        for (Menu menu : MENUS.values()) {
            MenuRegistration registration = api.registerMenu(
                    plugin,
                    MenuDefinitionSource.dynamic(menu.getId(), menu::buildMasivoGUIConfiguration),
                    MenuRegistrationOptions.defaults()
            );

            if (!registration.successful()) {
                MessagesUtil.sendDebugMessages("Failed to register MasivoGUI menu '" + menu.getId() + "': " + registration.errors(), Level.WARNING);
                continue;
            }

            for (String warning : registration.warnings()) {
                MessagesUtil.sendDebugMessages("MasivoGUI registration warning for '" + menu.getId() + "': " + warning, Level.WARNING);
            }

            menu.setMasivoGUIKey(registration.key());
            MASIVO_GUI_REGISTRATIONS.put(menu.getId().toUpperCase(Locale.ROOT), registration);
            MASIVO_GUI_KEYS.put(menu.getId().toUpperCase(Locale.ROOT), registration.key());
        }
    }

    private static void unregisterMasivoGUIMenus() {
        if (menuClickActionRegistration != null) {
            try {
                menuClickActionRegistration.unregister();
            } catch (Exception ignored) {
            }
        }
        if (cosmeticsListProviderRegistration != null) {
            try {
                cosmeticsListProviderRegistration.unregister();
            } catch (Exception ignored) {
            }
        }

        MasivoGUIApi api = masivoGUIApi;
        if (api != null) {
            try {
                api.unregisterMenus(HMCCosmeticsPlugin.getInstance());
            } catch (Exception ignored) {
            }
        }
        menuClickActionRegistration = null;
        cosmeticsListProviderRegistration = null;
    }

    private static ActionResult handleMenuClickAction(@NotNull MenuActionContext context, @NotNull MenuArguments arguments) {
        Player viewer = context.player();
        if (viewer == null) return ActionResult.failure("Viewer is unavailable");

        ParsedClickAction parsedClick = parseClickAction(arguments);
        if (parsedClick == null) return ActionResult.failure("Unable to parse click action argument: " + context.argument());

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
    private static ParsedClickAction parseClickAction(@NotNull MenuArguments arguments) {
        int slot = arguments.optionInt("slot", arguments.positionalInt(0, -1));
        if (slot < 0) return null;

        String rawClick = arguments.optionOrDefault("click", arguments.positionalOrDefault(1, ClickType.LEFT.name()));
        try {
            return new ParsedClickAction(slot, ClickType.valueOf(rawClick.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
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
