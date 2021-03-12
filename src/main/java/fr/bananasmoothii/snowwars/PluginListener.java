package fr.bananasmoothii.snowwars;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@SuppressWarnings("MethodMayBeStatic")
public class PluginListener implements Listener {

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        if (SnowWarsPlugin.mainSnowWarsGame != null && SnowWarsPlugin.mainSnowWarsGame.getPlayers().contains(event.getPlayer())) {
            SnowWarsPlugin.mainSnowWarsGame.removePlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerDeathEvent(PlayerDeathEvent event) {
        if (SnowWarsPlugin.mainSnowWarsGame != null && SnowWarsPlugin.mainSnowWarsGame.getPlayers().contains(event.getEntity())) {
            SnowWarsPlugin.mainSnowWarsGame.playerDied(event);
        }
    }
}
