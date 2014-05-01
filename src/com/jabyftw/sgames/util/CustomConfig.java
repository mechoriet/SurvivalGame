package com.jabyftw.sgames.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * @editor Rafael
 */
public class CustomConfig {

    private final Plugin pl;
    private final String name;
    private final File file;
    private FileConfiguration fileConfig;

    public CustomConfig(Plugin pl, String name) {
        this.pl = pl;
        this.name = name;
        file = new File(pl.getDataFolder(), name + ".yml");
    }

    public FileConfiguration getConfig() {
        if(fileConfig == null) {
            reloadConfig();
        }
        return fileConfig;
    }

    private void reloadConfig() {
        fileConfig = YamlConfiguration.loadConfiguration(file);
        InputStream defConfigStream = pl.getResource(name + ".yml");
        if(defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            fileConfig.setDefaults(defConfig);
        }
    }

    public void saveConfig() {
        try {
            getConfig().options().copyDefaults(true);
            getConfig().save(file);
            reloadConfig();
        } catch(IOException ex) {
            pl.getLogger().log(Level.WARNING, "Couldn't save {0}.yml", name);
        }
    }
}
