package fr.bananasmoothii.snowwars;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;

public class PluginListener implements Listener {

    @EventHandler
    public void onEntityPickupItemEvent(EntityPickupItemEvent event) {
        Item item = event.getItem();
        if (item.getItemStack().getType() == Material.SNOW_BLOCK) {
            item.setMetadata("canPlaceOn", new FixedMetadataValue(SnowWarsPlugin.inst(), Config.canPlaceSnowOn));
        }
    }
}
