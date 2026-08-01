package com.hibiscusmc.hmccosmetics.database.types;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.database.UserData;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class SQLData extends Data {
    @Override
    @SuppressWarnings({"resource"}) // Duplicate is from deprecated InternalData
    public CompletableFuture<UserData> get(UUID uniqueId) {
        return CompletableFuture.supplyAsync(() -> {
            UserData data = new UserData(uniqueId);

            try (PreparedStatement preparedStatement = preparedStatement("SELECT * FROM COSMETICDATABASE WHERE UUID = ?;")){
                preparedStatement.setString(1, uniqueId.toString());
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    if (rs.next()) {
                        String rawData = rs.getString("COSMETICS");
                        deserializeData(data, rawData);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return data;
        });
    }

    @Override
    @SuppressWarnings("resource")
    public void save(CosmeticUser user) {
        saveAsync(user).exceptionally(failure -> {
            HMCCosmeticsPlugin.getInstance().getLogger().log(
                    java.util.logging.Level.SEVERE,
                    "Unable to save cosmetic profile for " + user.getUniqueId(),
                    failure);
            return null;
        });
    }

    @Override
    @SuppressWarnings("resource")
    public CompletableFuture<Void> saveAsync(CosmeticUser user) {
        if (user == null) {
            return CompletableFuture.completedFuture(null);
        }
        String serialized = serializeData(user);
        UUID userId = user.getUniqueId();

        Runnable run = () -> {
            try (PreparedStatement preparedSt = preparedStatement("REPLACE INTO COSMETICDATABASE(UUID,COSMETICS) VALUES(?,?);")) {
                preparedSt.setString(1, userId.toString());
                preparedSt.setString(2, serialized);
                preparedSt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
        if (!HMCCosmeticsPlugin.getInstance().isDisabled()) {
            return CompletableFuture.runAsync(run);
        } else {
            try {
                run.run();
                return CompletableFuture.completedFuture(null);
            } catch (RuntimeException failure) {
                return CompletableFuture.failedFuture(failure);
            }
        }
    }

    public abstract PreparedStatement preparedStatement(String query);
}
