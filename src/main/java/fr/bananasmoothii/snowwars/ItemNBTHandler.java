package fr.bananasmoothii.snowwars;

import net.minecraft.server.v1_16_R3.NBTList;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;
import net.minecraft.server.v1_16_R3.NBTTagString;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ItemNBTHandler {

    private final net.minecraft.server.v1_16_R3.ItemStack item;
    private final NBTTagCompound compound;

    public ItemNBTHandler(ItemStack item) {
        this.item = CraftItemStack.asNMSCopy(item);
        compound = this.item.hasTag() ? this.item.getTag() : new NBTTagCompound();
    }

    public void set(String name, List<String> list) {
        NBTTagList nbtTagList = new NBTTagList();
        for (String s: list) {
            nbtTagList.add(NBTTagString.a(s)); // seems to be the best way
        }
        compound.set(name, nbtTagList);
    }

    public ItemStack get() {
        return CraftItemStack.asBukkitCopy(item);
    }
}
