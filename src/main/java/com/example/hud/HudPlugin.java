package com.example.hud;

import com.example.hud.command.GoldCommand;
import com.example.hud.command.HudCommand;
import com.example.hud.command.ManaCommand;
import com.example.hud.listener.PlayerListener;
import com.example.hud.manager.HudManager;
import com.example.hud.manager.PlayerDataManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HudPlugin extends JavaPlugin {

    private HudManager hudManager;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        playerDataManager = new PlayerDataManager(this);
        hudManager = new HudManager(this, playerDataManager);

        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, hudManager, playerDataManager), this);

        registerCommand("hud", new HudCommand(this, hudManager));
        registerCommand("mana", new ManaCommand(this, playerDataManager));
        registerCommand("gold", new GoldCommand(this, playerDataManager));

        // Init HUD for players already online (e.g. after /reload)
        for (Player player : getServer().getOnlinePlayers()) {
            playerDataManager.loadPlayer(player);
            hudManager.addPlayer(player);
        }

        hudManager.startUpdateTask();
        getLogger().info("MinecraftHUD enabled.");
    }

    @Override
    public void onDisable() {
        if (hudManager != null) {
            hudManager.cleanup();
        }
        if (playerDataManager != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                playerDataManager.savePlayer(player);
            }
        }
        getLogger().info("MinecraftHUD disabled.");
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
        }
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
}
