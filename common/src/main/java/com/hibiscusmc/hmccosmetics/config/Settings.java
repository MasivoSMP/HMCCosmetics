package com.hibiscusmc.hmccosmetics.config;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.section.SlotOptionConfig;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import com.hibiscusmc.hmccosmetics.util.search.PlayerSearchManager;
import lombok.Getter;
import lombok.Setter;
import me.lojosho.shaded.configurate.ConfigurationNode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public class Settings {

    // General Settings
    private static final String DEFAULT_MENU = "default-menu";
    private static final String CONFIG_VERSION = "config-version";
    private static final String COSMETIC_SETTINGS_PATH = "cosmetic-settings";
    private static final String BALLOON_OFFSET = "balloon-offset";
    private static final String BALLOON_PHYSICS = "balloon-physics";
    private static final String BALLOON_PHYSICS_SETTINGS = "balloon-physics-settings";
    private static final String BALLOON_PHYSICS_HAND_OFFSET = "hand-offset";
    private static final String BALLOON_PHYSICS_BUOYANCY = "buoyancy";
    private static final String BALLOON_PHYSICS_STRING_STIFFNESS = "string-stiffness";
    private static final String BALLOON_PHYSICS_LINEAR_DAMPING = "linear-damping-per-second";
    private static final String BALLOON_PHYSICS_PITCH_ATTACHMENT_FORWARD_OFFSET = "pitch-attachment-forward-offset";
    private static final String BALLOON_PHYSICS_PITCH_ATTACHMENT_DOWN_OFFSET = "pitch-attachment-down-offset";
    private static final String BALLOON_PHYSICS_PITCH_TORQUE_SCALE = "pitch-torque-scale";
    private static final String BALLOON_PHYSICS_PITCH_RESTORE_STIFFNESS = "pitch-restore-stiffness";
    private static final String BALLOON_PHYSICS_PITCH_DAMPING = "pitch-damping-per-second";
    private static final String BALLOON_PHYSICS_MAX_PITCH_DEGREES = "max-pitch-degrees";
    private static final String BACKPACK_OFFSET = "backpack-offset";
    private static final String VIEW_DISTANCE_PATH = "view-distance";
    private static final String DYE_MENU_PATH = "dye-menu";
    private static final String DYE_MENU_NAME = "title";
    private static final String DYE_MENU_INPUT_SLOT = "input-slot";
    private static final String DYE_MENU_OUTPUT_SLOT = "output-slot";
    private static final String DEBUG_ENABLE_PETH = "debug-mode";
    private static final String TICK_PERIOD_PATH = "tick-period";
    private static final String UNAPPLY_DEATH_PATH = "unapply-on-death";
    private static final String FORCE_PERMISSION_JOIN_PATH = "force-permission-join";
    private static final String FORCE_SHOW_COSMETICS_PATH = "force-show-join";
    private static final String ITEM_PROCESSING_PATH = "item-processing";
    private static final String ITEM_PROCESS_DISPLAY_NAME_PATH = "display-name";
    private static final String ITEM_PROCESS_LORE_PATH = "lore";
    private static final String DISABLED_GAMEMODE_PATH = "disabled-gamemode";
    private static final String DISABLED_GAMEMODE_GAMEMODES_PATH = "gamemodes";
    private static final String HOOK_SETTING_PATH = "hook-settings";
    private static final String HOOK_ITEMADDER_PATH = "itemsadder";
    private static final String HOOK_NEXO_PATH = "nexo";
    private static final String HOOK_RELOAD_CHANGE_PATH = "reload-on-change";
    private static final String HOOK_WORLDGUARD_PATH = "worldguard";
    private static final String HOOK_WG_MOVE_CHECK_PATH = "player-move-check";
    private static final String HOOK_WG_MOVE_CHECK_PATH_LEGACY = "player_move_check";
    private static final String COSMETIC_DISABLED_WORLDS_PATH = "disabled-worlds";
    private static final String COSMETIC_BACKPACK_FORCE_RIDING_PACKET_PATH = "backpack-force-riding-packet";
    private static final String COSMETIC_DESTROY_LOOSE_COSMETIC_PATH = "destroy-loose-cosmetics";
    private static final String COSMETIC_BALLOON_HEAD_FORWARD_PATH = "balloon-head-forward";
    private static final String COSMETIC_OFFHAND_PREVENT_SWAPPING = "offhand-prevent-swapping";
    private static final String COSMETIC_TOGGLE_SOUNDS_PATH = "toggle-sounds";
    private static final String COSMETIC_TOGGLE_SOUNDS_ON_PATH = "on-sound";
    private static final String COSMETIC_TOGGLE_SOUNDS_OFF_PATH = "off-sound";
    private static final String COSMETIC_TOGGLE_SOUNDS_ON_PATH_LEGACY = "on";
    private static final String COSMETIC_TOGGLE_SOUNDS_OFF_PATH_LEGACY = "off";
    private static final String COSMETIC_TOGGLE_SOUNDS_SOUND_PATH = "sound";
    private static final String COSMETIC_TOGGLE_SOUNDS_VOLUME_PATH = "volume";
    private static final String COSMETIC_TOGGLE_SOUNDS_PITCH_PATH = "pitch";
    private static final String MENU_SETTINGS_PATH = "menu-settings";
    private static final String MENU_CLICK_COOLDOWN_PATH = "click-cooldown";
    private static final String MENU_CLICK_COOLDOWN_TIME_PATH = "time";
    private static final String PURCHASE_LORE_PATH = "purchase-lore";
    private static final String COSMETIC_LORE_PATH = "cosmetic-lore";
    private static final String COSMETIC_LORE_LINES_PATH = "lines";
    private static final String COSMETIC_LORE_ALLOWED_WITH_MAPPINGS_PATH = "allowed-with-mappings";
    private static final String COSMETIC_TYPE_SETTINGS_PATH = "cosmetic-type";
    private static final String EQUIP_CLICK_TYPE = "equip-click";
    private static final String UNEQUIP_CLICK_TYPE = "unequip-click";
    private static final String DYE_CLICK_TYPE = "dye-click";
    private static final String SHADING_PATH = "shading";
    private static final String FIRST_ROW_SHIFT_PATH = "first-row-shift";
    private static final String SEQUENT_ROW_SHIFT_PATH = "sequent-row-shift";
    private static final String INDIVIDUAL_COLUMN_SHIFT_PATH = "individual-column-shift";
    private static final String BACKGROUND_PATH = "background";
    private static final String CLEAR_BACKGROUND_PATH = "clear-background";
    private static final String EQUIPPED_COSMETIC_COLOR_PATH = "equipped-cosmetic-color";
    private static final String EQUIPABLE_COSMETIC_COLOR_PATH = "equipable-cosmetic-color";
    private static final String LOCKED_COSMETIC_COLOR_PATH = "locked-cosmetic-color";
    private static final String ENABLED_PATH = "enabled";
    private static final String SLOT_OPTIONS_PATH = "slot-options";
    private static final String BACKPACK_PREVENT_DARKNESS_PATH = "backpack-prevent-darkness";
    private static final String BETTER_HUD_PATH = "betterhud";
    private static final String BETTER_HUD_HIDE_IN_WARDROBE_PATH = "wardrobe-hide";
    private static final String PLAYER_SEARCH_IMPLEMENTATION = "player-search-implmentation";

    @Getter
    private static String defaultMenu;
    @Getter
    private static String dyeMenuName;
    @Getter
    private static int dyeMenuInputSlot;
    @Getter
    private static int dyeMenuOutputSlot;
    @Getter
    private static int configVersion;
    @Getter
    private static boolean debugMode;
    @Getter
    private static boolean unapplyOnDeath;
    @Getter
    private static boolean forcePermissionJoin;
    @Getter
    private static boolean forceShowOnJoin;
    @Getter
    private static boolean itemProcessingDisplayName;
    @Getter
    private static boolean itemProcessingLore;
    @Getter
    private static boolean itemsAdderChangeReload;
    @Getter
    private static boolean nexoChangeReload;
    @Getter
    private static boolean worldGuardMoveCheck;
    private static final HashMap<EquipmentSlot, SlotOptionConfig> slotOptions = new HashMap<>();
    @Getter
    private static boolean destroyLooseCosmetics;
    @Getter
    private static boolean preventOffhandSwapping;
    @Getter
    private static boolean backpackForceRidingEnabled;
    @Getter
    private static boolean toggleSoundsEnabled;
    @Getter
    private static boolean disabledGamemodesEnabled;
    @Getter
    private static boolean balloonHeadForward;
    @Getter
    private static boolean balloonPhysics;
    @Getter
    private static boolean backpackPreventDarkness;
    @Getter
    private static List<String> disabledGamemodes;
    @Getter
    private static List<String> disabledWorlds;
    @Getter
    private static int viewDistance;
    @Getter
    private static int tickPeriod;
    @Getter
    private static Long defaultMenuCooldown;
    @Getter
    private static boolean menuClickCooldown;
    @Getter
    private static List<String> purchaseLore;
    @Getter
    private static boolean cosmeticLoreEnabled;
    @Getter
    private static List<String> cosmeticLoreLines;
    @Getter
    private static Map<String, String> cosmeticLoreAllowedWithMappings;
    @Getter
    private static Vector balloonOffset;
    @Getter
    private static Vector balloonPhysicsHandOffset;
    @Getter
    private static double balloonPhysicsBuoyancy;
    @Getter
    private static double balloonPhysicsStringStiffness;
    @Getter
    private static double balloonPhysicsLinearDampingPerSecond;
    @Getter
    private static double balloonPhysicsPitchAttachmentForwardOffset;
    @Getter
    private static double balloonPhysicsPitchAttachmentDownOffset;
    @Getter
    private static double balloonPhysicsPitchTorqueScale;
    @Getter
    private static double balloonPhysicsPitchRestoreStiffness;
    @Getter
    private static double balloonPhysicsPitchDampingPerSecond;
    @Getter
    private static double balloonPhysicsMaxPitchDegrees;
    @Getter
    private static Vector backpackOffset;
    @Getter
    private static String cosmeticEquipClickType;
    @Getter
    private static String cosmeticUnEquipClickType;
    @Getter
    private static String cosmeticDyeClickType;
    @Getter
    private static boolean defaultShading;
    @Getter
    private static String firstRowShift;
    @Getter
    private static String sequentRowShift;
    @Getter
    private static String individualColumnShift;
    @Getter
    private static String background;
    @Getter
    private static String clearBackground;
    @Getter
    private static String equippedCosmeticColor;
    @Getter
    private static String equipableCosmeticColor;
    @Getter
    private static String lockedCosmeticColor;
    @Getter @Setter
    private static boolean allPlayersHidden;
    @Getter
    private static boolean wardrobeHideHud;
    @Getter
    private static String toggleOnSound;
    @Getter
    private static float toggleOnSoundVolume;
    @Getter
    private static float toggleOnSoundPitch;
    @Getter
    private static String toggleOffSound;
    @Getter
    private static float toggleOffSoundVolume;
    @Getter
    private static float toggleOffSoundPitch;
    @Getter
    private static PlayerSearchManager.SearchEngine engine;


    public static void load(ConfigurationNode source) {

        debugMode = source.node(DEBUG_ENABLE_PETH).getBoolean(false);
        defaultMenu = source.node(DEFAULT_MENU).getString();
        configVersion = source.node(CONFIG_VERSION).getInt(0);
        if (configVersion == 0) {
            HMCCosmeticsPlugin plugin = HMCCosmeticsPlugin.getInstance();
            plugin.getLogger().severe("");
            plugin.getLogger().severe("");
            plugin.getLogger().severe("Improper Configuration Found (Config Version Does Not Exist!)");
            plugin.getLogger().severe("Problems will happen with the plugin! Delete and regenerate a new one!");
            plugin.getLogger().severe("");
            plugin.getLogger().severe("");
        }

        ConfigurationNode cosmeticSettings = source.node(COSMETIC_SETTINGS_PATH);

        ConfigurationNode disabledGamemodeSettings = cosmeticSettings.node(DISABLED_GAMEMODE_PATH);
        disabledGamemodesEnabled = disabledGamemodeSettings.node(ENABLED_PATH).getBoolean(true);
        try {
            disabledGamemodes = disabledGamemodeSettings.node(DISABLED_GAMEMODE_GAMEMODES_PATH).getList(String.class);
            disabledWorlds = cosmeticSettings.node(COSMETIC_DISABLED_WORLDS_PATH).getList(String.class);
        } catch (Exception e) {
            disabledGamemodes = new ArrayList<>();
            disabledWorlds = new ArrayList<>();
        }

        ConfigurationNode itemProcessingSettings = cosmeticSettings.node(ITEM_PROCESSING_PATH);
        itemProcessingDisplayName = itemProcessingSettings.node(ITEM_PROCESS_DISPLAY_NAME_PATH).getBoolean(true);
        itemProcessingLore = itemProcessingSettings.node(ITEM_PROCESS_LORE_PATH).getBoolean(true);

        unapplyOnDeath = cosmeticSettings.node(UNAPPLY_DEATH_PATH).getBoolean(false);
        forcePermissionJoin = cosmeticSettings.node(FORCE_PERMISSION_JOIN_PATH).getBoolean(false);
        forceShowOnJoin = cosmeticSettings.node(FORCE_SHOW_COSMETICS_PATH).getBoolean(false);
        destroyLooseCosmetics = cosmeticSettings.node(COSMETIC_DESTROY_LOOSE_COSMETIC_PATH).getBoolean(false);
        backpackForceRidingEnabled = cosmeticSettings.node(COSMETIC_BACKPACK_FORCE_RIDING_PACKET_PATH).getBoolean(false);
        preventOffhandSwapping = cosmeticSettings.node(COSMETIC_OFFHAND_PREVENT_SWAPPING).getBoolean(false);
        ConfigurationNode toggleSoundsSettings = cosmeticSettings.node(COSMETIC_TOGGLE_SOUNDS_PATH);
        toggleSoundsEnabled = toggleSoundsSettings.node(ENABLED_PATH).getBoolean(false);
        ConfigurationNode toggleOnSettings = getToggleSoundSettingsNode(toggleSoundsSettings, COSMETIC_TOGGLE_SOUNDS_ON_PATH, COSMETIC_TOGGLE_SOUNDS_ON_PATH_LEGACY, true);
        toggleOnSound = toggleOnSettings.node(COSMETIC_TOGGLE_SOUNDS_SOUND_PATH).getString("minecraft:block.note_block.pling");
        toggleOnSoundVolume = (float) toggleOnSettings.node(COSMETIC_TOGGLE_SOUNDS_VOLUME_PATH).getDouble(1.0D);
        toggleOnSoundPitch = (float) toggleOnSettings.node(COSMETIC_TOGGLE_SOUNDS_PITCH_PATH).getDouble(1.2D);
        ConfigurationNode toggleOffSettings = getToggleSoundSettingsNode(toggleSoundsSettings, COSMETIC_TOGGLE_SOUNDS_OFF_PATH, COSMETIC_TOGGLE_SOUNDS_OFF_PATH_LEGACY, false);
        toggleOffSound = toggleOffSettings.node(COSMETIC_TOGGLE_SOUNDS_SOUND_PATH).getString("minecraft:block.note_block.bass");
        toggleOffSoundVolume = (float) toggleOffSettings.node(COSMETIC_TOGGLE_SOUNDS_VOLUME_PATH).getDouble(1.0D);
        toggleOffSoundPitch = (float) toggleOffSettings.node(COSMETIC_TOGGLE_SOUNDS_PITCH_PATH).getDouble(0.8D);
        MessagesUtil.sendDebugMessages("Toggle sounds loaded: enabled=" + toggleSoundsEnabled
                + ", on=" + toggleOnSound + " (" + toggleOnSoundVolume + ", " + toggleOnSoundPitch + ")"
                + ", off=" + toggleOffSound + " (" + toggleOffSoundVolume + ", " + toggleOffSoundPitch + ")");

        cosmeticSettings.node(SLOT_OPTIONS_PATH).childrenMap().forEach((key, value) -> {
            EquipmentSlot slot = convertConfigToEquipment(key.toString().toLowerCase());
            if (slot == null) {
                MessagesUtil.sendDebugMessages("Invalid slot option: " + key, Level.WARNING);
                return;
            }
            boolean addEnchantments = value.node("add-enchantments").getBoolean(false);
            boolean requireEmpty = value.node("require-empty").getBoolean(false);
            slotOptions.put(slot, new SlotOptionConfig(slot, addEnchantments, requireEmpty));
        });

        tickPeriod = cosmeticSettings.node(TICK_PERIOD_PATH).getInt(-1);
        engine = PlayerSearchManager.SearchEngine.valueOf(cosmeticSettings.node(PLAYER_SEARCH_IMPLEMENTATION).getString("BUKKIT").toUpperCase());
        viewDistance = cosmeticSettings.node(VIEW_DISTANCE_PATH).getInt(-3);
        balloonHeadForward = cosmeticSettings.node(COSMETIC_BALLOON_HEAD_FORWARD_PATH).getBoolean(false);
        balloonPhysics = cosmeticSettings.node(BALLOON_PHYSICS).getBoolean(false);
        backpackPreventDarkness = cosmeticSettings.node(BACKPACK_PREVENT_DARKNESS_PATH).getBoolean(true);
        final var balloonPhysicsSettings = cosmeticSettings.node(BALLOON_PHYSICS_SETTINGS);
        if (balloonPhysicsSettings.node(BALLOON_PHYSICS_HAND_OFFSET).virtual()) {
            balloonPhysicsHandOffset = new Vector(0.35D, 1.20D, 0.18D);
        } else {
            balloonPhysicsHandOffset = loadVector(balloonPhysicsSettings.node(BALLOON_PHYSICS_HAND_OFFSET));
        }
        balloonPhysicsBuoyancy = balloonPhysicsSettings.node(BALLOON_PHYSICS_BUOYANCY).getDouble(2.35D);
        balloonPhysicsStringStiffness = balloonPhysicsSettings.node(BALLOON_PHYSICS_STRING_STIFFNESS).getDouble(10.0D);
        balloonPhysicsLinearDampingPerSecond = balloonPhysicsSettings.node(BALLOON_PHYSICS_LINEAR_DAMPING).getDouble(4.5D);
        balloonPhysicsPitchAttachmentForwardOffset = balloonPhysicsSettings.node(BALLOON_PHYSICS_PITCH_ATTACHMENT_FORWARD_OFFSET).getDouble(0.22D);
        balloonPhysicsPitchAttachmentDownOffset = balloonPhysicsSettings.node(BALLOON_PHYSICS_PITCH_ATTACHMENT_DOWN_OFFSET).getDouble(0.82D);
        balloonPhysicsPitchTorqueScale = balloonPhysicsSettings.node(BALLOON_PHYSICS_PITCH_TORQUE_SCALE).getDouble(1.55D);
        balloonPhysicsPitchRestoreStiffness = balloonPhysicsSettings.node(BALLOON_PHYSICS_PITCH_RESTORE_STIFFNESS).getDouble(52.0D);
        balloonPhysicsPitchDampingPerSecond = balloonPhysicsSettings.node(BALLOON_PHYSICS_PITCH_DAMPING).getDouble(9.5D);
        balloonPhysicsMaxPitchDegrees = balloonPhysicsSettings.node(BALLOON_PHYSICS_MAX_PITCH_DEGREES).getDouble(32.0D);

        ConfigurationNode menuSettings = source.node(MENU_SETTINGS_PATH);

        ConfigurationNode clickCooldownSettings = menuSettings.node(MENU_CLICK_COOLDOWN_PATH);
        menuClickCooldown = clickCooldownSettings.node(ENABLED_PATH).getBoolean(true);
        defaultMenuCooldown = clickCooldownSettings.node(MENU_CLICK_COOLDOWN_TIME_PATH).getLong(1000L);
        try {
            purchaseLore = menuSettings.node(PURCHASE_LORE_PATH).getList(String.class);
        } catch (Exception e) {
            purchaseLore = new ArrayList<>();
        }
        ConfigurationNode cosmeticLoreSettings = menuSettings.node(COSMETIC_LORE_PATH);
        cosmeticLoreEnabled = cosmeticLoreSettings.node(ENABLED_PATH).getBoolean(false);
        try {
            cosmeticLoreLines = cosmeticLoreSettings.node(COSMETIC_LORE_LINES_PATH).getList(String.class);
        } catch (Exception e) {
            cosmeticLoreLines = new ArrayList<>();
        }
        cosmeticLoreAllowedWithMappings = new HashMap<>();
        ConfigurationNode allowedWithMappings = cosmeticLoreSettings.node(COSMETIC_LORE_ALLOWED_WITH_MAPPINGS_PATH);
        allowedWithMappings.childrenMap().forEach((key, value) -> {
            String mappingKey = key.toString();
            String mappingValue = value.getString();
            if (mappingKey == null || mappingKey.isBlank() || mappingValue == null || mappingValue.isBlank()) return;
            cosmeticLoreAllowedWithMappings.put(mappingKey.toLowerCase(Locale.ROOT), mappingValue);
        });

        ConfigurationNode shadingSettings = menuSettings.node(SHADING_PATH);
        defaultShading = shadingSettings.node(ENABLED_PATH).getBoolean();
        firstRowShift = shadingSettings.node(FIRST_ROW_SHIFT_PATH).getString();
        sequentRowShift = shadingSettings.node(SEQUENT_ROW_SHIFT_PATH).getString();
        individualColumnShift = shadingSettings.node(INDIVIDUAL_COLUMN_SHIFT_PATH).getString();
        background = shadingSettings.node(BACKGROUND_PATH).getString();
        clearBackground = shadingSettings.node(CLEAR_BACKGROUND_PATH).getString();
        equippedCosmeticColor = shadingSettings.node(EQUIPPED_COSMETIC_COLOR_PATH).getString();
        equipableCosmeticColor = shadingSettings.node(EQUIPABLE_COSMETIC_COLOR_PATH).getString();
        lockedCosmeticColor = shadingSettings.node(LOCKED_COSMETIC_COLOR_PATH).getString();

        ConfigurationNode cosmeticTypeSettings = menuSettings.node(COSMETIC_TYPE_SETTINGS_PATH);
        cosmeticEquipClickType = cosmeticTypeSettings.node(EQUIP_CLICK_TYPE).getString("ANY");
        cosmeticUnEquipClickType = cosmeticTypeSettings.node(UNEQUIP_CLICK_TYPE).getString("ANY");
        cosmeticDyeClickType = cosmeticTypeSettings.node(DYE_CLICK_TYPE).getString("ANY");

        final var balloonSection = cosmeticSettings.node(BALLOON_OFFSET);
        balloonOffset = loadVector(balloonSection);

        final var backpackSection = cosmeticSettings.node(BACKPACK_OFFSET);
        if (backpackSection.virtual()) {
            backpackOffset = new Vector(0, -2, 0);
        } else {
            backpackOffset = loadVector(backpackSection);
        }

        ConfigurationNode dyeMenuSettings = source.node(DYE_MENU_PATH);

        dyeMenuName = dyeMenuSettings.node(DYE_MENU_NAME).getString("Dye Menu");
        dyeMenuInputSlot = dyeMenuSettings.node(DYE_MENU_INPUT_SLOT).getInt(19);
        dyeMenuOutputSlot = dyeMenuSettings.node(DYE_MENU_OUTPUT_SLOT).getInt(25);

        ConfigurationNode hookSettings = source.node(HOOK_SETTING_PATH);

        ConfigurationNode itemsAdderSettings = hookSettings.node(HOOK_ITEMADDER_PATH);
        itemsAdderChangeReload = itemsAdderSettings.node(HOOK_RELOAD_CHANGE_PATH).getBoolean(false);

        ConfigurationNode nexoSettings = hookSettings.node(HOOK_NEXO_PATH);
        nexoChangeReload = nexoSettings.node(HOOK_RELOAD_CHANGE_PATH).getBoolean(true);

        ConfigurationNode betterHudSettings = hookSettings.node(BETTER_HUD_PATH);
        wardrobeHideHud = betterHudSettings.node(BETTER_HUD_HIDE_IN_WARDROBE_PATH).getBoolean(true);

        ConfigurationNode worldGuardSettings = hookSettings.node(HOOK_WORLDGUARD_PATH);
        worldGuardMoveCheck = worldGuardSettings.node(HOOK_WG_MOVE_CHECK_PATH).getBoolean(true);
        // I messed up in release 2.2.6 and forgot to change player_move_check to player-move-check.
        if (!worldGuardSettings.node(HOOK_WG_MOVE_CHECK_PATH_LEGACY).virtual()) {
            MessagesUtil.sendDebugMessages("There is a deprecated way of using WG hook setting. Change player_move_check to player-move-check in your configuration to prevent issues in the future. ", Level.WARNING);
            worldGuardMoveCheck = worldGuardSettings.node(HOOK_WG_MOVE_CHECK_PATH_LEGACY).getBoolean(true);
        }
    }

    public static Vector loadVector(final ConfigurationNode config) {
        return new Vector(config.node("x").getDouble(), config.node("y").getDouble(), config.node("z").getDouble());
    }

    public static boolean isBackpackOffsetEnabled() {
        return backpackOffset != null && backpackOffset.lengthSquared() > 0.000001;
    }

    public static SlotOptionConfig getSlotOption(EquipmentSlot slot) {
        if (!slotOptions.containsKey(slot)) slotOptions.put(slot, new SlotOptionConfig(slot, false, false));
        return slotOptions.get(slot);
    }

    public static void setDebugMode(boolean newSetting) {
        debugMode = newSetting;

        HMCCosmeticsPlugin plugin = HMCCosmeticsPlugin.getInstance();

        plugin.getConfig().set("debug-mode", newSetting);

        plugin.saveConfig();
    }

    public static void playCosmeticToggleSound(Player player, boolean enabled) {
        if (player == null || !toggleSoundsEnabled) return;
        String sound = enabled ? toggleOnSound : toggleOffSound;
        if (sound == null || sound.isBlank()) return;

        float volume = enabled ? toggleOnSoundVolume : toggleOffSoundVolume;
        float pitch = enabled ? toggleOnSoundPitch : toggleOffSoundPitch;

        try {
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            MessagesUtil.sendDebugMessages("Unable to play toggle sound '" + sound + "'.", Level.WARNING);
        }
    }

    public static @NotNull String resolveAllowedWithDisplay(@NotNull String value) {
        if (value.isBlank()) return value;
        String mapped = cosmeticLoreAllowedWithMappings.get(value.toLowerCase(Locale.ROOT));
        if (mapped == null || mapped.isBlank()) return value;
        return mapped;
    }

    private static @NotNull ConfigurationNode getToggleSoundSettingsNode(
            @NotNull ConfigurationNode parent,
            @NotNull String primaryPath,
            @NotNull String legacyPath,
            boolean legacyBooleanPath
    ) {
        ConfigurationNode node = parent.node(primaryPath);
        if (!node.virtual()) return node;

        node = parent.node(legacyPath);
        if (!node.virtual()) return node;

        node = parent.node(legacyBooleanPath);
        return node;
    }

    private static EquipmentSlot convertConfigToEquipment(String slot) {
        return switch (slot) {
            case "helmet" -> EquipmentSlot.HEAD;
            case "chestplate" -> EquipmentSlot.CHEST;
            case "leggings" -> EquipmentSlot.LEGS;
            case "boots" -> EquipmentSlot.FEET;
            case "offhand" -> EquipmentSlot.OFF_HAND;
            case "mainhand" -> EquipmentSlot.HAND;
            default -> null;
        };
    }
}
