package eportela.itemtracker;

import org.bukkit.plugin.java.JavaPlugin;

public final class ItemTracker extends JavaPlugin {

    private DatabaseManager databaseManager;

    public static final String IDENTIFIER = "[ItemTracker]: ";
    public static final String ITEM_TABLE =
            "CREATE TABLE IF NOT EXISTS tracked_items ( " +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "item_nbt TEXT NOT NULL, " +
                    "custom_id VARCHAR(36) NOT NULL, " +
                    "CONSTRAINT UC_Player_Item UNIQUE (player_uuid, custom_id)" +
                    ");";
    private String host = "localhost";
    private String database = "itemTrackerDB";
    private String username = "root";
    private String password = "PASSWORD";

    @Override
    public void onEnable() {
        getLogger().info("Inside onEnable");
        try {
            databaseManager = new DatabaseManager(this, host, database, username, password);
            if (!databaseManager.connect()) {
                getLogger().warning("Failed to connect to the database. Plugin will be disabled.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            getLogger().info("Connected to MySQL database");
            databaseManager.createItemTable();
            getLogger().info("Item table created successfully");
            getLogger().info("Registering commands and events...");
            getCommand("trackItem").setExecutor(new TrackItemCommand(databaseManager));
            getLogger().info("ItemTracker plugin enabled");
        } catch (Exception e) {
            getLogger().severe("Error occurred while enabling ItemTracker plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("ItemTracker plugin disabled");
    }
}