package com.example.hud.listener;

import com.example.hud.HudPlugin;
import com.example.hud.manager.HudManager;
import com.example.hud.manager.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final HudPlugin plugin;
    private final HudManager hudManager;
    private final PlayerDataManager playerDataManager;

    public PlayerListener(HudPlugin plugin, HudManager hudManager, PlayerDataManager playerDataManager) {
        this.plugin             = plugin;
        this.hudManager         = hudManager;
        this.playerDataManager  = playerDataManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        playerDataManager.loadPlayer(event.getPlayer());
        // Delay by 5 ticks (0.25s) so the client finishes login sequence
        // before we send boss bar / action bar packets
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                hudManager.addPlayer(event.getPlayer());
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playerDataManager.savePlayer(event.getPlayer());
        hudManager.removePlayer(event.getPlayer());
        playerDataManager.unloadPlayer(event.getPlayer().getUniqueId());
    }
}
