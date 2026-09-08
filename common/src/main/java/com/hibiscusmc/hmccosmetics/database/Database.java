package com.hibiscusmc.hmccosmetics.database;

import com.hibiscusmc.hmccosmetics.config.section.DatabaseSettings;
import com.hibiscusmc.hmccosmetics.database.types.Data;
import com.hibiscusmc.hmccosmetics.database.types.MySQLData;
import com.hibiscusmc.hmccosmetics.user.CosmeticUser;
import com.hibiscusmc.hmccosmetics.user.CosmeticUsers;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Database {

    @Getter
    private static Data data;
    private static final MySQLData MYSQL_DATA = new MySQLData();
    public Database() {
        String databaseType = DatabaseSettings.getDatabaseType();
        if (databaseType == null || !databaseType.equalsIgnoreCase("mysql")) {
            throw new IllegalStateException(
                    "HMCCosmetics requires shared MySQL storage on sharded survival; configured type: "
                            + databaseType);
        }
        data = MYSQL_DATA;
        com.hibiscusmc.hmccosmetics.util.MessagesUtil.sendDebugMessages("Database is " + data);

        setup();
    }

    public static void setup() {
        data.setup();
    }

    public static void save(CosmeticUser user) {
        data.save(user);
    }

    public static CompletableFuture<Void> saveAsync(CosmeticUser user) {
        return data.saveAsync(user);
    }

    public static void save(Player player) {
        data.save(CosmeticUsers.getUser(player));
    }

    public static CompletableFuture<UserData> get(UUID uniqueId) {
        return data.get(uniqueId);
    }

    public static void clearData(UUID uniqueId) {
        data.clear(uniqueId);
    }
}
