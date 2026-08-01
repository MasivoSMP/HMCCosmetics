package com.hibiscusmc.hmccosmetics.database.types;

import com.hibiscusmc.hmccosmetics.config.Settings;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetic;
import com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot;
import com.hibiscusmc.hmccosmetics.cosmetic.Cosmetics;
import com.hibiscusmc.hmccosmetics.database.UserData;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import org.apache.commons.lang3.EnumUtils;
import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public abstract class Data {

    public abstract void setup();

    public abstract void save(CosmeticUser user);

    public CompletableFuture<Void> saveAsync(CosmeticUser user) {
        save(user);
        return CompletableFuture.completedFuture(null);
    }

    @Nullable
    public abstract CompletableFuture<UserData> get(UUID uniqueId);

    public abstract void clear(UUID uniqueId);

    // BACKPACK=colorfulbackpack&RRGGBB,HELMET=niftyhat,BALLOON=colorfulballoon,CHESTPLATE=niftychestplate
    @NotNull
    public final String serializeData(@NotNull CosmeticUser user) {
        List<String> data = new ArrayList<>();
        if (user.isHidden()) {
            for (CosmeticUser.HiddenReason reason :  user.getHiddenReasons()) {
                if (shouldHiddenSave(reason)) data.add("HIDDEN=" + reason);
            }
        }
        for (String cosmeticId : user.getPurchasedCosmetics()) {
            data.add("PURCHASED=" + cosmeticId);
        }
        for (String cosmeticId : user.getAdvancementUnlockedCosmetics()) {
            data.add("ADVANCEMENT=" + cosmeticId);
        }
        for (Cosmetic cosmetic : user.getCosmetics()) {
            Color color = user.getCosmeticColor(cosmetic.getSlot());
            String input = cosmetic.getSlot() + "=" + cosmetic.getId();
            if (color != null) input = input + "&" + color.asRGB();
            data.add(input);
        }
        return String.join(",", data);
    }

    public final void deserializeData(@NotNull UserData userData, @NotNull String raw) {
        String[] rawData = raw.split(",");
        for (String a : rawData) {
            if (a == null || a.isEmpty()) continue;
            String[] splitData = a.split("=", 2);
            if (splitData.length < 2) continue;
            MessagesUtil.sendDebugMessages("First split (suppose slot) " + splitData[0]);
            if (splitData[0].equalsIgnoreCase("HIDDEN")) {
                if (EnumUtils.isValidEnum(CosmeticUser.HiddenReason.class, splitData[1])) {
                    if (Settings.isForceShowOnJoin()) continue;
                    userData.addHiddenReason(CosmeticUser.HiddenReason.valueOf(splitData[1]));
                }
                continue;
            }
            if (splitData[0].equalsIgnoreCase("PURCHASED")) {
                userData.addPurchasedCosmetic(splitData[1]);
                continue;
            }
            if (splitData[0].equalsIgnoreCase("ADVANCEMENT")) {
                userData.addAdvancementUnlockedCosmetic(splitData[1]);
                continue;
            }

            CosmeticSlot slot = CosmeticSlot.valueOf(splitData[0]);
            Cosmetic cosmetic = null;
            if (slot == null) continue;
            if (splitData[1].contains("&")) {
                String[] colorSplitData = splitData[1].split("&");
                if (Cosmetics.hasCosmetic(colorSplitData[0])) cosmetic = Cosmetics.getCosmetic(colorSplitData[0]);
                if (slot == null || cosmetic == null) continue;
                userData.addCosmetic(slot, cosmetic, Integer.parseInt(colorSplitData[1]));
            } else {
                if (Cosmetics.hasCosmetic(splitData[1])) cosmetic = Cosmetics.getCosmetic(splitData[1]);
                if (slot == null || cosmetic == null) continue;
                userData.addCosmetic(slot, cosmetic, -1);
            }
        }
    }

    private boolean shouldHiddenSave(CosmeticUser.HiddenReason reason) {
        switch (reason) {
            case EMOTE, NONE, GAMEMODE, WORLD, DISABLED, POTION -> {
                return false;
            }
            default -> {
                return true;
            }
        }
    }
}
