package com.example.hud.command;

import com.example.hud.HudPlugin;
import com.example.hud.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GoldCommand implements CommandExecutor {

    private final HudPlugin plugin;
    private final PlayerDataManager playerData;

    public GoldCommand(HudPlugin plugin, PlayerDataManager playerData) {
        this.plugin     = plugin;
        this.playerData = playerData;
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

        // /gold <set|add|remove> <player> <amount>
        if (args.length < 3) {
            sender.sendMessage("§eUsage: /gold <set|add|remove> <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer §f" + args[1] + "§c is not online.");
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number: §f" + args[2]);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set"    -> { playerData.setGold(target, amount);    sender.sendMessage("§aSet gold of §f" + target.getName() + "§a to §f" + amount); }
            case "add"    -> { playerData.addGold(target, amount);    sender.sendMessage("§aAdded §f" + amount + "§a gold to §f" + target.getName()); }
            case "remove" -> { playerData.removeGold(target, amount); sender.sendMessage("§aRemoved §f" + amount + "§a gold from §f" + target.getName()); }
            default       -> sender.sendMessage("§eUsage: /gold <set|add|remove> <player> <amount>");
        }
        return true;
    }
}
