package fr.bananasmoothii.snowwars;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unchecked")
public abstract class Config {

    public static Map<String, Object> raw;
    public static List<Material> canPlaceSnowOn;
    public static List<String> canPlaceSnowOnStrings;
    public static List<Material> itemsAbleToBreakSnow;
    public static List<String> itemsAbleToBreakSnowStrings;
    public static int lives = 5;
    public static Location mainSpawn;
    public static @Nullable String quitCommand;
    public static int respawnDelay;
    public static int craftedSnowAmount;
    public static double maxFallHeight;
    public static int snowBlockBreakInterval;
    public static int snowBlockBreakLimit;
    public static List<String> startSet;
    public static boolean giveAtRespawn;
    public static boolean clearInventory;
    public static double snowballKnockbackMultiplier;
    public static double snowballYAdd;
    public static double snowballMaxY;
    public static double inversedSnowballTntChance;
    public static float snowballTntPower;
    public static int iceEventDelay;
    public static int iceEventKeep;
    public static int respawnFreezeMillis;
    public static int saturationDurationTicks;
    public static ArrayList<SnowWarsMap> maps;

    public static class Messages {
        public static Map<String, String> raw;
        public static String playerDiedBroadcast, playerKilledBroadcast, playerDiedTitle, playerDiedSubtitle, playerDiedForeverSubtitle,
                noPerm, join, quit, alreadyJoined, youResuscitated, playerWon, alreadyStarted, alreadyStartedSpectator, livesLeft, bossBar,
                notEnoughPlayers, startingIn, pleaseUseJoin, pleaseUseQuit, defaultWinner, startTitle, startSubtitle;

        public static String getPlayerDiedOrKilledBroadcast(String player, String remaining, String lives, @Nullable String killer) {
            if (killer == null)
                return playerDiedBroadcast.replace("{player}", player).replace("{remaining}", remaining).replace("{lives}", lives);
            return playerKilledBroadcast.replace("{killer}", killer).replace("{victim}", player).replace("{remaining}", remaining).replace("{lives}", lives);
        }

        public static String getPlayerDiedTitle(String lives) {
            return playerDiedTitle.replace("{lives}", lives);
        }

        public static String getPlayerDiedSubtitle(String lives, String respawnTime) {
            return playerDiedSubtitle.replace("{lives}", lives).replace("{time}", respawnTime);
        }

        public static String getNoPerm(String perm) {
            return noPerm.replace("{perm}", perm);
        }

        public static String getPlayerWon(String player) {
            return playerWon.replace("{player}", player);
        }

        public static String getBossBar(String time) {
            return bossBar.replace("{time}", time);
        }

        public static String getStartingIn(String time) {
            return startingIn.replace("{time}", time);
        }

        public static String getStartSubtitle(String mapName) {
            return startSubtitle.replace("{map name}", mapName);
        }
    }

    static {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yaml = new Yaml(dumperOptions);
    }

    protected static Yaml yaml;

    public static void load(@Nullable Runnable createConfigRunnable) {
        InputStream inputStream;
        try {
            inputStream = new FileInputStream("plugins/SnowWars/config.yml");
        } catch (FileNotFoundException e) {
            if (createConfigRunnable != null) {
                createConfigRunnable.run();
                load(null);
            }
            else {
                CustomLogger.severe("Could not create the config");
            }
            return;
        }
        raw = yaml.load(inputStream);

        String probableCause = "no probable cause";
        try {
            probableCause = "can-place-snow-on";
            canPlaceSnowOn = new ArrayList<>();
            canPlaceSnowOnStrings = new ArrayList<>();
            for (String material : (List<String>) raw.get("can-place-snow-on")) {
                probableCause = "can-place-snow-on." + material;
                Material givenMaterial = Material.getMaterial(material.toUpperCase());
                if (givenMaterial == null) throw new InvalidConfigException(material + " doesn't exist");
                assert givenMaterial.isSolid() : "The block " + givenMaterial.name() + " is not solid";
                canPlaceSnowOn.add(givenMaterial);
                canPlaceSnowOnStrings.add(material.toLowerCase());
            }

            itemsAbleToBreakSnow = new ArrayList<>();
            itemsAbleToBreakSnowStrings = new ArrayList<>();
            probableCause = "items-able-to-break-snow";
            for (String material : (List<String>) raw.get("items-able-to-break-snow")) {
                probableCause = "items-able-to-break-snow." + material;
                Material givenMaterial = Material.getMaterial(material.toUpperCase());
                if (givenMaterial == null) throw new InvalidConfigException(material + " doesn't exist");
                itemsAbleToBreakSnow.add(givenMaterial);
                itemsAbleToBreakSnowStrings.add(material.toLowerCase());
            }

            probableCause = "lives";
            lives = (int) raw.get("lives");

            probableCause = "main-spawn or main-spawn-world";
            mainSpawn = getLocation(
                    Objects.requireNonNull(SnowWarsPlugin.inst().getServer().getWorld((String) raw.get("main-spawn-world"))),
                    (String) raw.get("main-spawn"));

            probableCause = "spawn-delay";
            respawnDelay = (int) raw.get("respawn-delay");

            probableCause = "quit-command";
            quitCommand = (String) raw.get("quit-command");

            probableCause = "crafted-snow-amount";
            craftedSnowAmount = (int) raw.get("crafted-snow-amount");
            PluginListener.snowBlockBreakMaxDrops = (int) Math.ceil(4.0 / craftedSnowAmount);

            probableCause = "max-fall-height";
            maxFallHeight = (double) (Double) raw.get("max-fall-height");

            probableCause = "snow-block-break-interval";
            snowBlockBreakInterval = (int) raw.get("snow-block-break-interval");

            probableCause = "snow-block-break-limit";
            snowBlockBreakLimit = (int) raw.get("snow-block-break-limit");

            probableCause = "start-set";
            startSet = (List<String>) raw.get("start-set");

            probableCause = "give-set-at-respawn";
            giveAtRespawn = (boolean) raw.get("give-set-at-respawn");

            probableCause = "clear-inventory";
            clearInventory = (boolean) raw.get("clear-inventory");

            probableCause = "respawn-freeze";
            respawnFreezeMillis = (int) ((double) raw.get("respawn-freeze") * 1000);

            probableCause = "snowball-knockback-multiplier";
            snowballKnockbackMultiplier = (double) raw.get("snowball-knockback-multiplier");

            probableCause = "snowball-y-add";
            snowballYAdd = (double) raw.get("snowball-y-add");

            probableCause = "snowball-max-y";
            snowballMaxY = (double) raw.get("snowball-max-y");

            probableCause = "snowball-tnt-chance";
            inversedSnowballTntChance = (double) raw.get("snowball-tnt-chance");
            assert inversedSnowballTntChance > 0 && inversedSnowballTntChance <= 1 : "snowball tnt chance must be between 0 exclusive and 1 inclusive";
            inversedSnowballTntChance = 1 / inversedSnowballTntChance;

            probableCause = "snowball-tnt-power";
            snowballTntPower = (float) (double) raw.get("snowball-tnt-power");

            probableCause = "ice-event-delay";
            iceEventDelay = (int) raw.get("ice-event-delay");

            probableCause = "ice-event-keep";
            iceEventKeep = (int) raw.get("ice-event-keep");

            probableCause = "saturation-duration";
            saturationDurationTicks = ((int) raw.get("saturation-duration")) * 20;

            probableCause = "messages";
            Messages.raw = (Map<String, String>) raw.get("messages");

            for (Field field: Messages.class.getDeclaredFields()) {
                probableCause = "messages." + field.getName();
                if (field.getType().equals(String.class))
                    field.set(null, Messages.raw.get(field.getName()));
            }

            probableCause = "maps";
            maps = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> entry: ((Map<String, Map<String, Object>>) raw.get("maps")).entrySet()) {
                probableCause = "maps." + entry.getKey();
                Map<String, Object> rawMap = entry.getValue();

                World sourceWorld = SnowWarsPlugin.inst().getServer().getWorld((String) rawMap.get("world"));
                if (sourceWorld == null) throw new NullPointerException("World" + rawMap.get("world") + " does not exist");

                World playWorld = SnowWarsPlugin.inst().getServer().getWorld((String) rawMap.get("play-world"));
                if (playWorld == null) throw new NullPointerException("World" + rawMap.get("play-world") + " does not exist");

                List<Location> spawns = new ArrayList<>(((List<String>) rawMap.get("spawns")).size());
                for (String stringSpawn: (List<String>) rawMap.get("spawns")) {
                    spawns.add(getLocation(playWorld, stringSpawn));
                }
                maps.add(new SnowWarsMap(entry.getKey(),
                        getLocation(sourceWorld, (String) rawMap.get("spawn")),
                        getLocation(playWorld, (String) rawMap.get("play-spawn")),
                        getLocation(playWorld, (String) rawMap.get("min")),
                        getLocation(playWorld, (String) rawMap.get("max")),
                        spawns));
            }

        } catch (ClassCastException | InvalidConfigException | AssertionError | IllegalArgumentException | IllegalAccessException | NullPointerException e) {
            CustomLogger.severe("Error while loading the config ! This is probably the cause : " + probableCause);
            e.printStackTrace();
        }

        SnowWarsGame.initRecipes();
    }

    public static Location getLocation(World world, String location) {
        String[] xyz = location.split(" ");
        if (xyz.length == 3)
            return new Location(world, Double.parseDouble(xyz[0]), Double.parseDouble(xyz[1]), Double.parseDouble(xyz[2]));
        if (xyz.length == 5)
            return new Location(world, Double.parseDouble(xyz[0]), Double.parseDouble(xyz[1]), Double.parseDouble(xyz[2]), Float.parseFloat(xyz[3]), Float.parseFloat(xyz[4]));
        throw new IllegalArgumentException(location + " is not valid");
    }

    public static class InvalidConfigException extends RuntimeException {
        public InvalidConfigException(String msg) {
            super(msg);
        }
    }

    public static void addSpawnLocation(Location location, SnowWarsMap map) {
        map.getSpawnLocations().add(location);
        List<String> configLocations = (List<String>) ((Map<String, Map<String, Object>>) raw.get("maps")).get(map.getName()).get("spawns");
        configLocations.add(getStringLocation(location));
        refreshConfig();
    }

    public static String getStringLocation(Location l) {
        if (l.getYaw() == 0 && l.getPitch() == 0) {
            return l.getBlockX() + " " + l.getBlockY() + " " + l.getBlockZ();
        }
        return l.getX() + " " + l.getY() + " " + l.getZ() + " " + l.getYaw() + " " + l.getPitch();
    }

    public static void refreshConfig() {
        try {
            FileWriter fileWriter = new FileWriter("plugins/SnowWars/config.yml");
            fileWriter.write("# GitHub: https://github.com/bananasmoothii/SnowWars\n" +
                    "# Discord: https://discord.gg/HNHfEJXwbs\n" +
                    "# WARNING: All comments below (except this header section) will be gone after the first command that refreshes the config, if you want to find\n" +
                    "# a new config you can delete this one or head to https://github.com/bananasmoothii/SnowWars/blob/master/src/main/resources/config.yml\n" +
                    "# WARNING 2: Make a backup of your world because the plugin will mess with player inventories, locations, gamemodes...\n\n");
            yaml.dump(raw, fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static @Nullable SnowWarsMap needsToSetPlaySpawn;

    public static boolean addMap(final Player player, final @NotNull String name) {
        Objects.requireNonNull(name);
        if (needsToSetPlaySpawn != null) {
            player.sendMessage("§cPlease set the spawn location for playing as said before with §n/snowwars completemap");
        }
        if (Config.maps.stream().anyMatch((SnowWarsMap map) -> map.getName().equals(name))) {
            player.sendMessage("§cError: there is already a map with that name. If you want to override it, please first run §n/snowwars deletemap");
            return false;
        }
        Region source;
        try {
            //noinspection ConstantConditions
            source = WorldEdit.getInstance().getSessionManager().findByName(player.getName()).getSelection();
        } catch (IncompleteRegionException | NullPointerException e) {
            player.sendMessage("§cUnbale to get your current selection");
            return false;
        }
        if (! (source instanceof CuboidRegion)) {
            player.sendMessage("§cYour selection is not cuboid");
            return false;
        }
        needsToSetPlaySpawn = new SnowWarsMap(name, player.getLocation(), (CuboidRegion) source);
        player.sendMessage("§aDone, now please set the spawn for playing, set via §n/snowwars completemap");
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean finishAddMap(Player player) {
        if (needsToSetPlaySpawn == null) {
            player.sendMessage("§cPlease run §n/snowwars addmap§c before.");
            return false;
        }
        needsToSetPlaySpawn.setPlaySpawn(player.getLocation());
        Config.maps.add(needsToSetPlaySpawn);
        World playWorld = needsToSetPlaySpawn.getPlaySpawn().getWorld();
        HashMap<String, Object> map = new HashMap<>();
        map.put("play-spawn", getStringLocation(needsToSetPlaySpawn.getPlaySpawn()));
        map.put("play-world", playWorld.getName());
        map.put("spawn", getStringLocation(needsToSetPlaySpawn.getSourceSpawn()));
        map.put("world", needsToSetPlaySpawn.getSourceSpawn().getWorld().getName());
        map.put("min", getStringLocation(Util.blockVector3ToLocation(needsToSetPlaySpawn.getSourceRegion().getMinimumPoint(), playWorld)));
        map.put("max", getStringLocation(Util.blockVector3ToLocation(needsToSetPlaySpawn.getSourceRegion().getMaximumPoint(), playWorld)));
        map.put("spawns", new ArrayList<String>());
        ((Map<String, Map<String, Object>>) raw.get("maps")).put(needsToSetPlaySpawn.getName(), map);
        refreshConfig();
        player.sendMessage("§aDone. Don't forget to add spawn locations with §n/snowwars addspawn " + needsToSetPlaySpawn.getName());
        needsToSetPlaySpawn.refresh();
        needsToSetPlaySpawn = null;
        return true;
    }
}
