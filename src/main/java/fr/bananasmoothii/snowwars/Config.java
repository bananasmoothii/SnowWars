package fr.bananasmoothii.snowwars;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public abstract class Config {

    public static Map<String, Object> raw = null;
    public static List<Material> canPlaceSnowOn = new ArrayList<>();
    public static List<Material> itemsAbleToBreakSnow = new ArrayList<>();
    public static int lives = 5;
    public static World world;
    public static Location location;
    public static List<Location> spawnLocations;

    public static class Messages {
        public static String playerDeath = null;
    }

    protected static Yaml yaml = new Yaml();

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
            probableCause = "canPlaceSnowOn";
            for (String material : (List<String>) raw.get("canPlaceSnowOn")) {
                probableCause = "canPlaceOnSnow." + material;
                Material givenMaterial = Material.getMaterial(material.toUpperCase());
                if (givenMaterial == null) throw new InvalidConfigException();
                canPlaceSnowOn.add(givenMaterial);
            }

            probableCause = "itemsAbleToBreakSnow";
            for (String material : (List<String>) raw.get("itemsAbleToBreakSnow")) {
                probableCause = "itemsAbleToBreakSnow." + material;
                Material givenMaterial = Material.getMaterial(material.toUpperCase());
                if (givenMaterial == null) throw new InvalidConfigException();
                itemsAbleToBreakSnow.add(givenMaterial);
            }

            probableCause = "lives";
            lives = (int) raw.get("lives");

            probableCause = "world";
            world = SnowWarsPlugin.inst().getServer().getWorld((String) raw.get("world"));

            probableCause = "location";
            location = getLocation(world, (String) raw.get("location"));

            probableCause = "spawn-locations";
            for (String loc: (List<String>) raw.get("spawn-locations")) {
                addSpawnLocation(getLocation(world, loc), false);
            }

        } catch (ClassCastException | InvalidConfigException | AssertionError | IllegalArgumentException e) {
            CustomLogger.severe("Error while loading the config ! This may be the cause : " + probableCause);
            e.printStackTrace();
        }

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
                configLocations.add(loc.getX() + " " + loc.getY() + " " + loc.getZ());
            }
            refreshConfig();
        }
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
