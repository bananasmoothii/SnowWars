package fr.bananasmoothii.snowwars;

import fr.bananasmoothii.snowwars.Config.Messages;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;
import net.minecraft.server.v1_16_R3.NBTTagString;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.*;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SnowWarsGame {

    public static final ArrayList<SnowWarsGame> instances = new ArrayList<>();

    private final Map<Player, PlayerData> players = new HashMap<>();
    private boolean started = false;
    private int startLives = Config.lives;
    private Scoreboard scoreboard;

    public static class PlayerData {
        private int lives;
        private Location spawnLocation;
        private boolean isGhost = false;

        public PlayerData(int startLives, Location spawnLocation) {
            this.lives = startLives;
            this.spawnLocation = spawnLocation;
        }

        public PlayerData(int startLives) {
            this.lives = startLives;
        }

        public int getLives() {
            return lives;
        }

        public void setLives(int lives) {
            this.lives = lives;
        }

        public Location getSpawnLocation() {
            return spawnLocation;
        }

        public void setSpawnLocation(Location spawnLocation) {
            this.spawnLocation = spawnLocation;
        }

        public boolean isGhost() {
            return isGhost;
        }

        public boolean isPermanentDeath() {
            return lives == 0;
        }
    }

    public SnowWarsGame() {
        instances.add(this);
    }

    public int getStartLives() {
        return startLives;
    }

    public void setStartLives(int startLives) {
        this.startLives = startLives;
    }

    public PlayerData getData(Player player) {
        return players.get(player);
    }

    public void addPlayer(Player player) {
        if (! players.containsKey(player)) {
            players.put(player, new PlayerData(startLives));
            player.sendMessage(Messages.join);
        }
        else {
            player.sendMessage(Messages.alreadyJoined);
        }
        player.teleport(Config.location);
    }

    public void removePlayer(Player player) {
        players.remove(player);
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        player.teleport(Config.location);
        if (started) checkForStop();
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
        Bukkit.getScheduler().runTask(SnowWarsPlugin.inst(), () -> {
            setNewRecipes();
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = scoreboard.registerNewObjective("lives-left", "dummy", Messages.livesLeft);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            shuffledSpawns = new ArrayList<>(Config.spawnLocations);
            Collections.shuffle(shuffledSpawns);
            for (Player player : players.keySet()) {
                player.setScoreboard(scoreboard);
                Location loc = nextSpawnLocation();
                player.teleport(loc);
                players.get(player).spawnLocation = loc;
                player.setGameMode(GameMode.ADVENTURE);
                if (Config.clearInventory)
                    player.getInventory().clear();
                player.setHealth(20);
                player.setFoodLevel(20);
                giveStartKit(player);
                asyncFilterInventory(player.getInventory());
                player.setAllowFlight(false);
            }
            updateScoreBoard();
            started = true;
        });
    }

    public void updateScoreBoard() {
        Objective objective = scoreboard.getObjective("lives-left");
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        for (Map.Entry<Player, PlayerData> entry: players.entrySet()) {
            Score score = objective.getScore(entry.getKey().getDisplayName());
            score.setScore(entry.getValue().lives);
            //entry.getKey().setScoreboard(scoreboard);
        }
    }

    private static void giveStartKit(Player player) {
        for (String string: Config.startSet) {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                    "minecraft:give " + player.getName() + ' ' + string);
        }
    }

    public void stop() {
        if (! started) throw new IllegalStateException("you cannot stop a game that hasn't started");
        Bukkit.getScheduler().runTask(SnowWarsPlugin.inst(), SnowWarsGame::setOldRecipes);
        Bukkit.getScheduler().runTask(SnowWarsPlugin.inst(), () -> {
            Player winner = null;
            for (Map.Entry<Player, PlayerData> entry: players.entrySet()) {
                if (!entry.getValue().isPermanentDeath()) {
                    winner = entry.getKey();
                    break;
                }
            }
            String winnerName = winner == null ? "<error>" : winner.getDisplayName();
            for (Player player: players.keySet()) {
                player.sendTitle(Messages.getPlayerWon(winnerName), null, 20, 120, 40);
                player.setGameMode(GameMode.ADVENTURE);
                player.setAllowFlight(true);
            }
            if (winner != null) {
                final Player finalWinner = winner;
                final int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(SnowWarsPlugin.inst(), () -> {
                    Location loc = finalWinner.getLocation();
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                            "minecraft:execute at " + finalWinner.getName() + " run summon firework_rocket ~ ~ ~"
                                    + " {LifeTime:20,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:0,Trail:1,Colors:[I;4312372,14602026],FadeColors:[I;11743532,15435844]}],Flight:1}}}}");
                }, 5, 10);
                Bukkit.getScheduler().runTaskLater(SnowWarsPlugin.inst(), () -> {
                    Bukkit.getScheduler().cancelTask(task);
                    scoreboard.getObjective("lives-left").unregister();
                }, 300);
            }
            for (Player player: players.keySet()) {
                player.teleport(Config.location);
                player.setAllowFlight(false);
            }
        });
        started = false;
    }

    public Set<Player> getPlayers() {
        return players.keySet();
    }

    public void playerDied(PlayerDeathEvent playerDeathEvent) {
        final Player deadPlayer = playerDeathEvent.getEntity();
        final PlayerData playerData = players.get(deadPlayer);
        Bukkit.getScheduler().runTaskLater(SnowWarsPlugin.inst(), () -> {
            if (players.containsKey(deadPlayer)) {
                deadPlayer.spigot().respawn();
                playerData.isGhost = true;
                deadPlayer.teleport(playerData.spawnLocation);
                deadPlayer.setGameMode(GameMode.SPECTATOR);
                int lives = --players.get(deadPlayer).lives;
                int remainingPLayers = 0;
                for (PlayerData data : players.values()) {
                    if (data.lives > 0) remainingPLayers++;
                }
                updateScoreBoard();

                for (Player playingPlayer : players.keySet()) {
                    playingPlayer.sendMessage(Messages.getPlayerDiedOrKilledBroadcast(deadPlayer.getDisplayName(),
                            String.valueOf(remainingPLayers),
                            String.valueOf(playerData.lives),
                            playerDeathEvent.getEntity().getKiller()));
                }

                if (lives <= 0) {
                    deadPlayer.sendTitle(Messages.getPlayerDiedTitle(String.valueOf(lives)),
                            Messages.playerDiedForeverSubtitle,
                            20, 160, 40);
                    checkForStop(remainingPLayers);
                } else {
                    deadPlayer.sendTitle(Messages.getPlayerDiedTitle(String.valueOf(lives)),
                            Messages.getPlayerDiedSubtitle(String.valueOf(lives), Config.respawnDelay + "s"),
                            20, 140, 40);

                    Bukkit.getScheduler().runTaskLater(SnowWarsPlugin.inst(), () -> {
                        deadPlayer.teleport(playerData.spawnLocation);
                        deadPlayer.setGameMode(GameMode.ADVENTURE);
                        deadPlayer.sendMessage(Messages.youResuscitated);
                        playerData.isGhost = false;
                        if (Config.giveAtRespawn) giveStartKit(deadPlayer);
                        asyncFilterInventory(deadPlayer.getInventory());
                    }, Config.respawnDelay * 20L);
                }
            }
        }, 1);
    }

    public void checkForStop() {
        int remainingPLayers = 0;
        for (PlayerData data : players.values()) {
            if (data.lives > 0) remainingPLayers++;
        }
        if (remainingPLayers <= 1 && started) stop();
    }

    public void checkForStop(int remainingPLayers) {
        if (remainingPLayers <= 1 && started) stop();
    }

    private int i = 0;
    private List<Location> shuffledSpawns;
    private Location nextSpawnLocation() {
        Location location = shuffledSpawns.get(i);
        if (i == shuffledSpawns.size() - 1) i = 0;
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
            ItemStack result = filterItemStack(recipe.getResult());
            if (result.getType() == Material.SNOW_BLOCK)
                result.setAmount(Config.craftedSnowAmount);

            if (recipe instanceof ShapelessRecipe) {
                newRecipe = new ShapelessRecipe(key(((Keyed) recipe).getKey().getKey()), result);
                ShapelessRecipe copy = (ShapelessRecipe) newRecipe;
                ShapelessRecipe oldCopy = (ShapelessRecipe) recipe;
                for (ItemStack oldItemStack : oldCopy.getIngredientList()) {
                    copy.addIngredient(1, oldItemStack.getType());
                }
                copy.setGroup(oldCopy.getGroup());
            } else if (recipe instanceof ShapedRecipe) {
                newRecipe = new ShapedRecipe(key(((Keyed) recipe).getKey().getKey()), result);
                ShapedRecipe copy = (ShapedRecipe) newRecipe;
                ShapedRecipe oldCopy = (ShapedRecipe) recipe;
                copy.shape(oldCopy.getShape());
                for (Map.Entry<Character, ItemStack> entry: oldCopy.getIngredientMap().entrySet()) {
                    if (entry.getValue() != null)
                        copy.setIngredient(entry.getKey(), entry.getValue().getType());
                }
                copy.setGroup(oldCopy.getGroup());
            } else if (recipe instanceof SmithingRecipe) {
                newRecipe = new SmithingRecipe(key(((Keyed) recipe).getKey().getKey()), result,
                        ((SmithingRecipe) recipe).getBase(), ((SmithingRecipe) recipe).getAddition());
            } else continue;

            newRecipes.add(newRecipe);
        }
    }

    private static void setNewRecipes() {
        Server server = Bukkit.getServer();
        for (Recipe recipe : oldRecipes) {
            server.removeRecipe(((Keyed) recipe).getKey());
        }
        for (Recipe recipe : newRecipes) {
            server.addRecipe(recipe);
        }
    }

    private static void setOldRecipes() {
        Server server = Bukkit.getServer();
        for (Recipe recipe: newRecipes) {
            server.removeRecipe(((Keyed) recipe).getKey());
        }
        for (Recipe recipe: oldRecipes) {
            server.addRecipe(recipe);
        }
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(SnowWarsPlugin.inst(), name);
    }

    public static void asyncFilterInventory(Inventory inventory) {
        Bukkit.getScheduler().runTaskAsynchronously(SnowWarsPlugin.inst(), () -> filterInventory(inventory));
    }

    public static void filterInventory(Inventory inventory) {
        int i = 0;
        for (ItemStack itemStack: inventory.getContents()) {
            if (itemStack != null) {
                inventory.setItem(i, filterItemStack(itemStack));
                i++;
            }
        }
    }

    public static ItemStack filterItemStack(@NotNull ItemStack itemStack) {
        if (itemStack.getType() != Material.SNOW_BLOCK && ! Config.itemsAbleToBreakSnow.contains(itemStack.getType()))
            return itemStack;
        net.minecraft.server.v1_16_R3.ItemStack nmsItemStack = CraftItemStack.asNMSCopy(itemStack);
        NBTTagCompound compound = nmsItemStack.hasTag() ? nmsItemStack.getTag() : new NBTTagCompound();
        NBTTagList tagList = new NBTTagList();
        if (itemStack.getType() == Material.SNOW_BLOCK) {
            for (String material : Config.canPlaceSnowOnStrings) {
                tagList.add(NBTTagString.a(material));
            }
            //noinspection ConstantConditions
            compound.set("CanPlaceOn", tagList);
        } else {
            tagList.add(NBTTagString.a("snow_block"));
            tagList.add(NBTTagString.a("snow"));
            //noinspection ConstantConditions
            compound.set("CanDestroy", tagList);
        }
        nmsItemStack.setTag(compound);
        return CraftItemStack.asBukkitCopy(nmsItemStack);
    }
}
