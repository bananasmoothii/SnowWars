package fr.bananasmoothii.snowwars;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static fr.bananasmoothii.snowwars.SnowWarsPlugin.mainSnowWarsGame;

@SuppressWarnings({"MethodMayBeStatic", "unused"})
public class PluginListener implements Listener {

    public static int snowBlockBreakMaxDrops = 4;
    private static final Random random = new Random();

    @EventHandler
    public void onBlockBreakEvent(final BlockBreakEvent event) {
        Block block = event.getBlock();
        if (mainSnowWarsGame == null
                || ! mainSnowWarsGame.getPlayers().contains(event.getPlayer())
                || ! mainSnowWarsGame.isStarted()) return;
        if (block.getType() == Material.SNOW_BLOCK) {
            int amountToDrop = random.nextInt(snowBlockBreakMaxDrops);
            if (amountToDrop == 0)
                event.setDropItems(false);
        }
        if (Config.snowBlockBreakInterval != -1) {
            blockBreaker(block.getRelative(BlockFace.NORTH));
            blockBreaker(block.getRelative(BlockFace.EAST));
            blockBreaker(block.getRelative(BlockFace.SOUTH));
            blockBreaker(block.getRelative(BlockFace.WEST));
            blockBreaker(block.getRelative(BlockFace.UP));
            blockBreaker(block.getRelative(BlockFace.DOWN));
        }
    }

    private void blockBreaker(Block block) {
        if (block.getType() == Material.SNOW_BLOCK || block.getType() == Material.SNOW) {
            new BlockBreaker(block);
        }
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        if (mainSnowWarsGame != null && mainSnowWarsGame.getPlayers().contains(event.getPlayer())) {
            mainSnowWarsGame.removePlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        if (mainSnowWarsGame != null && mainSnowWarsGame.getPlayers().contains(event.getEntity())) {
            mainSnowWarsGame.playerDied(event);
        }
    }

    private static final HashMap<Player, Double> fallingPlayers = new HashMap<>(); // double is the height where the player started to fall

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (Config.maxFallHeight == 0
                || mainSnowWarsGame == null
                || ! mainSnowWarsGame.getPlayers().contains(player)
                || mainSnowWarsGame.getData(player).isGhost())
            return;

        if (mainSnowWarsGame.isStarted() && System.currentTimeMillis() - mainSnowWarsGame.getData(player).getLastRespawnTime() < Config.respawnFreezeMillis) {
            event.setCancelled(true);
            return;
        }

        Location to = event.getTo();
        //noinspection ConstantConditions
        if (! to.getWorld().getBlockAt(to.getBlockX(), to.getBlockY() -1, to.getBlockZ()).getType().isSolid()) {
            if (! fallingPlayers.containsKey(player))
                fallingPlayers.put(player, event.getTo().getY());
            else {
                double fallingDistance = fallingPlayers.get(player) - event.getTo().getY();
                if (fallingDistance > Config.maxFallHeight) {
                    player.setHealth(0.0); // kill the player
                    fallingPlayers.remove(player);
                } else if (fallingDistance < 0) {
                    fallingPlayers.put(player, event.getTo().getY());
                }
            }
        } else {
            fallingPlayers.remove(player);
        }

        if (!mainSnowWarsGame.isStarted() && mainSnowWarsGame.getPlayers().contains(player)) {
            for (SnowWarsMap snowWarsMap : Config.maps) {
                if (snowWarsMap.isVoting(player.getLocation())) {
                    // don't spam players at each movement, only when the vote changed
                    if (!snowWarsMap.equals(mainSnowWarsGame.votingPlayers.get(player))) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 80, 1, false, false, false));
                        // broadcast
                        for (Player snowWarsPlayer : mainSnowWarsGame.getPlayers()) {
                            SnowWarsPlugin.sendMessage(snowWarsPlayer, Config.Messages.getHasVoted(player.getDisplayName(), snowWarsMap.getName()));
                            snowWarsPlayer.playNote(player.getLocation(), Instrument.CHIME, new Note(18));
                        }
                        mainSnowWarsGame.votingPlayers.put(player, snowWarsMap);
                    }
                    break;
                }
            }
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @EventHandler
    public void onProjectileHitEvent(ProjectileHitEvent event) {
        if (event.getEntity().getType() != EntityType.SNOWBALL) return;
        // that doesn't care of velocity, if that makes an explosion then there is no velocity
        if (mainSnowWarsGame != null && mainSnowWarsGame.isStarted() && mainSnowWarsGame.getPlayers().contains(event.getEntity().getShooter())
                && ThreadLocalRandom.current().nextInt((int)Config.inversedSnowballTntChance) == 0) {
            event.getEntity().getWorld().createExplosion(event.getEntity().getLocation(), Config.snowballTntPower, false);
            return;
        }
        if (!(event.getHitEntity() instanceof Player)) return;
        Player hitPlayer = (Player) event.getHitEntity();
        Vector velocity = hitPlayer.getVelocity();
        ProjectileSource source = event.getEntity().getShooter();
        if (mainSnowWarsGame != null
                && ! mainSnowWarsGame.isStarted()
                && mainSnowWarsGame.getPlayers().contains(hitPlayer)) return;
        if (source instanceof LivingEntity) {
            Vector vectorFromAToB = Util.getVectorFromAToB(((LivingEntity) source).getLocation(), hitPlayer.getLocation());
            if (vectorFromAToB.length() != 0) {
                velocity.add(vectorFromAToB.normalize());
                hitPlayer.damage(0.5, (Entity) source);
            }
        } else if (source instanceof BlockProjectileSource) {
            Block blockSource = ((BlockProjectileSource) source).getBlock();
            velocity.add(Util.getVectorFromAToB(blockSource.getX(), blockSource.getY(), blockSource.getZ(), hitPlayer.getLocation()));
            hitPlayer.damage(1, event.getEntity());
        }
        velocity.normalize();
        velocity.setY(velocity.getY() + Config.snowballYAdd);
        if (velocity.getY() > Config.snowballMaxY) velocity.setY(Config.snowballMaxY);
        velocity.multiply(Config.snowballKnockbackMultiplier);
        hitPlayer.setVelocity(velocity);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (mainSnowWarsGame == null) return;
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        Set<Player> snowWarsGamePlayers = mainSnowWarsGame.getPlayers();
        if (damager instanceof Player
                && ! mainSnowWarsGame.isStarted()
                && snowWarsGamePlayers.contains(damager)
                && (victim.getType() == EntityType.SNOWMAN || snowWarsGamePlayers.contains(victim)))
            event.setCancelled(true);
    }

    @SuppressWarnings("ConstantConditions")
    @EventHandler
    public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
        String toName = event.getTo().getWorld().getName();
        if (! event.getFrom().getWorld().getName().equals(toName)) {
            if (toName.equals("snowwars") && (mainSnowWarsGame == null || !mainSnowWarsGame.getPlayers().contains(event.getPlayer()))
                    && ! event.getPlayer().hasPermission("snowwars.teleport")) {
                event.setCancelled(true);
                SnowWarsPlugin.sendMessage(event.getPlayer(), Config.Messages.pleaseUseJoin);
            } else if (!toName.equals("snowwars") && mainSnowWarsGame != null && mainSnowWarsGame.getPlayers().contains(event.getPlayer())
                    && ! event.getPlayer().hasPermission("snowwars.teleport")) {
                event.setCancelled(true);
                SnowWarsPlugin.sendMessage(event.getPlayer(), Config.Messages.pleaseUseQuit);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoinEvent(final PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(SnowWarsPlugin.inst(), () -> {
            if (event.getPlayer().getWorld().getName().equals("snowwars")) {
                if (mainSnowWarsGame == null)
                    mainSnowWarsGame = new SnowWarsGame();
                mainSnowWarsGame.addPlayer(event.getPlayer());
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileLaunchEvent(ProjectileLaunchEvent event) {
        //noinspection SuspiciousMethodCalls
        if (event.getEntity().getType() != EntityType.SNOWBALL
                || mainSnowWarsGame == null
                || !mainSnowWarsGame.isStarted()
                || !(event.getEntity().getShooter() instanceof Player)
                || !mainSnowWarsGame.getPlayers().contains(event.getEntity().getShooter())) return;

        Player player = (Player) event.getEntity().getShooter();
        if (! playerSnowballs.containsKey(player)) {
            playerSnowballs.put(player, new Snowballs());
            return;
        }
        if (playerSnowballs.get(player).snowballThrownTooFast()) {
            Location explosion = player.getLocation();
            explosion.add(Config.AntiCheat.punitionExplosionOffset * ThreadLocalRandom.current().nextInt(-1, 2),
                          Config.AntiCheat.punitionExplosionOffset * ThreadLocalRandom.current().nextInt(-1, 2),
                          Config.AntiCheat.punitionExplosionOffset * ThreadLocalRandom.current().nextInt(-1, 2));
            //noinspection ConstantConditions
            explosion.getWorld().createExplosion(explosion, Config.AntiCheat.punitionExplosionPower, true, true);
        }
    }

    public HashMap<Player, Snowballs> playerSnowballs = new HashMap<>();

    public static class Snowballs {
        private int snowballStrike;
        private long lastSnowball;

        private long[] snowballs = new long[Config.AntiCheat.snowballCheck];
        private int index;

        /**
         * Acts like a setter and a getter: it saves the snowball timestamp and
         * @return {@code true} if there is a problem with the rate and the player should be punished.
         *         If it returns true, all latest snowball timestamps are reset.
         */
        public boolean snowballThrownTooFast() {
            long time = System.currentTimeMillis();
            if (time - lastSnowball >= Config.AntiCheat.maxSnowballAge * 1000L) {
                reset();
                lastSnowball = time;
                snowballStrike = 1;
                addSnowball(time);
                return false;
            }
            lastSnowball = time;
            snowballStrike++;
            addSnowball(time);
            if (snowballStrike >= Config.AntiCheat.snowballCheck) {
                if (lastSnowball - getSnowballStrikeStart() <= Config.AntiCheat.minSnowballInterval * Config.AntiCheat.snowballCheck * 1000) {
                    reset();
                    return true;
                }
            }
            return false;
        }

        private void addSnowball(long time) {
            snowballs[index++] = time;
            if (index == snowballs.length) index = 0;
        }

        public long getSnowballStrikeStart() {
            return snowballs[index];
        }

        public void reset() {
            snowballStrike = 0;
            snowballs = new long[Config.AntiCheat.snowballCheck];
            index = 0;
        }
    }
}
