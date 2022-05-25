package fr.bananasmoothii.snowwars;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTList;
import fr.bananasmoothii.snowwars.Config.Messages;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SnowWarsGame {

    private final Map<Player, PlayerData> players = new HashMap<>();
    private boolean started;
    private int startLives = Config.lives;
    private Scoreboard scoreboard;
    private @Nullable BukkitTask iceEventTask;
    private @Nullable SnowWarsMap currentMap;
    @Nullable BukkitTask startCountDownTask;
    /**
     * {@link SnowWarsGame} does not manage this, {@link PluginListener#onPlayerMoveEvent(PlayerMoveEvent)} does.
     * SnowWarsGame just reads that map.
     */
    public final Map<Player, SnowWarsMap> votingPlayers = new HashMap<>();

    public static class PlayerData {
        private int lives;
        private Location spawnLocation;
        private boolean isGhost;
        private long lastRespawnTime;

        public Snowballs snowballs = new Snowballs();
        /** Height at which the player started falling */
        public double fallingFrom;
        public boolean isFalling;

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
            return lives <= 0;
        }

        protected void justRespawned() {
            lastRespawnTime = System.currentTimeMillis();
        }

        public long getLastRespawnTime() {
            return lastRespawnTime;
        }
    }

    @SuppressWarnings("ConstantConditions")
    public SnowWarsGame() {
        replaceFromBlocks.add(BlockTypes.STRUCTURE_VOID.getDefaultState().toBaseBlock());
        replaceToBlock = BlockTypes.ICE.getDefaultState().toBaseBlock();
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

    public @Nullable SnowWarsMap getCurrentMap() {
        return currentMap;
    }

    public Set<Player> getPlayers() {
        return players.keySet();
    }

    /**
     * @return an unmodifiable map of players and their {@link PlayerData}
     */
    public Map<Player, PlayerData> getPlayersAndData() {
        return Collections.unmodifiableMap(players);
    }

    public void addPlayer(Player player) {
        boolean setSpectator = false;
        if (! players.containsKey(player)) {
            if (! started) {
                players.put(player, new PlayerData(startLives));
                SnowWarsPlugin.sendMessage(player, Messages.join);
            } else {
                PlayerData data = new PlayerData(0);
                data.isGhost = true;
                data.spawnLocation = nextSpawnLocation();
                players.put(player, data);
                SnowWarsPlugin.sendMessage(player, Messages.alreadyStartedSpectator);
                player.setScoreboard(scoreboard);
                setSpectator = true;
                updateScoreBoard();
            }
        }
        else {
            if (! started)
                SnowWarsPlugin.sendMessage(player, Messages.alreadyJoined);
            else {
                SnowWarsPlugin.sendMessage(player, Messages.alreadyStarted);
                return;
            }
        }
        player.teleport(Config.mainSpawn);
        player.setGameMode(GameMode.ADVENTURE);
        if (Config.clearInventory)
            player.getInventory().clear();
        if (setSpectator)
            player.setGameMode(GameMode.SPECTATOR);
    }

    public void removePlayer(Player player) {
        players.remove(player);
        if (started) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            updateScoreBoard();
            checkForStop();
        }
    }

    public void addPlayer(@NotNull Iterable<? extends Player> playerList) {
        for (Player player: playerList) {
            addPlayer(player);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public void start(final @Nullable CommandSender source, final @Nullable String mapName, final boolean startCountdown) {
        final long softStartTime = System.currentTimeMillis();
        if (startCountdown) {
            startCountDownTask = Bukkit.getScheduler().runTaskTimerAsynchronously(SnowWarsPlugin.inst(), () -> {
                for (Player p : getPlayers()) {
                    SnowWarsPlugin.sendMessage(p, Messages.getStartingIn(String.valueOf(10 - (System.currentTimeMillis() - softStartTime) / 1000)));
                }
            }, 0, 20);
        }
        // choosing and refreshing map
        try {
            currentMap = chooseMap(mapName);
        } catch (UnableToStartException e) {
            e.printStackTrace();
            if (source != null)
                e.showFullMinecraftMessageTo(source);
            if (startCountDownTask != null) startCountDownTask.cancel();
            startCountDownTask = null;
            return;
        }
        currentMap.asyncRefreshAndCatchExceptions();
        votingPlayers.clear();
        Bukkit.getScheduler().runTaskLater(SnowWarsPlugin.inst(), () -> {
            try {
                if (startCountDownTask != null) {
                    startCountDownTask.cancel();
                    startCountDownTask = null;
                }
                if (currentMap.getDifferentSpawns() < players.size()) {
                    currentMap = chooseMap(null);
                    currentMap.refresh();
                }
                syncStart();
            } catch (UnableToStartException e) {
                e.printStackTrace();
                if (source != null)
                    e.showFullMinecraftMessageTo(source);
            }
        }, startCountdown ? 200 : 0);
    }

    /**
     * This is not refreshing the map!
     */
    private void syncStart() throws UnableToStartException {
        setNewRecipes();
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.getObjective("lives-left");
        if (objective != null) objective.unregister();
        objective = scoreboard.registerNewObjective("lives-left", "dummy", Messages.livesLeft);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        shuffledSpawns = new ArrayList<>(currentMap.getSpawnLocations());
        Collections.shuffle(shuffledSpawns);
        i = 0;
        for (Map.Entry<Player, PlayerData> entry : players.entrySet()) {
            Player player = entry.getKey();
            PlayerData data = entry.getValue();
            data.lives = startLives;
            data.isGhost = false;
            data.justRespawned();
            player.setScoreboard(scoreboard);
            player.setFallDistance(0f);
            player.setFireTicks(0);
            player.setHealth(20);
            player.setFoodLevel(20);
            Location loc = nextSpawnLocation();
            player.teleport(loc);
            players.get(player).spawnLocation = loc;
            player.setGameMode(GameMode.ADVENTURE);
            if (Config.clearInventory)
                player.getInventory().clear();
            asyncFilterInventory(player.getInventory());
            giveStartKit(player);
            player.setAllowFlight(false);
            player.playSound(loc, Sound.BLOCK_PORTAL_TRAVEL, 0.1f, 0.8f);
            player.setGameMode(GameMode.ADVENTURE);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Config.saturationDurationTicks, Config.saturationLevel - 1, false, false, true));
            player.sendTitle(Messages.startTitle, Messages.getStartSubtitle(currentMap.getName()), 10, 80, 20);
        }
        started = true;
        updateScoreBoard();
        iceEventTask = Bukkit.getScheduler().runTaskTimer(SnowWarsPlugin.inst(), this::iceEvent,
                Config.iceEventDelay * 20L, Config.iceEventDelay * 20L);
    }

    private @NotNull SnowWarsMap chooseMap(@Nullable String mapName) throws UnableToStartException {
        if (Config.maps.isEmpty()) throw new UnableToStartException("There are no defined maps, please run §n/snowwars addmap");
        ArrayList<SnowWarsMap> possibleMaps = new ArrayList<>();
        @Nullable SnowWarsMap chosenMap = null;
        if (mapName != null) {
            for (SnowWarsMap map : Config.maps) {
                if (mapName.equals(map.getName())) {
                    if (map.getDifferentSpawns() == 0)
                        throw new UnableToStartException("That map has no spawn locations. Please set spawn locations with §n/snowwars addspawn");
                    chosenMap = map;
                    break;
                }
            }
            if (chosenMap == null) {
                throw new UnableToStartException("That map doesn't exist: " + mapName);
            }
        } else {
            int totalPlayers = players.size();

            HashMap<SnowWarsMap, Integer> mapVoting = new HashMap<>();
            int minimumVotesToWin = totalPlayers / 2 + 1;
            @Nullable SnowWarsMap votedMap = null;
            for (SnowWarsMap map : votingPlayers.values()) {
                Integer votes = mapVoting.get(map);
                if (votes == null) votes = 1;
                else votes++;
                if (votes >= minimumVotesToWin) {
                    votedMap = map;
                    break;
                }
                mapVoting.put(map, votes);
            }

            if (votedMap != null && votedMap.getDifferentSpawns() >= totalPlayers) {
                chosenMap = votedMap;
            }
            else {
                if (votedMap != null && votedMap.getDifferentSpawns() < totalPlayers) {
                    for (Player player : players.keySet()) {
                        SnowWarsPlugin.sendMessage(player, Messages.getNotEnoughSpawnPoints(votedMap.getName(), String.valueOf(totalPlayers)));
                    }
                }

                // reverse sorting
                Config.maps.sort((map1, map2) -> map2.getDifferentSpawns() - map1.getDifferentSpawns());
                for (SnowWarsMap map : Config.maps) {
                    if (map.getDifferentSpawns() >= totalPlayers) {
                        possibleMaps.add(map);
                    }
                    else break; // the others will be below totalPlayers
                }
                if (possibleMaps.isEmpty()) {
                    if (Config.maps.isEmpty()) {
                        throw new UnableToStartException("Please define first at least one map with §n/snowwars addspawn");
                    }
                    chosenMap = Config.maps.get(Config.maps.size() - 1);
                    if (chosenMap.getDifferentSpawns() == 0) {
                        throw new UnableToStartException("Found no usable map with more than 0 spawns, please run §n/snowwars addspawn§c to add spawn locations");
                    }
                } else {
                    chosenMap = possibleMaps.get(ThreadLocalRandom.current().nextInt(possibleMaps.size()));
                }
            }
        }
        return chosenMap;
    }

    /**
     * Replaces all structure voids to ice
     * @throws NullPointerException if currentMap is null (the game didn't start)
     */
    public void iceEvent() {
        if (currentMap == null) throw new NullPointerException("There is no current map");
        if (currentMap.getPlaySpawn() == null || currentMap.getPlaySpawn().getWorld() == null) throw new NullPointerException("This is not a valid map");
        final long startTime = System.currentTimeMillis();
        final BossBar bossBar = Bukkit.getServer().createBossBar(
                Messages.getBossBar(String.valueOf(Config.iceEventKeep)),
                BarColor.BLUE, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        for (Player player: getPlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 0.4f, 1.5f);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.85f, 0.5f);
            bossBar.addPlayer(player);
        }
        final BukkitTask countDownTask = Bukkit.getScheduler().runTaskTimer(SnowWarsPlugin.inst(), () -> {
            // in seconds
            double timeRemaining = Config.iceEventKeep - (System.currentTimeMillis() - startTime) / 1000d;
            if (timeRemaining < 0) timeRemaining = 0d;
            bossBar.setTitle(Messages.getBossBar(String.valueOf(Math.ceil(timeRemaining * 10) / 10)));
            bossBar.setProgress(timeRemaining / Config.iceEventKeep);
            if (timeRemaining < 5)
                bossBar.setColor(BarColor.RED);
        }, 1, 1);
        Bukkit.getScheduler().runTask(SnowWarsPlugin.inst(), () -> {
            try (final EditSession replaceEditSession = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(currentMap.getPlaySpawn().getWorld())).fastMode(false).build()) {
                replaceEditSession.replaceBlocks(currentMap.getPlayRegion(), replaceFromBlocks, replaceToBlock);

                Bukkit.getScheduler().runTaskLater(SnowWarsPlugin.inst(), () -> {
                    countDownTask.cancel();
                    for (Player player: getPlayers()) {
                        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.1f, 0.5f);
                    }
                    bossBar.removeAll();
                    try (EditSession undoES = WorldEdit.getInstance().newEditSessionBuilder()
                            .world(BukkitAdapter.adapt(currentMap.getPlaySpawn().getWorld())).fastMode(false).build()) {
                        replaceEditSession.undo(undoES);
                    }
                }, Config.iceEventKeep * 20L);
            } catch (MaxChangedBlocksException e) {
                e.printStackTrace();
            }
        });
    }

    private final Set<BaseBlock> replaceFromBlocks = new HashSet<>();
    private final BaseBlock replaceToBlock;

    @SuppressWarnings("ConstantConditions")
    public void updateScoreBoard() {
        if (started) {
            Objective objective = scoreboard.getObjective("lives-left");
            for (String entry : scoreboard.getEntries()) {
                scoreboard.resetScores(entry);
            }
            for (Map.Entry<Player, PlayerData> entry : players.entrySet()) {
                Score score = objective.getScore(entry.getKey().getDisplayName());
                score.setScore(entry.getValue().lives);
            }
        }
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private static void giveStartKit(Player player) {
        for (String string: Config.startSet) {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                    "minecraft:give " + player.getName() + ' ' + string);
        }
    }

    public void stop() {
        if (! started) throw new IllegalStateException("you cannot stop a game that hasn't started");
        if (iceEventTask != null) {
            iceEventTask.cancel();
            iceEventTask = null;
        }
        Bukkit.getScheduler().runTask(SnowWarsPlugin.inst(), SnowWarsGame::setOldRecipes);
        Player winner = null;
        @SuppressWarnings("TypeMayBeWeakened")
        ArrayList<Player> sortedPlayers = new ArrayList<>(players.keySet());
        sortedPlayers.sort(Comparator.comparingInt((Player p) -> players.get(p).lives));
        for (Player player: sortedPlayers) {
            if (!players.get(player).isPermanentDeath()) {
                winner = player;
                break;
            }
        }
        String winnerName = winner == null ? Messages.defaultWinner : winner.getDisplayName();
        for (Player player: players.keySet()) {
            player.sendTitle(Messages.getPlayerWon(winnerName), null, 20, 120, 40);
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
        }
        int task = 0;
        if (winner != null) {
            final Player finalWinner = winner;
            task = Bukkit.getScheduler().scheduleSyncRepeatingTask(SnowWarsPlugin.inst(),
                    () -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                        "minecraft:execute at " + finalWinner.getName() + " run summon firework_rocket ~ ~3 ~"
                            + " {LifeTime:20,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:0,Trail:1,Colors:[I;4312372,14602026],FadeColors:[I;11743532,15435844]}],Flight:1}}}}"), 5, 10);
        }
        final Player finalWinner1 = winner;
        final int finalTask = task;
        Bukkit.getScheduler().runTaskLater(SnowWarsPlugin.inst(), () -> {
            if (finalWinner1 != null) Bukkit.getScheduler().cancelTask(finalTask);
            Objective objective = scoreboard.getObjective("lives-left");
            if (objective != null) objective.unregister();
        }, 300);
        for (Map.Entry<Player, PlayerData> entry: players.entrySet()) {
            entry.getKey().teleport(Config.mainSpawn);
            entry.getKey().setAllowFlight(false);
            entry.getValue().lives = Config.lives;
            entry.getValue().isGhost = false;
        }
        started = false;
    }

    public void playerDied(@NotNull PlayerDeathEvent playerDeathEvent) {
        final Player deadPlayer = playerDeathEvent.getEntity();
        final PlayerData playerData = players.get(deadPlayer);
        if (! players.containsKey(deadPlayer)) return;
        Bukkit.getScheduler().runTaskLater(SnowWarsPlugin.inst(), () -> {
            CustomLogger.info("(debug) Player " + deadPlayer.getName() + " died, started=" + started);
            if (started) {
                Location deathLocation = deadPlayer.getLocation();
                deadPlayer.spigot().respawn();
                playerData.isGhost = true;
                deadPlayer.setGameMode(GameMode.SPECTATOR);
                deadPlayer.teleport(deathLocation.add(0, 3, 0));
                int lives = --players.get(deadPlayer).lives;
                int remainingPLayers = 0;
                for (PlayerData data : players.values()) {
                    if (data.lives > 0) remainingPLayers++;
                }
                updateScoreBoard();

                EntityDamageEvent last = playerDeathEvent.getEntity().getLastDamageCause();
                @Nullable String killer = null;
                if (last instanceof EntityDamageByEntityEvent) {
                    Entity damager = ((EntityDamageByEntityEvent) last).getDamager();
                    if (damager instanceof Player) {
                        killer = ((Player) damager).getDisplayName();
                    } else {
                        killer = damager.getCustomName();
                        if (killer == null) killer = damager.getName();
                    }
                }
                String message = Messages.getPlayerDiedOrKilledBroadcast(deadPlayer.getDisplayName(),
                        String.valueOf(remainingPLayers),
                        String.valueOf(playerData.lives),
                        killer);
                for (Player playingPlayer : players.keySet()) {
                    SnowWarsPlugin.sendMessage(playingPlayer, message);
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

                    Bukkit.getScheduler().runTaskLater(SnowWarsPlugin.inst(),
                            () -> respawnPlayer(deadPlayer), Config.respawnDelay * 20L);
                }
            } else {
                deadPlayer.spigot().respawn();
                deadPlayer.teleport(Config.mainSpawn);
            }
        }, 1);
    }

    public void respawnPlayer(Player player) {
        PlayerData playerData = players.get(player);
        Location spawnLocation = playerData.spawnLocation;
        player.teleport(spawnLocation);
        CustomLogger.info("(debug) Player " + player.getName() + " respawned at " + player.getLocation() + " (spawnLocation=" + spawnLocation + ")");
        player.setGameMode(GameMode.ADVENTURE);
        SnowWarsPlugin.sendMessage(player, Messages.youResuscitated);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Config.saturationDurationTicks, Config.saturationLevel - 1, false, false, true));
        playerData.isGhost = false;
        if (Config.giveAtRespawn) giveStartKit(player);
        asyncFilterInventory(player.getInventory());
        player.setLastDamageCause(null);
        for (int j = 1; j <= Config.spawnSafetyCheck; j++) {
            Block block = player.getWorld().getBlockAt(player.getLocation().subtract(0, j, 0));
            if (block.getType() == Material.AIR) {
                if (j == Config.spawnSafetyCheck) { // end of checking
                    block.setType(Config.spawnSafetyBlock, false);
                    CustomLogger.info("(debug) Spawn safety block placed at " + block.getLocation() + " for " + player.getName() + " that has now " + playerData.lives + " lives left.");
                    break;
                }
            } else break;
        }
        player.playSound(spawnLocation, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.8f, 1f);
        playerData.justRespawned();
    }

    public void checkForStop() {
        int remainingPLayers = 0;
        for (PlayerData data : players.values()) {
            if (data.lives > 0) remainingPLayers++;
        }
        if (remainingPLayers <= 1 && started) stop();
    }

    public void checkForStop(int remainingPLayers) {
        if (remainingPLayers <= 1 && started)
            Bukkit.getScheduler().runTask(SnowWarsPlugin.inst(), this::stop);
    }

    private int i;
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

            if (recipe instanceof ShapelessRecipe oldCopy) {
                newRecipe = new ShapelessRecipe(key(((Keyed) recipe).getKey().getKey()), result);
                ShapelessRecipe copy = (ShapelessRecipe) newRecipe;
                for (ItemStack oldItemStack : oldCopy.getIngredientList()) {
                    copy.addIngredient(1, oldItemStack.getType());
                }
                copy.setGroup(oldCopy.getGroup());
            } else if (recipe instanceof ShapedRecipe oldCopy) {
                newRecipe = new ShapedRecipe(key(((Keyed) recipe).getKey().getKey()), result);
                ShapedRecipe copy = (ShapedRecipe) newRecipe;
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

    @Contract("_ -> new")
    private static @NotNull NamespacedKey key(String name) {
        return new NamespacedKey(SnowWarsPlugin.inst(), name);
    }

    public static void asyncFilterInventory(Inventory inventory) {
        Bukkit.getScheduler().runTaskAsynchronously(SnowWarsPlugin.inst(), () -> filterInventory(inventory));
    }

    public static void filterInventory(@NotNull Inventory inventory) {
        if (inventory.isEmpty()) return;
        int i = 0;
        for (ItemStack itemStack: inventory.getContents()) {
            if (itemStack != null) inventory.setItem(i, filterItemStack(itemStack));
            i++;
        }
    }

    public static ItemStack filterItemStack(@NotNull ItemStack itemStack) {
        if (itemStack.getType() != Material.SNOW_BLOCK && ! Config.itemsAbleToBreakSnow.contains(itemStack.getType()))
            return itemStack;
        NBTItem nbtItem = new NBTItem(itemStack);
        if (itemStack.getType() == Material.SNOW_BLOCK) {
            nbtItem.getStringList("CanPlaceOn").addAll(Config.canPlaceSnowOnStrings);
        } else {
            NBTList<String> canBreak = nbtItem.getStringList("CanDestroy");
            canBreak.add("minecraft:snow");
            canBreak.add("minecraft:snow_block");
            canBreak.add("minecraft:powder_snow");
        }
        return nbtItem.getItem();
    }

    public static class UnableToStartException extends Exception {
        private final @NotNull String minecraftMessage;

        public UnableToStartException(String message) {
            super(message);
            minecraftMessage = "§c" + message;
        }

        public @NotNull String getFullMinecraftMessage() {
            return "§cCould not start the game: " + minecraftMessage;
        }

        public void showFullMinecraftMessageTo(CommandSender player) {
            SnowWarsPlugin.sendMessage(player, getFullMinecraftMessage());
        }

        public @NotNull String getMinecraftMessage() {
            return minecraftMessage;
        }
    }
}
