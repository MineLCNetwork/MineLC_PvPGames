package net.minelc.minelc_pvpgames;

import net.minelc.minelc_pvpgames.listeners.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public final class MineLC_PvPGames extends JavaPlugin {
    public static MineLC_PvPGames instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        // Plugin startup logic
        Bukkit.getLogger().info("** Cargando el plugin MineLC_PvPGames... **");
        Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


    public void loadConfigInUTF() {
        File configFile = new File(this.getDataFolder(), "config.yml");
        if(!configFile.exists()) {
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), "UTF-8"));
            this.getConfig().load(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getIntConfig(String key, int defaultInt) {
        FileConfiguration config = this.getConfig();
        if(config.contains(key)) {
            if(config.isInt(key)) {
                return config.getInt(key);
            }
        }

        return defaultInt;
    }

    public String getStringConfig(String key, String defaultString) {
        FileConfiguration config = this.getConfig();
        if(config.contains(key)) {
            if(config.isString(key)) {
                return config.getString(key);
            }
        }

        return defaultString;
    }

    public boolean getBooleanConfig(String key, boolean defaultBool) {
        FileConfiguration config = this.getConfig();
        if(config.contains(key)) {
            if(config.isBoolean(key)) {
                return config.getBoolean(key);
            }
        }

        return defaultBool;
    }

    public static MineLC_PvPGames getInstance() {
        return instance;
    }
}
