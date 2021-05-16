package fr.bananasmoothii.snowwars;

import com.sk89q.worldedit.WorldEditException;
import fr.bananasmoothii.snowwars.Config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
        Config.load(this::saveDefaultConfig);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static SnowWarsPlugin inst() {
        return plugin;
    }

    public static SnowWarsGame mainSnowWarsGame = null;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || !command.getName().equalsIgnoreCase("snowwars")) {
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "join":
                if (mainSnowWarsGame == null) {
                    mainSnowWarsGame = new SnowWarsGame();
                }
                if (args.length >= 2 && ! args[1].isEmpty()) {
                    if (hasNoPerm(sender, "snowwars.join.others")) return true;
                    if (args[1].equals("*")) {
                        World world = sender instanceof Entity ? ((Entity) sender).getWorld() : Bukkit.getServer().getWorlds().get(0);
                        mainSnowWarsGame.addPlayer(world.getPlayers());
                    } else {
                        Player player = Bukkit.getServer().getPlayer(args[1]);
                        if (player == null) {
                            sender.sendMessage("§cThat player doesn't exist");
                            return true;
                        }
                        mainSnowWarsGame.addPlayer(player);
                    }
                } else {
                    if (sender instanceof Player) {
                        mainSnowWarsGame.addPlayer((Player) sender);
                    }
                    else
                        sender.sendMessage("You can't do that");
                }
                return true;
            case "quit":
                if (mainSnowWarsGame==null || !mainSnowWarsGame.getPlayers().contains(sender)) {
                    sender.sendMessage("§cYou can't do that.");
                }
                mainSnowWarsGame.removePlayer((Player) sender);
                sender.sendMessage(Messages.quit);
                return true;
            case "start":
                if (mainSnowWarsGame == null || mainSnowWarsGame.getPlayers().size() < 2) {
                    sender.sendMessage(Messages.notEnoughPlayers);
                    return true;
                }
                if (mainSnowWarsGame.isStarted()) {
                    sender.sendMessage(Messages.alreadyStarted);
                    return true;
                }
                final long softStartTime = System.currentTimeMillis();
                BukkitTask countDownTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                    for (Player player : mainSnowWarsGame.getPlayers()) {
                        player.sendMessage(Messages.getStartingIn(String.valueOf(10 - (System.currentTimeMillis() - softStartTime) / 1000)));
                    }
                }, 0, 20);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    countDownTask.cancel();
                    mainSnowWarsGame.start();
                }, 200);
                return true;
            case "forcestart":
                if (hasNoPerm(sender, "snowwars.forcestart")) return true;
                if (mainSnowWarsGame == null) {
                    sender.sendMessage("§cNo game to start");
                    return false;
                }
                if (mainSnowWarsGame.isStarted()) {
                    sender.sendMessage(Messages.alreadyStarted);
                    return false;
                }
                if (Config.spawnLocations.isEmpty()) {
                    sender.sendMessage("§cYou need to add spawn locations first via §o/snowwars addspawn");
                } else {
                    mainSnowWarsGame.start();
                    sender.sendMessage("§aGame started.");
                }
                return true;
            case "stop":
                if (hasNoPerm(sender, "snowwars.stop")) return true;
                if (mainSnowWarsGame == null) {
                    sender.sendMessage("§cNo game to stop");
                    return false;
                }
                if (!mainSnowWarsGame.isStarted()) {
                    sender.sendMessage("§cThe game didn't start");
                    return false;
                }
                mainSnowWarsGame.stop();
                sender.sendMessage("§aGame stopped.");
                return true;
            case "setmainspawn":
                if (hasNoPerm(sender, "snowwars.setmainspawn")) return true;
                if (sender instanceof Entity) {
                    Location location = ((Entity) sender).getLocation();
                    World world = ((Entity) sender).getWorld();
                    Config.location = location;
                    Config.world = world;
                    Config.raw.put("location", Config.getStringLocation(location));
                    Config.raw.put("world", world.getName());
                    Config.refreshConfig();
                    sender.sendMessage("§aSet the main spawn and refreshed the config.");
                    return true;
                }
                sender.sendMessage("§cYou are not an entity with position");
                return false;
            case "addspawn":
                if (hasNoPerm(sender, "snowwars.addspawn")) return true;
                if (sender instanceof Entity) {
                    Config.addSpawnLocation(((Entity) sender).getLocation());
                    sender.sendMessage("§aAdded spawn location and refreshed the config.");
                    return true;
                }
                sender.sendMessage("§cYou are not an entity with position");
                return false;
            case "reload":
                if (hasNoPerm(sender, "snowwars.reload")) return true;
                if (mainSnowWarsGame != null) {
                    if (mainSnowWarsGame.isStarted())
                        mainSnowWarsGame.stop();
                    mainSnowWarsGame = null;
                }
                Config.load(this::saveDefaultConfig);
                sender.sendMessage("§aConfig reloaded.");
                return true;
            case "setsource":
                if (! (sender instanceof Player)) {
                    sender.sendMessage("Only an ingame player can do that");
                    return false;
                }
                if (hasNoPerm(sender, "snowwars.setsource")) return true;
                return Config.setSource((Player) sender);
            case "refreshmap":
                if (hasNoPerm(sender, "snowwars.refreshmap")) return true;
                try {
                    SnowWarsGame.refreshMap();
                    return true;
                } catch (WorldEditException e) {
                    sender.sendMessage("§cAn error occurred, see console for details");
                    e.printStackTrace();
                    return false;
                } catch (NullPointerException e) {
                    sender.sendMessage("§cAn error occurred, you probably didn't set the source with /snowwars setsource");
                    e.printStackTrace();
                }
                sender.sendMessage("§aSuccessfully refreshed the map");
                return true;
            case "iceevent":
                if (hasNoPerm(sender, "snowwars.iceevent")) return true;
                if (mainSnowWarsGame == null || !mainSnowWarsGame.isStarted()) {
                    sender.sendMessage("§cNo game started");
                    return false;
                }
                mainSnowWarsGame.iceEvent();
                sender.sendMessage("§aTriggered ice event");
                return true;
            case "addlive":
                if (hasNoPerm(sender, "snowwars.addlive")) return true;
                if (mainSnowWarsGame == null || ! mainSnowWarsGame.isStarted()) {
                    sender.sendMessage("§cThis command needs the game to be started.");
                    return false;
                }
                try {
                    Player target = Objects.requireNonNull(Bukkit.getPlayer(args[1]));
                    int lives = Integer.parseInt(args[2]);
                    SnowWarsGame.PlayerData targetData = mainSnowWarsGame.getData(target);
                    int newLives = targetData.getLives();
                    targetData.setLives(newLives + lives);
                    mainSnowWarsGame.updateScoreBoard();
                    if (newLives + lives > 0 && newLives <= 0) { // to deal with cases where lives could be negative
                        mainSnowWarsGame.respawnPlayer(target);
                    } else if (newLives + lives <= 0) {
                        mainSnowWarsGame.checkForStop();
                    }
                    sender.sendMessage("§aGave " + args[2] + " lives to " + args[1]);
                    return true;
                } catch (NullPointerException | IndexOutOfBoundsException e) {
                    sender.sendMessage("§cMissing arguments: §r§n/snowwars addlive <player> <lives>");
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cNot a valid number: " + args[2]);
                }
                return false;
            default:
                return false;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("snowwars")) return null;
        List<String> results = new ArrayList<>();
        if (args.length == 1) {
            results.add("join");
            results.add("quit");
            results.add("start");
            if (sender.hasPermission("snowwars.forcestart")) results.add("forcestart");
            if (sender.hasPermission("snowwars.stop")) results.add("stop");
            if (sender.hasPermission("snowwars.addspawn")) results.add("addspawn");
            if (sender.hasPermission("snowwars.setmainspawn")) results.add("setmainspawn");
            if (sender.hasPermission("snowwars.reload")) results.add("reload");
            if (sender.hasPermission("snowwars.setsource")) results.add("setsource");
            if (sender.hasPermission("snowwars.iceevent")) results.add("iceevent");
            if (sender.hasPermission("snowwars.refreshmap")) results.add("refreshmap");
            if (sender.hasPermission("snowwars.addlive")) results.add("addlive");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("join") && sender.hasPermission("snowwars.join.others")) {
                results.add("*");
                for (Player online: Bukkit.getOnlinePlayers()) {
                    results.add(online.getName());
                }
            } else if (args[0].equalsIgnoreCase("addlive") && sender.hasPermission("snowwars.addlive")
                    && mainSnowWarsGame != null && mainSnowWarsGame.isStarted()) {
                for (Player playing: mainSnowWarsGame.getPlayers()) {
                    results.add(playing.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("addlive") && sender.hasPermission("snowwars.addlive")
                && mainSnowWarsGame != null && mainSnowWarsGame.isStarted()) {
            results.add("-1");
            results.add("1");
        }
        return results;
    }

    public static boolean hasNoPerm(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.isOp()) return false;
        sender.sendMessage(Messages.getNoPerm(permission));
        return true;
    }
}
