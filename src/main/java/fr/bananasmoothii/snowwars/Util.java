package fr.bananasmoothii.snowwars;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public abstract class Util {

    public static Vector getVectorFromAToB(int ax, int ay, int az, Location b) {
        return new Vector(b.getX() - ax, b.getY() - ay, b.getZ() - az);
    }

    public static @NotNull Vector getVectorFromAToB(Location a, Location b) {
        return new Vector(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ());
    }

    public static Location blockVector3ToLocation(BlockVector3 blockVector3, World world) {
        return new Location(world, blockVector3.getX(), blockVector3.getY(), blockVector3.getZ());
    }

    public static BlockVector3 locationToBlockVector3(Location location) {
        return BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
