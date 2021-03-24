package fr.bananasmoothii.snowwars;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Random;

@SuppressWarnings({"MethodMayBeStatic", "unused"})
public class PluginListener implements Listener {

    public static int snowBlockBreakMaxDrops = 4;
    private static final Random random = new Random();

    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        if (SnowWarsPlugin.mainSnowWarsGame == null
                || ! SnowWarsPlugin.mainSnowWarsGame.getPlayers().contains(event.getPlayer())
                || ! SnowWarsPlugin.mainSnowWarsGame.isStarted()) return;
        if (event.getBlock().getType() == Material.SNOW_BLOCK) {
            int amountToDrop = random.nextInt(snowBlockBreakMaxDrops);
            if (amountToDrop == 0)
                event.setDropItems(false);
        }
        //Bukkit.getScheduler().runTaskAsynchronously()
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        if (SnowWarsPlugin.mainSnowWarsGame != null && SnowWarsPlugin.mainSnowWarsGame.getPlayers().contains(event.getPlayer())) {
            SnowWarsPlugin.mainSnowWarsGame.removePlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        if (SnowWarsPlugin.mainSnowWarsGame != null && SnowWarsPlugin.mainSnowWarsGame.isStarted() && SnowWarsPlugin.mainSnowWarsGame.getPlayers().contains(event.getEntity())) {
            SnowWarsPlugin.mainSnowWarsGame.playerDied(event);
        }
    }

    /*
    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player
            && SnowWarsPlugin.mainSnowWarsGame != null
            && SnowWarsPlugin.mainSnowWarsGame.isStarted()
            && SnowWarsPlugin.mainSnowWarsGame.getPlayers().contains(event.getEntity())
            && event.getFinalDamage() > ((Damageable) event.getEntity()).getHealth())
        {
            event.setCancelled(true);
            SnowWarsPlugin.mainSnowWarsGame.playerDied(((Player) event.getEntity()).getPlayer(), null);
        }
    }
    
     */

    private static final HashMap<Player, Double> fallingPlayers = new HashMap<>(); // double is the height where the player started to fall

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (Config.maxFallHeight == 0
                || SnowWarsPlugin.mainSnowWarsGame == null
                || ! SnowWarsPlugin.mainSnowWarsGame.getPlayers().contains(player)
                || ! SnowWarsPlugin.mainSnowWarsGame.isStarted()
                || SnowWarsPlugin.mainSnowWarsGame.getData(player).isGhost())
            return;

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
}
