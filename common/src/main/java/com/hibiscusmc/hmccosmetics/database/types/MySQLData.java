package com.hibiscusmc.hmccosmetics.database.types;

import com.hibiscusmc.hmccosmetics.HMCCosmeticsPlugin;
import com.hibiscusmc.hmccosmetics.config.section.DatabaseSettings;
import com.hibiscusmc.hmccosmetics.util.MessagesUtil;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

public class MySQLData extends SQLData {

    // Connection Information
    private String host;
    private String user;
    private String database;
    private String password;
    private int port;

    @Nullable
    private Connection connection;

    @Override
    public void setup() {
        host = DatabaseSettings.getHost();
        user = DatabaseSettings.getUsername();
        database = DatabaseSettings.getDatabase();
        password = DatabaseSettings.getPassword();
        port = DatabaseSettings.getPort();

        HMCCosmeticsPlugin plugin = HMCCosmeticsPlugin.getInstance();
        try {
            validateConfiguration(plugin);
            openConnection();
            if (connection == null) throw new IllegalStateException("Connection is null");
            try (PreparedStatement preparedStatement =  connection.prepareStatement("CREATE TABLE IF NOT EXISTS `COSMETICDATABASE` " +
                    "(UUID varchar(36) PRIMARY KEY, " +
                    "COSMETICS MEDIUMTEXT " +
                    ");")) {
                preparedStatement.execute();
            }
        } catch (SQLException | IllegalStateException e) {
            plugin.getLogger().severe("");
            plugin.getLogger().severe("");
            plugin.getLogger().severe("MySQL DATABASE CAN NOT BE REACHED.");
            plugin.getLogger().severe("CHECK CONFIG FOR ERRORS");
            plugin.getLogger().severe("");
            plugin.getLogger().severe("SAFETY SHUTTING DOWN SERVER");
            plugin.getLogger().severe("");
            plugin.getLogger().severe("");
            Bukkit.shutdown();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clear(UUID uniqueId) {
        HMCCosmeticsPlugin.getInstance().getScheduler().runAsync(() -> {
            try (PreparedStatement preparedSt = preparedStatement("DELETE FROM COSMETICDATABASE WHERE UUID=?;")) {
                preparedSt.setString(1, uniqueId.toString());
                preparedSt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void openConnection() throws SQLException {
        // Connection isn't null AND Connection isn't closed :: return
        try {
            if (isConnectionOpen()) return;
            if (connection != null) close(); // Close connection if still active
        } catch (RuntimeException e) {
            e.printStackTrace(); // If isConnectionOpen() throws error
        }

        // Connect to database host
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, setupProperties());
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    public void close() {
        HMCCosmeticsPlugin.getInstance().getScheduler().runAsync(() -> {
            try {
                if (connection == null) throw new IllegalStateException("Connection is null");
                connection.close();
            } catch (SQLException | NullPointerException e) {
                System.out.println(e.getMessage());
            }
        });
    }

    @NotNull
    private Properties setupProperties() {
        Properties props = new Properties();
        String username = user == null ? "" : user;
        String secret = password == null ? "" : password;
        props.setProperty("user", username);
        props.setProperty("password", secret);
        return props;
    }

    private void validateConfiguration(HMCCosmeticsPlugin plugin) {
        boolean invalid = false;
        if (host == null || host.isBlank()) {
            plugin.getLogger().severe("MySQL host is missing in database-settings.");
            invalid = true;
        }
        if (database == null || database.isBlank()) {
            plugin.getLogger().severe("MySQL database is missing in database-settings.");
            invalid = true;
        }
        if (user == null || user.isBlank()) {
            plugin.getLogger().severe("MySQL username is missing in database-settings.");
            invalid = true;
        }
        if (invalid) {
            throw new IllegalStateException("Invalid MySQL configuration");
        }
        if (password == null) {
            plugin.getLogger().warning("MySQL password is missing; using empty string.");
            password = "";
        }
    }

    private boolean isConnectionOpen() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PreparedStatement preparedStatement(String query) {
        PreparedStatement ps = null;

        if (!isConnectionOpen()) {
            MessagesUtil.sendDebugMessages("The MySQL database connection is not open (Could the database been idle for to long?). Reconnecting...", Level.WARNING);
            try {
                openConnection();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        try {
            if (connection == null) throw new IllegalStateException("Connection is null");
            ps = connection.prepareStatement(query);
        } catch (SQLException | IllegalStateException e) {
            e.printStackTrace();
        }

        return ps;
    }
}
