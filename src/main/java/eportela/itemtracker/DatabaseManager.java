package eportela.itemtracker;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private Connection connection;
    private String host;
    private JavaPlugin plugin;
    private String database;
    private String username;
    private String password;

    public DatabaseManager(JavaPlugin plugin, String host, String database, String username, String password) {
        this.plugin = plugin;
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public boolean connect() {
        String url = "jdbc:mysql://" + host + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useLegacyDatetimeCode=false&useUnicode=true&characterEncoding=UTF-8";

        try {
            connection = DriverManager.getConnection(url, username, password);
            plugin.getLogger().info("Connected to MySQL database");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL database", e);
            return false;
        }
    }

    public void createItemTable() {
        try (PreparedStatement statement = getConnection().prepareStatement(ItemTracker.ITEM_TABLE)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating the item table", e);
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Disconnected from MySQL database");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to disconnect from MySQL database: " + e.getMessage(), e);
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void startTrackItem(String playerId, ItemStack item) {
        String sql = "INSERT INTO tracked_items (player_uuid, item_nbt, custom_id) VALUES (?, ?, ?)";

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            String itemNBT = serializeItemNBT(item);
            UUID uniqueId = UUID.randomUUID();
            statement.setString(1, playerId);
            statement.setString(2, itemNBT);
            statement.setString(3, uniqueId.toString());
            statement.executeUpdate();
            plugin.getLogger().info("Item tracked successfully!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to track item: " + e.getMessage(), e);
        }
    }

    public String serializeItemNBT(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        String serializedNBT = null;
        if (meta != null) {
            serializedNBT = meta.getPersistentDataContainer().toString();
        }

        return serializedNBT;
    }

    public ItemStack deserializeItemNBT(String itemNBT) {
        if (itemNBT == null) {
            return null;
        }

        ItemStack item = new ItemStack(Material.STONE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(new NamespacedKey(plugin, "serialized_nbt"), PersistentDataType.STRING, itemNBT);
            item.setItemMeta(meta);
            return item;
        }

        return null;
    }
    public void updateItemOwner(String customId, String playerId) {
        String sql = "UPDATE tracked_items SET player_uuid = ? WHERE id = ?";

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, playerId);
            statement.setString(2, customId);
            statement.executeUpdate();
            plugin.getLogger().info("Item owner updated successfully!");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update item owner: " + e.getMessage(), e);
        }
    }
}