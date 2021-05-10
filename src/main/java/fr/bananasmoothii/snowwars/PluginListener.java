package fr.bananasmoothii.snowwars;

import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Random;
import java.util.Set;
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
        if (mainSnowWarsGame != null && mainSnowWarsGame.isStarted() && mainSnowWarsGame.getPlayers().contains(event.getEntity())) {
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
    }

    @EventHandler
    public void onProjectileHitEvent(ProjectileHitEvent event) {
        if (event.getEntity().getType() != EntityType.SNOWBALL || !(event.getHitEntity() instanceof Player)) return;
        Player hitPlayer = (Player) event.getHitEntity();
        Vector velocity = hitPlayer.getVelocity();
        ProjectileSource source = event.getEntity().getShooter();
        if (mainSnowWarsGame != null
                && ! mainSnowWarsGame.isStarted()
                && mainSnowWarsGame.getPlayers().contains(hitPlayer)) return;
        if (source instanceof LivingEntity) {
            velocity.add(Util.getVectorFromAToB(((LivingEntity) source).getLocation(), hitPlayer.getLocation()).normalize());
            hitPlayer.damage(0.5, (Entity) source);
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
        if (ThreadLocalRandom.current().nextInt((int)Config.inversedSnowballTntChance) == Config.inversedSnowballTntChance)
            hitPlayer.getWorld().createExplosion(hitPlayer.getLocation(), 2.6f, false);
    }

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

    @EventHandler
    public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
        String toName = event.getTo().getWorld().getName();
        if (toName.equals("snowwars") && ! event.getFrom().getWorld().getName().equals(toName)
                && (mainSnowWarsGame == null || ! mainSnowWarsGame.getPlayers().contains(event.getPlayer()))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Config.Messages.pleaseUseJoin);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        if (event.getPlayer().getWorld().getName().equals("snowwars")) {
            if (mainSnowWarsGame == null)
                mainSnowWarsGame = new SnowWarsGame();
            mainSnowWarsGame.addPlayer(event.getPlayer());
        }
    }
}
