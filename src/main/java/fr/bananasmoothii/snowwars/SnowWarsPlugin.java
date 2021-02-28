package fr.bananasmoothii.snowwars;

import org.bukkit.plugin.java.JavaPlugin;

public final class SnowWarsPlugin extends JavaPlugin {

    private static SnowWarsPlugin plugin;

    @Override
    public void onLoad() {
        plugin = this;
        CustomLogger.setLogger(getLogger());
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PluginListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static SnowWarsPlugin inst() {
        return plugin;
    }
}
