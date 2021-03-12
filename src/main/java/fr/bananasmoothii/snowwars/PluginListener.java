package fr.bananasmoothii.snowwars;

import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;
import net.minecraft.server.v1_16_R3.NBTTagString;
import net.minecraft.server.v1_16_R3.RecipeBook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("MethodMayBeStatic")
public class PluginListener implements Listener {

    @EventHandler
    public void onEntityPickupItemEvent(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof HumanEntity && SnowWarsPlugin.mainSnowWarsGame != null && SnowWarsPlugin.mainSnowWarsGame.getPlayers().contains(event.getEntity())) {
            asyncFilterInventory(((HumanEntity) event.getEntity()).getInventory());
        }
    }

    @EventHandler
    public void onCraftItemEvent(CraftItemEvent event) {
        for (HumanEntity humanEntity: event.getViewers()) {
            if (SnowWarsPlugin.mainSnowWarsGame != null && SnowWarsPlugin.mainSnowWarsGame.getPlayers().contains(humanEntity))
                asyncFilterInventory(humanEntity.getInventory());
        }
    }

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

    public static void asyncFilterInventory(Inventory inventory) {
        Bukkit.getScheduler().runTaskAsynchronously(SnowWarsPlugin.inst(), () -> filterInventory(inventory));
    }

    public static void filterInventory(Inventory inventory) {
        for (ItemStack itemStack: inventory.getContents()) {
            if (itemStack != null) {
                inventory.setItem(inventory.first(itemStack.getType()), filterItemStack(itemStack));
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
            //noinspection ConstantConditions
            compound.set("CanDestroy", tagList);
        }
        nmsItemStack.setTag(compound);
        return CraftItemStack.asBukkitCopy(nmsItemStack);
    }
}
