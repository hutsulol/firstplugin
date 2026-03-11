package com.example.hud.manager;

import com.example.hud.HudPlugin;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the per-player HUD consisting of:
 *   - Action bar: [MANA_ICON] mana   [HEART_ICON] hearts   gold [EXP_ICON]
 *   - Boss bar:   ████████  20.0000000 / 20.0000000  ████████  (red, full width)
 *
 * Icons are read from config.yml so you can swap in your own Unicode characters
 * without recompiling.
 */
public class HudManager {

    private final HudPlugin plugin;
    private final PlayerDataManager playerData;

    /** One BossBar instance per online player. */
    private final Map<UUID, BossBar> healthBars = new HashMap<>();

    private BukkitTask updateTask;

    // Number formatters built once and reused
    private final DecimalFormat thousandsFormat; // 99.999.999
    private final DecimalFormat healthFormat;    // 20.0000000

    public HudManager(HudPlugin plugin, PlayerDataManager playerData) {
        this.plugin     = plugin;
        this.playerData = playerData;

        // Integers separated by dots:  99.999.999
        // Must use Locale.ROOT to avoid conflicts with system locale decimal separators
        DecimalFormatSymbols dotSep = new DecimalFormatSymbols(Locale.ROOT);
        dotSep.setGroupingSeparator('.');
        dotSep.setDecimalSeparator(','); // avoid collision: grouping='.' decimal=','
        this.thousandsFormat = new DecimalFormat("#,###", dotSep);

        // Health with 7 decimal places:  20.0000000
        DecimalFormatSymbols healthSep = new DecimalFormatSymbols(Locale.ROOT);
        healthSep.setDecimalSeparator('.');
        this.healthFormat = new DecimalFormat("0.0000000", healthSep);
    }

    // ----------------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------------

    public void startUpdateTask() {
        int interval = plugin.getConfig().getInt("update-interval", 2);
        plugin.getLogger().info("[MinecraftHUD] Starting update task (interval=" + interval + " ticks)");

        final long[] tickCount = {0};
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            tickCount[0]++;
            // Log a heartbeat every 5 seconds so we know the task is alive
            if (tickCount[0] % (100 / Math.max(1, interval)) == 0) {
                plugin.getLogger().info("[MinecraftHUD] Task alive, online players: "
                        + plugin.getServer().getOnlinePlayers().size());
            }
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                try {
                    updateHud(player);
                } catch (Throwable e) {
                    plugin.getLogger().severe("[MinecraftHUD] Error updating HUD for "
                            + player.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }, 0L, interval);
    }

    public void addPlayer(Player player) {
        if (healthBars.containsKey(player.getUniqueId())) return;

        BossBar.Color   color   = parseBossBarColor(plugin.getConfig().getString("health-bar.color",   "RED"));
        BossBar.Overlay overlay = parseBossBarOverlay(plugin.getConfig().getString("health-bar.overlay", "PROGRESS"));

        BossBar bar = BossBar.bossBar(Component.empty(), 1.0f, color, overlay);
        healthBars.put(player.getUniqueId(), bar);

        // Delay by 1 tick so the client is fully ready to receive the boss bar packet
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.showBossBar(bar);
            }
        }, 1L);
    }

    public void removePlayer(Player player) {
        BossBar bar = healthBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    public void cleanup() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            removePlayer(player);
        }
        healthBars.clear();
    }

    // ----------------------------------------------------------------
    //  HUD update
    // ----------------------------------------------------------------

    public void updateHud(Player player) {
        double health    = player.getHealth();
        double maxHealth = getMaxHealth(player);

        // Hearts displayed as integer ceiling (e.g. 2.0 hp → 1 heart)
        int hearts = (int) Math.ceil(health / 2.0);

        double mana = playerData.getMana(player);
        long   gold = playerData.getGold(player);

        updateActionBar(player, mana, hearts, gold);

        if (plugin.getConfig().getBoolean("health-bar.enabled", true)) {
            updateHealthBar(player, health, maxHealth);
        }
    }

    // ----------------------------------------------------------------
    //  Action bar:  [MANA_ICON] 99.999.999   [HEART_ICON] 1   99.999.999 [EXP_ICON]
    // ----------------------------------------------------------------

    private void updateActionBar(Player player, double mana, int hearts, long gold) {
        String manaIcon  = plugin.getConfig().getString("icons.mana",   "V");
        String heartIcon = plugin.getConfig().getString("icons.health", "<3");
        String expIcon   = plugin.getConfig().getString("icons.exp",    "*");

        TextColor manaColor  = hexColor(plugin.getConfig().getString("colors.mana",   "5599FF"));
        TextColor heartColor = hexColor(plugin.getConfig().getString("colors.health", "FF4444"));
        TextColor expColor   = hexColor(plugin.getConfig().getString("colors.exp",    "FFCC00"));

        String manaStr = thousandsFormat.format((long) mana);
        String goldStr = thousandsFormat.format(gold);

        Component actionBar = Component.text()
                // Mana section
                .append(Component.text(manaIcon + " ", manaColor))
                .append(Component.text(manaStr, manaColor))
                // Separator
                .append(Component.text("   "))
                // Health section
                .append(Component.text(heartIcon + " ", heartColor))
                .append(Component.text(String.valueOf(hearts), heartColor))
                // Separator
                .append(Component.text("   "))
                // Gold / exp section
                .append(Component.text(goldStr + " ", expColor))
                .append(Component.text(expIcon, expColor))
                .build();

        player.sendActionBar(actionBar);
    }

    // ----------------------------------------------------------------
    //  Boss bar health bar:  20.0000000 / 20.0000000
    // ----------------------------------------------------------------

    private void updateHealthBar(Player player, double health, double maxHealth) {
        BossBar bar = healthBars.get(player.getUniqueId());
        if (bar == null) return;

        // Re-show on every update to guarantee visibility
        player.showBossBar(bar);

        String text = healthFormat.format(health) + " / " + healthFormat.format(maxHealth);

        TextColor textColor = hexColor(plugin.getConfig().getString("colors.health-bar-text", "FFFFFF"));

        bar.name(Component.text(text, textColor));
        bar.progress((float) Math.max(0.0, Math.min(1.0, health / maxHealth)));
    }

    // ----------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------

    private double getMaxHealth(Player player) {
        // Try 1.21+ name first, then the legacy 1.20 name — resolved at runtime
        for (String name : new String[]{"MAX_HEALTH", "GENERIC_MAX_HEALTH"}) {
            try {
                Attribute attr = Attribute.valueOf(name);
                AttributeInstance inst = player.getAttribute(attr);
                if (inst != null) return inst.getValue();
            } catch (Throwable ignored) {}
        }
        return 20.0;
    }

    private static TextColor hexColor(String hex) {
        try {
            return TextColor.color(Integer.parseInt(hex.replace("#", ""), 16));
        } catch (NumberFormatException e) {
            return TextColor.color(0xFFFFFF);
        }
    }

    private static BossBar.Color parseBossBarColor(String name) {
        try {
            return BossBar.Color.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.RED;
        }
    }

    private static BossBar.Overlay parseBossBarOverlay(String name) {
        try {
            return BossBar.Overlay.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}
