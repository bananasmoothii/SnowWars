package fr.bananasmoothii.snowwars;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public abstract class Config {

    public static Map<String, Object> raw = null;
    public static List<Material> canPlaceSnowOn = new ArrayList<>();
    public static List<Material> itemsAbleToBreakSnow = new ArrayList<>();
    public static int lives = 5;
    public static class Messages {
        public static String playerDeath = null;
    }

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
        Yaml yaml = new Yaml();
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

        } catch (ClassCastException | InvalidConfigException e) {
            CustomLogger.severe("Error while loading the config ! This may be the cause : " + probableCause);
            e.printStackTrace();
        }

    }

    public static class InvalidConfigException extends RuntimeException {
        public InvalidConfigException() {
            super();
        }
        public InvalidConfigException(String msg) {
            super(msg);
        }
    }
}
