package fr.bananasmoothii.snowwars;

import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public abstract class Util {

    @Contract("_, _, _, _ -> new")
    public static @NotNull Vector getVectorFromAToB(int ax, int ay, int az, @NotNull Location b) {
        return new Vector(b.getX() - ax, b.getY() - ay, b.getZ() - az);
    }

    @Contract("_, _ -> new")
    public static @NotNull Vector getVectorFromAToB(@NotNull Location a, @NotNull Location b) {
        return new Vector(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ());
    }

    @Contract("_, _ -> new")
    public static @NotNull Location blockVector3ToLocation(@NotNull BlockVector3 blockVector3, World world) {
        return new Location(world, blockVector3.getX(), blockVector3.getY(), blockVector3.getZ());
    }

    @Contract("_ -> new")
    public static @NotNull BlockVector3 locationToBlockVector3(@NotNull Location location) {
        return BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }
}
