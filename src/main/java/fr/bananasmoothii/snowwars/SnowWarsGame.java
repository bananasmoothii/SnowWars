package fr.bananasmoothii.snowwars;

import fr.bananasmoothii.snowwars.Config.Messages;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.security.Key;
import java.util.*;

public class SnowWarsGame {

    public static final ArrayList<SnowWarsGame> instances = new ArrayList<>();

    private final List<Player> players = new ArrayList<>();
    private final Map<Player, Integer> playerLives = new HashMap<>();
    private boolean started = false;
    private int startLives = Config.lives;
    private HashMap<Player, Location> spawnLocations = new HashMap<>();

    public SnowWarsGame() {
        instances.add(this);
    }

    public int getStartLives() {
        return startLives;
    }

    public void setStartLives(int startLives) {
        this.startLives = startLives;
    }

    public Integer getLivesRemaining(Player player) {
        return playerLives.get(player);
    }

    public void addPlayer(Player player) {
        if (! players.contains(player)) {
            players.add(player);
            player.sendMessage(Messages.join);
        }
        else {
            player.sendMessage(Messages.alreadyJoined);
        }
        player.teleport(Config.location);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public void addPlayer(Collection<? extends Player> playerList) {
        for (Player player: playerList) {
            addPlayer(player);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public void start() {
        setNewRecipes();

        for (Player player: players) {
            playerLives.put(player, startLives);
            Location loc = nextSpawnLocation();
            player.teleport(loc);
            spawnLocations.put(player, loc);
            player.setGameMode(GameMode.ADVENTURE);
        }
        started = true;
    }

    public void stop() {
        started = false;
        setOldRecipes();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void playerDied(PlayerDeathEvent playerDeathEvent) {
        final Player player = playerDeathEvent.getEntity();
        if (playerLives.containsKey(player)) {
            int lives = playerLives.get(player) - 1;
            int remainingPLayers = lives == 0 ? players.size() - 1 : players.size();

            player.spigot().respawn();
            player.setGameMode(GameMode.SPECTATOR);

            for (Player playingPlayer: players) {
                playingPlayer.sendMessage(Messages.getPlayerDiedBroadcast(player.getDisplayName(), String.valueOf(remainingPLayers)));
            }

            if (lives == 0) {
                players.remove(player);
                player.sendTitle(Messages.getPlayerDiedTitle(String.valueOf(lives)),
                        Messages.playerDiedForeverSubtitle,
                        1, 6, 2);
            } else {
                player.sendTitle(Messages.getPlayerDiedTitle(String.valueOf(lives)),
                        Messages.getPlayerDiedSubtitle(String.valueOf(lives)),
                        1, 4, 2);
                Bukkit.getScheduler().runTaskLater(SnowWarsPlugin.inst(), () -> {
                    player.teleport(spawnLocations.get(player));
                    player.setGameMode(GameMode.ADVENTURE);
                    player.sendMessage(Messages.youResuscitated);
                }, Config.respawnDelay * 20L);
            }
        }

    }

    private int i = 0;
    private Location nextSpawnLocation() {
        Location location = Config.spawnLocations.get(i);
        if (i == Config.spawnLocations.size() - 1) i = 0;
        else i++;
        return location;
    }

    public static ArrayList<Recipe> newRecipes;
    public static ArrayList<Recipe> oldRecipes;

    public static void initRecipes() {
        Server server = Bukkit.getServer();
        oldRecipes = new ArrayList<>(Config.itemsAbleToBreakSnow.size() + 1);
        oldRecipes.addAll(server.getRecipesFor(new ItemStack(Material.SNOW_BLOCK)));
        for (Material material: Config.itemsAbleToBreakSnow) {
            oldRecipes.addAll(server.getRecipesFor(
                    new ItemStack(material)
            ));
        }
        newRecipes = new ArrayList<>(Config.itemsAbleToBreakSnow.size() + 1);
        for (Recipe recipe: oldRecipes) {
            Recipe newRecipe;
            ItemStack result = PluginListener.filterItemStack(recipe.getResult());
            if (result.getType() == Material.SNOW_BLOCK)
                result.setAmount(Config.craftedSnowAmount);

            if (recipe instanceof ShapelessRecipe) {
                newRecipe = new ShapelessRecipe(((Keyed) recipe).getKey(), result);
            } else if (recipe instanceof ShapedRecipe) {
                newRecipe = new ShapedRecipe(((Keyed) recipe).getKey(), result);
            } else continue;

            newRecipes.add(newRecipe);
        }
    }

    public static void setNewRecipes() {
        Server server = Bukkit.getServer();
        for (Recipe recipe : oldRecipes) {
            server.removeRecipe(((Keyed) recipe).getKey());
        }
        for (Recipe recipe : newRecipes) {
            server.addRecipe(recipe);
        }
    }

    public static void setOldRecipes() {
        Server server = Bukkit.getServer();
        for (Recipe recipe: newRecipes) {
            server.removeRecipe(((Keyed) recipe).getKey());
        }
        for (Recipe recipe: oldRecipes) {
            server.addRecipe(recipe);
        }
    }
}
