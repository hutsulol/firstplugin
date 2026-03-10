package com.example.hud.command;

import com.example.hud.HudPlugin;
import com.example.hud.manager.HudManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HudCommand implements CommandExecutor {

    private final HudPlugin plugin;
    private final HudManager hudManager;

    public HudCommand(HudPlugin plugin, HudManager hudManager) {
        this.plugin     = plugin;
        this.hudManager = hudManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {

        if (!sender.hasPermission("hud.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /hud <reload|show|hide>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage("§aMinecraftHUD config reloaded.");
            }
            case "show" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                hudManager.addPlayer(player);
                sender.sendMessage("§aHUD shown.");
            }
            case "hide" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }
                hudManager.removePlayer(player);
                sender.sendMessage("§aHUD hidden.");
            }
            default -> sender.sendMessage("§eUsage: /hud <reload|show|hide>");
        }
        return true;
    }
}
