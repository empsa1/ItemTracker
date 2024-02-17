package eportela.itemtracker;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class TrackItemCommand implements CommandExecutor, Listener {

    private final DatabaseManager databaseManager;
    public TrackItemCommand(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (args.length != 0) {
            MessageSender.sendMessage("/trackItem has no arguments!", ChatColor.RED, player);
            return false;
        }

        if (itemInHand.getType() == Material.AIR) {
            MessageSender.sendMessage("You must hold an item to track it!", ChatColor.RED, player);
            return false;
        }

        if (itemInHand.getMaxStackSize() != 1) {
            MessageSender.sendMessage("You cannot track items with stack size different than 1!", ChatColor.RED, player);
            return false;
        }

        if (isTrackedItem(itemInHand)) {
            MessageSender.sendMessage("Item is already being tracked!", ChatColor.RED, player);
            return false;
        }
        databaseManager.startTrackItem(player.getUniqueId().toString(), itemInHand);
        sender.sendMessage(ChatColor.GREEN + "Item tracked successfully!");
        return true;
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem().getItemStack();
        if (isTrackedItem(item)) {
            System.out.println("Item is being tracked!");
            updateItemOwner(player, item);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (isTrackedItem(item)) {
            System.out.println("Item is being tracked!");
            updateItemOwner(player, item);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (isTrackedItem(item)) {
            System.out.println("Item is being tracked!");
            updateItemOwner(null, item);
        }
    }

    private boolean isTrackedItem(ItemStack item) {
        String serializedNBT = databaseManager.serializeItemNBT(item);
        if (serializedNBT == null) {
            return false;
        }

        try (PreparedStatement statement = databaseManager.getConnection().prepareStatement("SELECT COUNT(*) AS count FROM tracked_items WHERE item_nbt = ?")) {
            statement.setString(1, serializedNBT);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt("count");
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            (ItemTracker.getPlugin(ItemTracker.class)).getLogger().log(Level.SEVERE, "Failed to check if item is tracked: " + e.getMessage(), e);
        }
        return false;
    }

    private void updateItemOwner(Player player, ItemStack item) {
        String customId = getItemCustomId(item);
        if (customId != null) {
            String playerId = (player != null) ? player.getUniqueId().toString() : null;
            databaseManager.updateItemOwner(customId, playerId);
        }
    }

    private String getItemCustomId(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(ItemTracker.getPlugin(ItemTracker.class), "custom_id"), PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(new NamespacedKey(ItemTracker.getPlugin(ItemTracker.class), "custom_id"), PersistentDataType.STRING);
        }
        return null;
    }
}