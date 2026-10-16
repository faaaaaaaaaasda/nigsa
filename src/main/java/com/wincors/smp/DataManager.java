package com.wincors.smp;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DataManager {
    public static final class PlayerData {
        public Origin origin;
        public int mana;
        public boolean weaponClaimed;
        public boolean initialized;
        public boolean vanished;
        public boolean pendingChoice;
    }

    private final WincorsSMP plugin;
    private final File file;
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private boolean endOpen;
    private String maceId;
    private boolean dirty;

    public DataManager(WincorsSMP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        endOpen = y.getBoolean("end-open", plugin.getConfig().getBoolean("end.default-open", false));
        maceId = y.getString("mace-id");
        ConfigurationSection s = y.getConfigurationSection("players");
        if (s != null) {
            for (String k : s.getKeys(false)) {
                try {
                    PlayerData d = new PlayerData();
                    d.origin = Origin.parse(s.getString(k + ".origin"));
                    d.mana = s.getInt(k + ".mana");
                    d.weaponClaimed = s.getBoolean(k + ".weapon-claimed");
                    d.initialized = s.getBoolean(k + ".initialized");
                    d.vanished = s.getBoolean(k + ".vanished");
                    d.pendingChoice = s.getBoolean(k + ".pending-choice");
                    players.put(UUID.fromString(k), d);
                } catch (IllegalArgumentException ignored) { }
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("end-open", endOpen);
        y.set("mace-id", maceId);
        for (Map.Entry<UUID, PlayerData> en : players.entrySet()) {
            String k = "players." + en.getKey();
            PlayerData d = en.getValue();
            y.set(k + ".origin", d.origin == null ? null : d.origin.name());
            y.set(k + ".mana", d.mana);
            y.set(k + ".weapon-claimed", d.weaponClaimed);
            y.set(k + ".initialized", d.initialized);
            y.set(k + ".vanished", d.vanished);
            y.set(k + ".pending-choice", d.pendingChoice);
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save data.yml: " + ex.getMessage());
        }
    }

    public void startAutoSave() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> { if (dirty) save(); }, 600L, 600L);
    }

    public void markDirty() { dirty = true; }

    public PlayerData get(UUID id) {
        return players.computeIfAbsent(id, k -> new PlayerData());
    }

    public Map<UUID, PlayerData> all() { return players; }

    public boolean isEndOpen() { return endOpen; }

    public void setEndOpen(boolean open) { this.endOpen = open; dirty = true; }

    public String getMaceId() { return maceId; }

    public void setMaceId(String id) { this.maceId = id; dirty = true; }
}
