package com.example.hud.command;

import com.example.hud.HudPlugin;
import com.example.hud.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ManaCommand implements CommandExecutor {

    private final HudPlugin plugin;
    private final PlayerDataManager playerData;

    public ManaCommand(HudPlugin plugin, PlayerDataManager playerData) {
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

        // /mana <set|add|remove|setmax> <player> <amount>
        if (args.length < 3) {
            sender.sendMessage("§eUsage: /mana <set|add|remove|setmax> <player> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer §f" + args[1] + "§c is not online.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number: §f" + args[2]);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set"    -> { playerData.setMana(target, amount);    sender.sendMessage("§aSet mana of §f" + target.getName() + "§a to §f" + amount); }
            case "add"    -> { playerData.addMana(target, amount);    sender.sendMessage("§aAdded §f" + amount + "§a mana to §f" + target.getName()); }
            case "remove" -> { playerData.removeMana(target, amount); sender.sendMessage("§aRemoved §f" + amount + "§a mana from §f" + target.getName()); }
            case "setmax" -> { playerData.setMaxMana(target, amount); sender.sendMessage("§aSet max mana of §f" + target.getName() + "§a to §f" + amount); }
            default       -> sender.sendMessage("§eUsage: /mana <set|add|remove|setmax> <player> <amount>");
        }
        return true;
    }
}
