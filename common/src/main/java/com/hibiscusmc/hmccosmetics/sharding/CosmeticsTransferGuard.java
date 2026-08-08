package com.hibiscusmc.hmccosmetics.sharding;

import com.hibiscusmc.hmccosmetics.database.Database;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import gg.masivo.sharding.paper.api.guard.OutgoingPreparationResult;
import gg.masivo.sharding.paper.api.guard.PlayerTransferGuard;
import gg.masivo.sharding.paper.api.guard.TransferGuardResult;
import gg.masivo.sharding.paper.api.guard.TransferIntent;
import java.util.concurrent.CompletionStage;
import org.bukkit.entity.Player;

public final class CosmeticsTransferGuard implements PlayerTransferGuard {
    @Override
    public String namespace() {
        return "hmccosmetics:profile";
    }

    @Override
    public TransferGuardResult evaluate(Player player, TransferIntent intent) {
        CosmeticUser user = CosmeticUsers.getUser(player);
        if (user == null) {
            return TransferGuardResult.deferred("hmccosmetics:profile_unavailable");
        }
        if (user.isInWardrobe()) {
            return TransferGuardResult.rejected("hmccosmetics:wardrobe_active");
        }
        return TransferGuardResult.allowed();
    }

    @Override
    public CompletionStage<OutgoingPreparationResult> prepareOutgoing(
            Player player, TransferIntent intent) {
        CosmeticUser user = CosmeticUsers.getUser(player);
        if (user == null || user.isInWardrobe()) {
            return java.util.concurrent.CompletableFuture.completedFuture(
                    OutgoingPreparationResult.rejected("hmccosmetics:profile_changed"));
        }
        return Database.saveAsync(user).handle((ignored, failure) -> failure == null
                ? OutgoingPreparationResult.ready()
                : OutgoingPreparationResult.rejected("hmccosmetics:profile_save_failed"));
    }
}
