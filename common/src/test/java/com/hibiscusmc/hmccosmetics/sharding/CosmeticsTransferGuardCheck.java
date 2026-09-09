package com.hibiscusmc.hmccosmetics.sharding;

import gg.masivo.sharding.paper.api.guard.OutgoingPreparationResult;
import gg.masivo.sharding.paper.api.guard.TransferGuardResult;
import org.bukkit.entity.Player;

/** Run with assertions enabled; no server or optional cosmetic provider is required. */
public final class CosmeticsTransferGuardCheck {
    public static void main(String[] args) {
        java.util.UUID id = java.util.UUID.randomUUID();
        Player player = (Player) java.lang.reflect.Proxy.newProxyInstance(Player.class.getClassLoader(),
            new Class<?>[]{Player.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("getUniqueId")) return id;
                throw new UnsupportedOperationException(method.getName());
            });
        CosmeticsTransferGuard guard = new CosmeticsTransferGuard();
        assert guard.evaluate(player, null) instanceof TransferGuardResult.Allowed;
        assert guard.prepareOutgoing(player, null).toCompletableFuture().join() instanceof OutgoingPreparationResult.Ready;
    }
}
