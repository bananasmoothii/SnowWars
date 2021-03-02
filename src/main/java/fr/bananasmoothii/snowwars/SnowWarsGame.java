package fr.bananasmoothii.snowwars;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.*;

public class SnowWarsGame {

    public static final ArrayList<SnowWarsGame> instances = new ArrayList<>();

    private final List<Player> players = new ArrayList<>();
    private final Map<Player, Integer> playerLives = new HashMap<>();
    private boolean started = false;
    private int startLives = Config.lives;

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
        players.add(player);
        player.teleport(Config.location);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public void addPlayer(Collection<? extends Player> playerList) {
        players.addAll(playerList);
    }

    public boolean isStarted() {
        return started;
    }

    public void start() {
        for (Player player: players) {
            playerLives.put(player, startLives);
            player.teleport(nextSpawnLocation());
        }
        started = true;
    }

    public void stop() {
        started = false;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void playerDied(PlayerDeathEvent playerDeathEvent) {
        Player player = playerDeathEvent.getEntity();
        if (playerLives.containsKey(player)) {
            int lives = playerLives.get(player) - 1;
            if (lives == 0) {
                player.setGameMode(GameMode.SPECTATOR);
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
}
