package com.example.hud.command;

import com.example.hud.HudPlugin;
import com.example.hud.manager.HudManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            sender.sendMessage("§eUsage: /hud <reload|show|hide|test>");
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

            // Direct diagnostic: bypasses the update task entirely.
            // Sends action bar + boss bar immediately so you can see if the API works at all.
            case "test" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cOnly players can use this.");
                    return true;
                }

                // 1) Action bar — plain text, no custom fonts needed
                player.sendActionBar(
                        Component.text("§b[MANA] 99.999.999   §c[HP] 10   §e99.999.999 [EXP]")
                );

                // 2) Boss bar at top of screen
                BossBar testBar = BossBar.bossBar(
                        Component.text("§a== HUD TEST == 20.0000000 / 20.0000000", NamedTextColor.WHITE),
                        1.0f,
                        BossBar.Color.RED,
                        BossBar.Overlay.PROGRESS
                );
                player.showBossBar(testBar);

                // Auto-hide boss bar after 5 seconds so it doesn't persist
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) player.hideBossBar(testBar);
                }, 100L);

                player.sendMessage("§a[MinecraftHUD] Test sent! Check:");
                player.sendMessage("§7 - Action bar: above your hotbar");
                player.sendMessage("§7 - Boss bar: top of screen (stays 5 sec)");
                plugin.getLogger().info("[MinecraftHUD] /hud test executed for " + player.getName());
            }

            default -> sender.sendMessage("§eUsage: /hud <reload|show|hide|test>");
        }
        return true;
    }
}
