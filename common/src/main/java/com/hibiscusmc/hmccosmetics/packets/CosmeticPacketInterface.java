package com.hibiscusmc.hmccosmetics.packets;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.Settings;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import com.hibiscusmc.hmccosmetics.cosmetic.types.CosmeticBackpackType;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUserSnapshot;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import com.hibiscusmc.hmccosmetics.user.manager.UserWardrobeManager;
import com.hibiscusmc.hmccosmetics.util.HMCCInventoryUtils;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import com.hibiscusmc.hmccosmetics.util.packets.HMCCPacketManager;
import me.lojosho.hibiscuscommons.packets.PacketAction;
import me.lojosho.hibiscuscommons.packets.PacketInterface;
import me.lojosho.hibiscuscommons.packets.data.*;
import me.lojosho.hibiscuscommons.util.EntityIdRegistry;
import me.lojosho.hibiscuscommons.util.PacketThreadGate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CosmeticPacketInterface implements PacketInterface {

    @Override
    public @NotNull PacketAction writeContainerContent(@NotNull Player player, @NotNull ContainerContentWrapper wrapper) {
        int windowId = wrapper.getWindowId();
        MessagesUtil.sendDebugMessages("writeContainerContent (windowid: " + windowId + " )");
        if (windowId != 0) return PacketAction.NOTHING;

        CosmeticUserSnapshot snapshot = CosmeticUsers.getSnapshot(player);
        if (snapshot.isInWardrobe()) return PacketAction.NOTHING;

        List<ItemStack> slotData = wrapper.getSlotData();
        int maxSlots = slotData.size();

        for (int slot = 0; slot < maxSlots; slot++) {
            if ((slot >= 5 && slot <= 8) || slot == 45) {
                ItemStack override = snapshot.getContainerOverride(slot);
                if (override == null) continue;
                slotData.set(slot, override);
                if (Settings.isDebugMode()) MessagesUtil.sendDebugMessages("Set " + slot + " as " + override);
            }
        }

        wrapper.setSlotData(slotData);
        MessagesUtil.sendDebugMessages("Menu Fired, updated cosmetics on slotdata " + windowId + " with " + slotData.size());
        return PacketAction.CHANGED;
    }

    @Override
    public @NotNull PacketAction writeSlotContent(@NotNull Player player, @NotNull SlotContentWrapper wrapper) {
        int windowId = wrapper.getWindowId();
        int slot = wrapper.getSlot();

        MessagesUtil.sendDebugMessages("SetSlot Initial ");
        if (windowId != 0) return PacketAction.NOTHING;

        CosmeticUserSnapshot snapshot = CosmeticUsers.getSnapshot(player);
        if (snapshot.isInWardrobe()) return PacketAction.NOTHING;

        MessagesUtil.sendDebugMessages("SetSlot Slot " + slot);
        CosmeticSlot cosmeticSlot = HMCCInventoryUtils.NMSCosmeticSlot(slot);
        EquipmentSlot equipmentSlot = HMCCInventoryUtils.getPacketArmorSlot(slot);
        if (cosmeticSlot == null || equipmentSlot == null) return PacketAction.NOTHING;
        if (!snapshot.hasCosmeticInSlot(cosmeticSlot)) return PacketAction.NOTHING;

        ItemStack override = snapshot.getContainerOverride(slot);
        if (override == null) return PacketAction.NOTHING;

        wrapper.setItemStack(override);
        return PacketAction.CHANGED;
    }

    @Override
    public @NotNull PacketAction writeEquipmentContent(@NotNull Player player, @NotNull EntityEquipmentWrapper wrapper) {
        if (player.getEntityId() != wrapper.getEntityId()) return PacketAction.NOTHING;

        CosmeticUserSnapshot snapshot = CosmeticUsers.getSnapshot(player);
        if (snapshot.isInWardrobe()) return PacketAction.NOTHING;

        Map<EquipmentSlot, ItemStack> armor = wrapper.getArmor();
        for (EquipmentSlot slot : armor.keySet()) {
            ItemStack override = snapshot.getEquipmentOverride(slot);
            if (override != null) {
                armor.put(slot, override);
            }
        }

        wrapper.setArmor(armor);
        MessagesUtil.sendDebugMessages("Equipment for " + player.getName() + " has been updated for " + player.getName());
        return PacketAction.CHANGED;
    }

    @Override
    public @NotNull PacketAction writePassengerContent(@NotNull Player player, @NotNull PassengerWrapper wrapper) {
        if (!Settings.isBackpackInterceptPassengerPacket()) return PacketAction.NOTHING;

        CosmeticUser viewerUser = CosmeticUsers.getUser(player);
        if (viewerUser == null || viewerUser.isInWardrobe()) return PacketAction.NOTHING;

        int ownerId = wrapper.getOwner();

        Optional<CosmeticUser> optionalCosmeticUser = CosmeticUsers.values().stream().filter(user -> user.getPlayer() != null).filter(user -> ownerId == user.getPlayer().getEntityId()).findFirst();
        if (optionalCosmeticUser.isEmpty()) return PacketAction.NOTHING;
        CosmeticUser user = optionalCosmeticUser.get();

        Cosmetic backpackCosmetic = user.getCosmetic(CosmeticSlot.BACKPACK);
        if (backpackCosmetic == null) return PacketAction.NOTHING;
        if (!(backpackCosmetic instanceof CosmeticBackpackType cosmeticBackpackType)) return PacketAction.NOTHING;
        // If a player is viewing their own backpack, don't do anything
        if (user.getUniqueId().equals(viewerUser.getUniqueId())) {
            if (cosmeticBackpackType.isFirstPersonCompadible()) return PacketAction.NOTHING;
        }

        if (user.getUserBackpackManager() == null) return PacketAction.NOTHING;

        List<Integer> originalPassengers = wrapper.getPassengers();
        List<Integer> passengers = new ArrayList<>(originalPassengers.size() + 1);
        passengers.add(user.getUserBackpackManager().getFirstArmorStandId());
        passengers.addAll(originalPassengers);
        wrapper.setPassengers(passengers);
        return PacketAction.CHANGED;
    }

    @Override
    public PacketAction readPlayerScale(@NotNull Player player, @NotNull PlayerScaleWrapper wrapper) {
        UUID uuid = EntityIdRegistry.getUuid(wrapper.getEntityId());
        if (uuid == null) return PacketAction.NOTHING;

        CosmeticUserSnapshot snapshot = CosmeticUsers.getSnapshot(uuid);
        if (snapshot.isInWardrobe()) return PacketAction.NOTHING;

        List<Integer> backpackEntityIds = snapshot.getBackpackEntityIds();
        if (backpackEntityIds.isEmpty()) return PacketAction.NOTHING;

        PacketThreadGate.runOnEntity(HMCCosmeticsPlugin.getInstance(), player, () -> {
            for (int cosmeticId : backpackEntityIds) {
                HMCCPacketManager.sendEntityScalePacket(cosmeticId, wrapper.getScale(), Collections.singletonList(player));
            }
        });

        return PacketAction.NOTHING;
    }

    @Override
    public @NotNull PacketAction readInventoryClick(@NotNull Player player, @NotNull InventoryClickWrapper wrapper) {
        int clickType = wrapper.getClickType();
        int slotNumber = wrapper.getSlotNumber();
        if (clickType != 0 || slotNumber == -999) return PacketAction.NOTHING;

        CosmeticUserSnapshot snapshot = CosmeticUsers.getSnapshot(player);
        if (snapshot.isInWardrobe()) return PacketAction.NOTHING;

        CosmeticSlot cosmeticSlot = HMCCInventoryUtils.NMSCosmeticSlot(slotNumber);
        if (cosmeticSlot == null || !snapshot.hasCosmeticInSlot(cosmeticSlot)) return PacketAction.NOTHING;

        PacketThreadGate.runOnEntityLater(HMCCosmeticsPlugin.getInstance(), player, () -> {
            CosmeticUser user = CosmeticUsers.getUser(player);
            if (user == null) return;
            user.updateCosmetic(cosmeticSlot);
        }, 1);
        MessagesUtil.sendDebugMessages("Packet fired, updated cosmetic " + cosmeticSlot);
        return PacketAction.NOTHING;
    }

    @Override
    public @NotNull PacketAction readPlayerAction(@NotNull Player player, @NotNull PlayerActionWrapper wrapper) {
        if (!Settings.isPreventOffhandSwapping()) return PacketAction.NOTHING;
        String actionType = wrapper.getActionType();
        MessagesUtil.sendDebugMessages("EntityStatus Initial " + player.getEntityId() + " - " + actionType);
        // If it's not SWAP_ITEM_WITH_OFFHAND, ignore
        if (!actionType.equalsIgnoreCase("SWAP_ITEM_WITH_OFFHAND")) return PacketAction.NOTHING;

        CosmeticUserSnapshot snapshot = CosmeticUsers.getSnapshot(player);
        if (!snapshot.hasCosmeticInSlot(CosmeticSlot.OFFHAND)) return PacketAction.NOTHING;
        return PacketAction.CANCELLED;
    }

    @Override
    public @NotNull PacketAction readPlayerArm(@NotNull Player player, @NotNull PlayerSwingWrapper wrapper) {
        CosmeticUserSnapshot snapshot = CosmeticUsers.getSnapshot(player);
        if (!snapshot.isInWardrobe() || !snapshot.isWardrobeRunning()) return PacketAction.NOTHING;

        PacketThreadGate.runOnEntity(HMCCosmeticsPlugin.getInstance(), player, () -> {
            CosmeticUser user = CosmeticUsers.getUser(player);
            if (user == null || !user.isInWardrobe()) return;
            if (user.getWardrobeManager().getWardrobeStatus() != UserWardrobeManager.WardrobeStatus.RUNNING) return;

            user.getWardrobeManager().openLastOpenMenu();
        });
        return PacketAction.CANCELLED;
    }

    @Override
    public @NotNull PacketAction readEntityHandle(@NotNull Player player, @NotNull PlayerInteractWrapper wrapper) {
        CosmeticUserSnapshot snapshot = CosmeticUsers.getSnapshot(player);
        if (!snapshot.isInWardrobe()) return PacketAction.NOTHING;
        return PacketAction.CANCELLED;
    }
}
