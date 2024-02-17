package eportela.itemtracker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageSender {

    public static void sendMessage(String message, ChatColor color, Player player) {
        if (player != null && player.isOnline()) {
            message = ItemTracker.IDENTIFIER + message;
            player.sendMessage(color + message);
        } else {
            Bukkit.broadcastMessage(color + message);
        }
    }
}