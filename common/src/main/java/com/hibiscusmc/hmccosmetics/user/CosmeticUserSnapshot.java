package com.hibiscusmc.hmccosmetics.user;

import com.hibiscusmc.hmccosmetics.config.Settings;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import com.hibiscusmc.hmccosmetics.cosmetic.types.CosmeticArmorType;
import com.hibiscusmc.hmccosmetics.user.manager.UserBackpackManager;
import com.hibiscusmc.hmccosmetics.user.manager.UserEntity;
import com.hibiscusmc.hmccosmetics.user.manager.UserWardrobeManager;
import com.hibiscusmc.hmccosmetics.util.HMCCInventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CosmeticUserSnapshot {

    private static final CosmeticUserSnapshot EMPTY = new CosmeticUserSnapshot(
        null,
        false,
        false,
        Set.of(),
        Collections.emptyMap(),
        Collections.emptyMap(),
        List.of()
    );

    private final UUID uniqueId;
    private final boolean inWardrobe;
    private final boolean wardrobeRunning;
    private final Set<CosmeticSlot> cosmeticSlots;
    private final Map<Integer, ItemStack> containerOverrides;
    private final Map<EquipmentSlot, ItemStack> equipmentOverrides;
    private final List<Integer> backpackEntityIds;

    private CosmeticUserSnapshot(
        @Nullable UUID uniqueId,
        boolean inWardrobe,
        boolean wardrobeRunning,
        @NotNull Set<CosmeticSlot> cosmeticSlots,
        @NotNull Map<Integer, ItemStack> containerOverrides,
        @NotNull Map<EquipmentSlot, ItemStack> equipmentOverrides,
        @NotNull List<Integer> backpackEntityIds
    ) {
        this.uniqueId = uniqueId;
        this.inWardrobe = inWardrobe;
        this.wardrobeRunning = wardrobeRunning;
        this.cosmeticSlots = cosmeticSlots;
        this.containerOverrides = containerOverrides;
        this.equipmentOverrides = equipmentOverrides;
        this.backpackEntityIds = backpackEntityIds;
    }

    public static @NotNull CosmeticUserSnapshot empty(@Nullable UUID uniqueId) {
        if (uniqueId == null) {
            return EMPTY;
        }
        return new CosmeticUserSnapshot(
            uniqueId,
            false,
            false,
            Set.of(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            List.of()
        );
    }

    public static @NotNull CosmeticUserSnapshot fromUser(@NotNull CosmeticUser user) {
        Player player = user.getPlayer();
        if (player == null) {
            return empty(user.getUniqueId());
        }

        boolean inWardrobe = user.isInWardrobe();
        boolean wardrobeRunning = false;
        if (inWardrobe && user.getWardrobeManager() != null) {
            wardrobeRunning = user.getWardrobeManager().getWardrobeStatus() == UserWardrobeManager.WardrobeStatus.RUNNING;
        }

        Set<CosmeticSlot> slots = user.getSlotsWithCosmetics().isEmpty()
            ? Set.of()
            : Set.copyOf(user.getSlotsWithCosmetics());

        Map<Integer, ItemStack> containerOverrides = new HashMap<>();
        Map<EquipmentSlot, ItemStack> equipmentOverrides = new HashMap<>();

        if (!inWardrobe) {
            for (Cosmetic cosmetic : user.getCosmetics()) {
                if (!(cosmetic instanceof CosmeticArmorType armorType)) {
                    continue;
                }
                EquipmentSlot equipSlot = armorType.getEquipSlot();
                ItemStack item = user.getUserCosmeticItem(armorType);
                if (item == null) {
                    continue;
                }

                boolean requireEmpty = Settings.getSlotOption(equipSlot).isRequireEmpty();
                boolean isAir = player.getInventory().getItem(equipSlot).getType().isAir();

                int packetSlot = HMCCInventoryUtils.getPacketArmorSlot(equipSlot);
                if (packetSlot != -1 && (!requireEmpty || isAir)) {
                    containerOverrides.put(packetSlot, item.clone());
                }

                if (equipSlot != EquipmentSlot.HAND && (!requireEmpty || !isAir)) {
                    equipmentOverrides.put(equipSlot, item.clone());
                }
            }
        }

        List<Integer> backpackEntityIds = List.of();
        UserBackpackManager backpackManager = user.getUserBackpackManager();
        if (backpackManager != null) {
            UserEntity entityManager = backpackManager.getEntityManager();
            if (entityManager != null) {
                backpackEntityIds = List.copyOf(entityManager.getIds());
            }
        }

        return new CosmeticUserSnapshot(
            user.getUniqueId(),
            inWardrobe,
            wardrobeRunning,
            slots,
            Collections.unmodifiableMap(containerOverrides),
            Collections.unmodifiableMap(equipmentOverrides),
            backpackEntityIds
        );
    }

    public boolean hasCosmeticInSlot(@NotNull CosmeticSlot slot) {
        return cosmeticSlots.contains(slot);
    }

    public boolean isInWardrobe() {
        return inWardrobe;
    }

    public boolean isWardrobeRunning() {
        return wardrobeRunning;
    }

    public @Nullable ItemStack getContainerOverride(int slot) {
        ItemStack item = containerOverrides.get(slot);
        return item == null ? null : item.clone();
    }

    public @Nullable ItemStack getEquipmentOverride(@NotNull EquipmentSlot slot) {
        ItemStack item = equipmentOverrides.get(slot);
        return item == null ? null : item.clone();
    }

    public @NotNull List<Integer> getBackpackEntityIds() {
        return backpackEntityIds;
    }
}
