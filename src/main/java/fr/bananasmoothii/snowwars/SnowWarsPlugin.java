package fr.bananasmoothii.snowwars;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

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
        //noinspection ConstantConditions
        getCommand("snowwars").setExecutor(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static SnowWarsPlugin inst() {
        return plugin;
    }

    private SnowWarsGame mainSnowWarsGame = null;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            return false;
        }
        switch (args[0]) {
            case "join":
                if (mainSnowWarsGame == null) {
                    mainSnowWarsGame = new SnowWarsGame();
                }
                if (sender instanceof Player) {
                    mainSnowWarsGame.addPlayer((Player) sender);
                    return true;
                }
                break;
            case "start":
                if (mainSnowWarsGame == null) {
                    sender.sendMessage("No game to start");
                    return false;
                }
                mainSnowWarsGame.start();
            case "stop":
                if (mainSnowWarsGame == null) {
                    sender.sendMessage("No game to stop");
                    return false;
                }
                mainSnowWarsGame.stop();
                mainSnowWarsGame = null;
            case "addspawn":
                if (sender instanceof Entity) {
                    Config.addSpawnLocation(((Entity) sender).getLocation());
                    return true;
                }
                sender.sendMessage("You are not an entity with position");
        }
        return false;
    }
}
