package fr.bananasmoothii.snowwars;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public abstract class Config {

    public static Map<String, Object> raw;
    public static List<Material> canPlaceSnowOn;
    public static List<String> canPlaceSnowOnStrings;
    public static List<Material> itemsAbleToBreakSnow;
    public static List<String> itemsAbleToBreakSnowStrings;
    public static int lives = 5;
    public static World world;
    public static Location location;
    public static List<Location> spawnLocations;
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

    public static class Messages {
        public static Map<String, String> raw;
        public static String playerDiedBroadcast, playerKilledBroadcast, playerDiedTitle, playerDiedSubtitle, playerDiedForeverSubtitle,
                noPerm, join, alreadyJoined, youResuscitated, playerWon, alreadyStarted;

        public static String getPlayerDiedOrKilledBroadcast(String player, String remaining, String lives, @Nullable Player killer) {
            if (killer == null)
                return playerDiedBroadcast.replace("{player}", player).replace("{remaining}", remaining).replace("{lives}", lives);
            return playerKilledBroadcast.replace("{killer}", killer.getDisplayName()).replace("{victim}", player).replace("{remaining}", remaining).replace("{lives}", lives);
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
                if (givenMaterial == null) throw new InvalidConfigException();
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
                if (givenMaterial == null) throw new InvalidConfigException();
                itemsAbleToBreakSnow.add(givenMaterial);
                itemsAbleToBreakSnowStrings.add(material.toLowerCase());
            }

            probableCause = "lives";
            lives = (int) raw.get("lives");

            probableCause = "world";
            world = SnowWarsPlugin.inst().getServer().getWorld((String) raw.get("world"));

            probableCause = "location";
            location = getLocation(world, (String) raw.get("location"));

            probableCause = "spawn-delay";
            respawnDelay = (int) raw.get("respawn-delay");

            probableCause = "spawn-locations";
            spawnLocations = new ArrayList<>();
            for (String loc: (List<String>) raw.get("spawn-locations")) {
                addSpawnLocation(getLocation(world, loc), false);
            }

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

            probableCause = "snowball-knockback-multiplier";
            snowballKnockbackMultiplier = (double) raw.get("snowball-knockback-multiplier");

            probableCause = "snowball-y-add";
            snowballYAdd = (double) raw.get("snowball-y-add");

            probableCause = "messages";
            Messages.raw = (Map<String, String>) raw.get("messages");

            for (Field field: Messages.class.getDeclaredFields()) {
                probableCause = "messages." + field.getName();
                if (field.getType().equals(String.class))
                    field.set(null, Messages.raw.get(field.getName()));
            }

        } catch (ClassCastException | InvalidConfigException | AssertionError | IllegalArgumentException | IllegalAccessException e) {
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
        public InvalidConfigException() {
            super();
        }
        public InvalidConfigException(String msg) {
            super(msg);
        }
    }

    protected static long lastRefresh = 0L;

    public static void addSpawnLocation(Location location) {
        addSpawnLocation(location, true);
    }

    private static void addSpawnLocation(Location location, boolean updateRaw) {
        spawnLocations.add(location);
        if (updateRaw) {
            List<String> configLocations = (List<String>) raw.get("spawn-locations");
            for (Location loc: spawnLocations) {
                configLocations.add(getStringLocation(loc));
            }
            refreshConfig();
        }
    }

    public static String getStringLocation(Location location) {
        return location.getX() + " " + location.getY() + " " + location.getZ() + " " + location.getYaw() + " " + location.getPitch();
    }

    public static void refreshConfig() {
        if (lastRefresh == 0L || System.currentTimeMillis() - lastRefresh > 20000) {
            try {
                yaml.dump(raw, new FileWriter("plugins/SnowWars/config.yml"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
