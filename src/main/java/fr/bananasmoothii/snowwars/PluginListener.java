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
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

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
        if (mainSnowWarsGame != null && mainSnowWarsGame.getPlayers().contains(event.getEntity())) {
            mainSnowWarsGame.playerDied(event);
        }
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        if (mainSnowWarsGame == null) return;
        Player player = event.getPlayer();
        Location to = event.getTo();

        if (!mainSnowWarsGame.getPlayers().contains(player)) return;

        if (mainSnowWarsGame.isStarted() && System.currentTimeMillis() - mainSnowWarsGame.getData(player).getLastRespawnTime() < Config.respawnFreezeMillis) {
            event.setCancelled(true);
            return;
        }

        if (Config.maxFallHeight > 0 && mainSnowWarsGame.getPlayers().contains(player)
                && player.getGameMode() == GameMode.ADVENTURE) {
            SnowWarsGame.PlayerData data = mainSnowWarsGame.getData(player);
            if (!to.getWorld().getBlockAt(to.getBlockX(), to.getBlockY() - 1, to.getBlockZ()).getType().isSolid()) {
                if (!data.isFalling) {
                    data.fallingFrom = to.getY();
                    data.isFalling = true;
                } else {
                    double fallingDistance = data.fallingFrom - to.getY();
                    if (fallingDistance > Config.maxFallHeight) {
                        player.setHealth(0.0); // kill the player
                        data.isFalling = false;
                    } else if (fallingDistance < 0) {
                        data.fallingFrom = to.getY();
                    }
                }
            } else {
                data.isFalling = false;
            }
        }

        if (!mainSnowWarsGame.isStarted() && mainSnowWarsGame.getPlayers().contains(player) && player.getGameMode() == GameMode.ADVENTURE) {
            for (SnowWarsMap snowWarsMap : Config.maps) {
                if (snowWarsMap.isVoting(player.getLocation())) {
                    // don't spam players at each movement, only when the vote changed
                    if (snowWarsMap != mainSnowWarsGame.votingPlayers.get(player)) {
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
        if (!(event.getHitEntity() instanceof Player hitPlayer)) return;
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

    @EventHandler(priority = EventPriority.HIGHEST)
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
        if (mainSnowWarsGame != null && mainSnowWarsGame.isStarted() && mainSnowWarsGame.getPlayers().contains(event.getPlayer())
                && event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE && !event.getPlayer().hasPermission("snowwars.teleport")) {
            event.setCancelled(true);
            SnowWarsPlugin.sendMessage(event.getPlayer(), Config.Messages.pleaseUseQuit);
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
                || !(event.getEntity().getShooter() instanceof Player player)
                || !mainSnowWarsGame.getPlayers().contains(event.getEntity().getShooter())) return;

        SnowWarsGame.PlayerData data = mainSnowWarsGame.getData(player);
        if (! player.hasPermission("snowwars.anticheat.bypass") && data.snowballs.snowballThrownTooFast()) {
            Location explosion = player.getLocation();
            explosion.add(Config.AntiCheat.punitionExplosionOffset * ThreadLocalRandom.current().nextInt(-1, 2),
                          Config.AntiCheat.punitionExplosionOffset * ThreadLocalRandom.current().nextInt(-1, 2),
                          Config.AntiCheat.punitionExplosionOffset * ThreadLocalRandom.current().nextInt(-1, 2));
            explosion.getWorld().createExplosion(explosion, Config.AntiCheat.punitionExplosionPower, true, true);
            Bukkit.getScheduler().runTaskAsynchronously(SnowWarsPlugin.inst(), () -> {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.hasPermission("snowwars.anticheat.notify"))
                        SnowWarsPlugin.sendMessage(onlinePlayer, "The player " + player.getName() + " was blown up for throwning snowballs too fast. (cheat)");
                }
            });
        }
    }

    @EventHandler
    public void onInventoryOpenEvent(InventoryOpenEvent event) {
        if (mainSnowWarsGame != null && mainSnowWarsGame.getPlayers().contains(event.getPlayer()))
            SnowWarsGame.filterInventory(event.getInventory());
    }

    @EventHandler
    public void onCraftItemEvent(CraftItemEvent event) {
        if (mainSnowWarsGame == null || mainSnowWarsGame.getPlayers().contains(event.getWhoClicked())) return;
        final ItemStack item = event.getCurrentItem();
        if (item == null) return;
        if (Config.itemsAbleToBreakSnow.contains(item.getType()))
            SnowWarsGame.filterItemStack(item);
    }
}
