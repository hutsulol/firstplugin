package com.example.hud.manager;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores and manages custom per-player stats: mana and gold.
 * Values are cached in memory while the player is online and
 * persisted via PersistentDataContainer on save/unload.
 */
public class PlayerDataManager {

    private final JavaPlugin plugin;

    private final NamespacedKey keyMana;
    private final NamespacedKey keyMaxMana;
    private final NamespacedKey keyGold;

    private final Map<UUID, Double> manaCache    = new HashMap<>();
    private final Map<UUID, Double> maxManaCache = new HashMap<>();
    private final Map<UUID, Long>   goldCache    = new HashMap<>();

    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin    = plugin;
        this.keyMana    = new NamespacedKey(plugin, "mana");
        this.keyMaxMana = new NamespacedKey(plugin, "max_mana");
        this.keyGold    = new NamespacedKey(plugin, "gold");
    }

    // ----------------------------------------------------------------
    //  Lifecycle
    // ----------------------------------------------------------------

    public void loadPlayer(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        UUID id = player.getUniqueId();

        double defaultMana    = plugin.getConfig().getDouble("defaults.mana",     99_999_999);
        double defaultMaxMana = plugin.getConfig().getDouble("defaults.max-mana", 99_999_999);
        long   defaultGold    = plugin.getConfig().getLong  ("defaults.gold",     99_999_999L);

        manaCache.put   (id, pdc.getOrDefault(keyMana,    PersistentDataType.DOUBLE, defaultMana));
        maxManaCache.put(id, pdc.getOrDefault(keyMaxMana, PersistentDataType.DOUBLE, defaultMaxMana));
        goldCache.put   (id, pdc.getOrDefault(keyGold,    PersistentDataType.LONG,   defaultGold));
    }

    public void savePlayer(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        UUID id = player.getUniqueId();

        if (manaCache.containsKey(id))    pdc.set(keyMana,    PersistentDataType.DOUBLE, manaCache.get(id));
        if (maxManaCache.containsKey(id)) pdc.set(keyMaxMana, PersistentDataType.DOUBLE, maxManaCache.get(id));
        if (goldCache.containsKey(id))    pdc.set(keyGold,    PersistentDataType.LONG,   goldCache.get(id));

        player.saveData();
    }

    public void unloadPlayer(UUID id) {
        manaCache.remove(id);
        maxManaCache.remove(id);
        goldCache.remove(id);
    }

    // ----------------------------------------------------------------
    //  Mana
    // ----------------------------------------------------------------

    public double getMana(Player player) {
        return manaCache.getOrDefault(player.getUniqueId(), 0.0);
    }

    public double getMaxMana(Player player) {
        return maxManaCache.getOrDefault(player.getUniqueId(), 100.0);
    }

    public void setMana(Player player, double value) {
        double clamped = Math.max(0, Math.min(value, getMaxMana(player)));
        manaCache.put(player.getUniqueId(), clamped);
    }

    public void addMana(Player player, double amount) {
        setMana(player, getMana(player) + amount);
    }

    public void removeMana(Player player, double amount) {
        setMana(player, getMana(player) - amount);
    }

    public void setMaxMana(Player player, double value) {
        maxManaCache.put(player.getUniqueId(), Math.max(0, value));
        // Clamp current mana if needed
        if (getMana(player) > value) setMana(player, value);
    }

    // ----------------------------------------------------------------
    //  Gold
    // ----------------------------------------------------------------

    public long getGold(Player player) {
        return goldCache.getOrDefault(player.getUniqueId(), 0L);
    }

    public void setGold(Player player, long value) {
        goldCache.put(player.getUniqueId(), Math.max(0, value));
    }

    public void addGold(Player player, long amount) {
        setGold(player, getGold(player) + amount);
    }

    public void removeGold(Player player, long amount) {
        setGold(player, getGold(player) - amount);
    }
}
