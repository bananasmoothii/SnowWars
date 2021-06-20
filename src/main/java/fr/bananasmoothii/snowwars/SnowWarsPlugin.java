package fr.bananasmoothii.snowwars;

import com.sk89q.worldedit.WorldEditException;
import fr.bananasmoothii.snowwars.Config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
import java.util.Map;
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

    public static @Nullable SnowWarsGame mainSnowWarsGame;

    @Nullable BukkitTask startCountDownTask;

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
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly an in-game player can do that");
                    return false;
                }
                Player player = (Player) sender;
                if (mainSnowWarsGame==null || !mainSnowWarsGame.getPlayers().contains(player)) {
                    sender.sendMessage("§cYou can't do that.");
                }
                mainSnowWarsGame.removePlayer(player);
                player.teleport(Config.mainSpawn);
                player.setGameMode(GameMode.ADVENTURE);
                if (player.isOnline() && Config.quitCommand != null) {
                    player.performCommand(Config.quitCommand);
                }
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
                if (Config.maps.isEmpty()) {
                    sender.sendMessage("§cYou need to add maps first via §o/snowwars addmap");
                    return false;
                }
                final @Nullable String mapName;
                if (args.length >= 2) {
                    if (hasNoPerm(sender, "snowwars.choosemap")) return true;
                    mapName = getMapNameFromArgs(args);
                } else mapName = null;
                final long softStartTime = System.currentTimeMillis();
                if (startCountDownTask != null) {
                    sender.sendMessage(Messages.alreadyStarted);
                    return true;
                }
                startCountDownTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                    for (Player p : mainSnowWarsGame.getPlayers()) {
                        p.sendMessage(Messages.getStartingIn(String.valueOf(10 - (System.currentTimeMillis() - softStartTime) / 1000)));
                    }
                }, 0, 20);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    startCountDownTask.cancel();
                    mainSnowWarsGame.start(sender, mapName);
                    startCountDownTask = null;
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
                if (Config.maps.isEmpty()) {
                    sender.sendMessage("§cYou need to add maps first via §o/snowwars addmap");
                    return false;
                }
                final @Nullable String mapName1;
                if (args.length >= 2) {
                    if (hasNoPerm(sender, "snowwars.choosemap")) return true;
                    mapName1 = getMapNameFromArgs(args);
                } else mapName1 = null;
                mainSnowWarsGame.start(sender, mapName1);
                sender.sendMessage("§aGame started.");
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
                    Config.mainSpawn = location;
                    Config.raw.put("main-spawn", Config.getStringLocation(location));
                    //noinspection ConstantConditions
                    Config.raw.put("main-spawn-world", location.getWorld().getName());
                    Config.refreshConfig();
                    sender.sendMessage("§aSet the main spawn and refreshed the config.");
                    return true;
                }
                sender.sendMessage("§cYou are not an entity with position");
                return false;
            case "addspawn":
                if (hasNoPerm(sender, "snowwars.addspawn")) return true;
                if (!(sender instanceof Entity)) {
                    sender.sendMessage("§cYou are not an entity with position");
                    return false;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cPlease specify the map name");
                    return false;
                }
                final String mapName2 = getMapNameFromArgs(args);
                SnowWarsMap specifiedMap = null;
                for (SnowWarsMap map: Config.maps) {
                    if (map.getName().equals(mapName2)) {
                        specifiedMap = map;
                        break;
                    }
                }
                if (specifiedMap == null) {
                    sender.sendMessage("§cThat map doesn't exist: " + mapName2);
                    return false;
                }
                Config.addSpawnLocation(((Entity) sender).getLocation(), specifiedMap);
                sender.sendMessage("§aAdded spawn location and refreshed the config.");
                return true;
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
            case "addmap":
                if (! (sender instanceof Player)) {
                    sender.sendMessage("Only an ingame player can do that");
                    return false;
                }
                if (hasNoPerm(sender, "snowwars.addmap")) return true;
                if (args.length < 2) {
                    sender.sendMessage("§cNot enough arguments, please give a name for that map");
                    return false;
                }
                return Config.addMap((Player) sender, getMapNameFromArgs(args));
            case "completemap":
                if (! (sender instanceof Player)) {
                    sender.sendMessage("Only an ingame player can do that");
                    return false;
                }
                if (hasNoPerm(sender, "snowwars.addmap")) return true;
                return Config.finishAddMap((Player) sender);
            case "refreshmap":
                if (hasNoPerm(sender, "snowwars.refreshmap")) return true;
                try {
                    //noinspection ConstantConditions
                    mainSnowWarsGame.getCurrentMap().refresh();
                    return true;
                } catch (WorldEditException e) {
                    sender.sendMessage("§cAn error occurred, see console for details");
                    e.printStackTrace();
                    return false;
                } catch (NullPointerException e) {
                    sender.sendMessage("§cPlease start the game first");
                }
                sender.sendMessage("§aSuccessfully refreshed the map");
                return true;
            case "deletemap":
                if (hasNoPerm(sender, "snowwars.deletemap")) return true;
                if (args.length < 2) {
                    sender.sendMessage("§cPlease specify the map name");
                    return false;
                }
                final String mapName3 = getMapNameFromArgs(args);
                boolean removed = Config.maps.removeIf((SnowWarsMap map) -> mapName3.equals(map.getName()));
                if (!removed) sender.sendMessage("§eThat map doesn't exist");
                else {
                    //noinspection unchecked
                    ((Map<String, Map<String, Object>>) Config.raw.get("maps")).remove(mapName3);
                    Config.refreshConfig();
                    sender.sendMessage("§aRemoved \"" + mapName3 + "\" and refreshed the config.");
                }
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

    private @NotNull static String getMapNameFromArgs(@NotNull String @NotNull [] args) {
        StringBuilder name = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            name.append(args[i]);
            name.append(' ');
        }
        name.setLength(name.length() - 1); // remove last ' '
        return name.toString();
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
            if (sender.hasPermission("snowwars.forcestart"))   results.add("forcestart");
            if (sender.hasPermission("snowwars.stop"))         results.add("stop");
            if (sender.hasPermission("snowwars.addspawn"))     results.add("addspawn");
            if (sender.hasPermission("snowwars.setmainspawn")) results.add("setmainspawn");
            if (sender.hasPermission("snowwars.reload"))       results.add("reload");
            if (sender.hasPermission("snowwars.addmap"))       {results.add("addmap"); results.add("completemap");}
            if (sender.hasPermission("snowwars.deletemap"))    results.add("deletemap");
            if (sender.hasPermission("snowwars.iceevent"))     results.add("iceevent");
            if (sender.hasPermission("snowwars.refreshmap"))   results.add("refreshmap");
            if (sender.hasPermission("snowwars.addlive"))      results.add("addlive");
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
            } else if ((args[0].equalsIgnoreCase("addspawn") && sender.hasPermission("snowwars.addspawn"))
                    || (args[0].equalsIgnoreCase("deletemap") && sender.hasPermission("snowwars.deletemap"))
                    || (args[0].equalsIgnoreCase("start") && sender.hasPermission("snowwars.choosemap"))
                    || (args[0].equalsIgnoreCase("forcestart") && sender.hasPermission("snowwars.forcestart") && sender.hasPermission("snowwars.choosemap"))) {
                for (SnowWarsMap map: Config.maps) {
                    results.add(map.getName());
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
