package com.example.hud.listener;

import com.example.hud.manager.HudManager;
import com.example.hud.manager.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final HudManager hudManager;
    private final PlayerDataManager playerDataManager;

    public PlayerListener(HudManager hudManager, PlayerDataManager playerDataManager) {
        this.hudManager        = hudManager;
        this.playerDataManager = playerDataManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        playerDataManager.loadPlayer(event.getPlayer());
        hudManager.addPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        playerDataManager.savePlayer(event.getPlayer());
        hudManager.removePlayer(event.getPlayer());
        playerDataManager.unloadPlayer(event.getPlayer().getUniqueId());
    }
}
