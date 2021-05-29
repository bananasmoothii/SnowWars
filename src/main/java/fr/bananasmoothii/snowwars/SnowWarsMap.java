package fr.bananasmoothii.snowwars;

import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class SnowWarsMap {

    private String name;

    private Location sourceSpawn;
    private Location playSpawn;

    private @NotNull CuboidRegion sourceRegion;
    private @NotNull CuboidRegion playRegion;

    private Collection<Location> spawnLocations;

    public SnowWarsMap(String name, Location sourceSpawn, Location playSpawn, @NotNull CuboidRegion sourceRegion, Collection<Location> spawnLocations) {
        this.name = name;
        if (sourceSpawn != null && sourceSpawn.getWorld() == null) throw new NullPointerException("please give Locations with a set World");
        this.sourceSpawn = sourceSpawn;
        if (playSpawn != null && playSpawn.getWorld() == null) throw new NullPointerException("please give Locations with a set World");
        this.playSpawn = playSpawn;
        this.sourceRegion = Objects.requireNonNull(sourceRegion);
        this.spawnLocations = spawnLocations;
        this.playRegion = calculateRegionAtNewLocation(sourceRegion, sourceSpawn,playSpawn);
    }

    public SnowWarsMap(String name, Location sourceSpawn, Location playSpawn, Location sourceMin, Location sourceMax, Collection<Location> spawnLocations) {
        this(name,
                sourceSpawn,
                playSpawn,
                new CuboidRegion(BukkitAdapter.adapt(sourceSpawn.getWorld()), Util.locationToBlockVector3(sourceMin), Util.locationToBlockVector3(sourceMax)),
                spawnLocations);
    }

    /**
     * WARNING: this constructor is only intended to use with {@link SnowWarsMap#setPlaySpawn(Location)} after
     */
    public SnowWarsMap(String name, Location sourceSpawn, @NotNull CuboidRegion sourceRegion) {
        this(name, sourceSpawn, null, sourceRegion, new ArrayList<>());
    }

    public void refresh() throws WorldEditException {
        com.sk89q.worldedit.world.World srcWorld = BukkitAdapter.adapt(sourceSpawn.getWorld());
        com.sk89q.worldedit.world.World destWorld = BukkitAdapter.adapt(playSpawn.getWorld());
        BlockVector3 srcCenter = Util.locationToBlockVector3(sourceSpawn);
        BlockVector3 destCenter = Util.locationToBlockVector3(playSpawn);

        try (EditSession editSession = new EditSessionBuilder(destWorld).fastmode(false).build()) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(srcWorld, sourceRegion, srcCenter, editSession, destCenter);
            forwardExtentCopy.setCopyingEntities(true);
            Operations.complete(forwardExtentCopy);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getSourceSpawn() {
        return sourceSpawn;
    }

    public void setSourceSpawn(Location sourceSpawn) {
        if (sourceSpawn == null || sourceSpawn.getWorld() == null) throw new NullPointerException("please give Locations with a set World and don't set to null");
        this.sourceSpawn = Objects.requireNonNull(sourceSpawn);
    }

    public Location getPlaySpawn() {
        return playSpawn;
    }

    public void setPlaySpawn(Location playSpawn) {
        if (playSpawn == null || playSpawn.getWorld() == null) throw new NullPointerException("please give Locations with a set World and don't set to null");
        this.playSpawn = Objects.requireNonNull(playSpawn);
    }

    public @NotNull CuboidRegion getSourceRegion() {
        return sourceRegion;
    }

    public void setSourceRegion(@NotNull CuboidRegion sourceRegion) {
        this.sourceRegion = Objects.requireNonNull(sourceRegion);
        playRegion = calculateRegionAtNewLocation(sourceRegion, sourceSpawn, playSpawn);
    }

    public Collection<Location> getSpawnLocations() {
        return spawnLocations;
    }

    public int getDifferentSpawns() {
        return this.spawnLocations.size();
    }

    public @NotNull CuboidRegion getPlayRegion() {
        return playRegion;
    }

    public static @NotNull CuboidRegion calculateRegionAtNewLocation(CuboidRegion oldRegion, Location oldSourcePoint, Location newSourcePoint) {
        return new CuboidRegion(BukkitAdapter.adapt(newSourcePoint.getWorld()),
                BlockVector3.at(newSourcePoint.getBlockX() + oldRegion.getMinimumPoint().getBlockX() - oldSourcePoint.getBlockX(), newSourcePoint.getBlockY() + oldRegion.getMinimumPoint().getBlockY() - oldSourcePoint.getBlockY(), newSourcePoint.getBlockZ() + oldRegion.getMinimumPoint().getBlockZ() - oldSourcePoint.getBlockZ()),
                BlockVector3.at(newSourcePoint.getBlockX() + oldRegion.getMaximumPoint().getBlockX() - oldSourcePoint.getBlockX(), newSourcePoint.getBlockY() + oldRegion.getMaximumPoint().getBlockY() - oldSourcePoint.getBlockY(), newSourcePoint.getBlockZ() + oldRegion.getMaximumPoint().getBlockZ() - oldSourcePoint.getBlockZ())
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SnowWarsMap that = (SnowWarsMap) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
