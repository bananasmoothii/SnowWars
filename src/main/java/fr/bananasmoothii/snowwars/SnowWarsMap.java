package fr.bananasmoothii.snowwars;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
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
    private @Nullable Location playSpawn;

    private @NotNull CuboidRegion sourceRegion;
    private @Nullable CuboidRegion playRegion; // nullable if playSpawn is null

    private final Collection<Location> spawnLocations;

    private @Nullable Location voteLocation;

    @SuppressWarnings({"NullableProblems"})
    public SnowWarsMap(String name, Location sourceSpawn, Location playSpawn, @NotNull CuboidRegion sourceRegion, Collection<Location> spawnLocations, @Nullable Location voteLocation) {
        this.name = name;
        if (sourceSpawn != null && sourceSpawn.getWorld() == null) throw new NullPointerException("please give Locations with a set World");
        this.sourceSpawn = sourceSpawn;
        if (playSpawn != null && playSpawn.getWorld() == null) throw new NullPointerException("please give Locations with a set World");
        this.playSpawn = playSpawn;
        this.sourceRegion = Objects.requireNonNull(sourceRegion);
        this.spawnLocations = spawnLocations;
        if (playSpawn != null)
            this.playRegion = calculateRegionAtNewLocation(sourceRegion, sourceSpawn,playSpawn);
        this.voteLocation = voteLocation;
    }

    public SnowWarsMap(String name, Location sourceSpawn, Location playSpawn, Location sourceMin, Location sourceMax, Collection<Location> spawnLocations, @Nullable Location voteLocation) {
        this(name,
                sourceSpawn,
                playSpawn,
                new CuboidRegion(BukkitAdapter.adapt(sourceSpawn.getWorld()), Util.locationToBlockVector3(sourceMin), Util.locationToBlockVector3(sourceMax)),
                spawnLocations,
                voteLocation);
    }

    /**
     * WARNING: this constructor is only intended to use with {@link SnowWarsMap#setPlaySpawn(Location)} after
     */
    public SnowWarsMap(String name, Location sourceSpawn, @NotNull CuboidRegion sourceRegion) {
        this(name, sourceSpawn, null, sourceRegion, new ArrayList<>(), null);
    }

    public void refresh() throws WorldEditException {
        if (playSpawn == null || playSpawn.getWorld() == null) throw new NullPointerException("Not a valid map");
        com.sk89q.worldedit.world.World srcWorld = BukkitAdapter.adapt(sourceSpawn.getWorld());
        com.sk89q.worldedit.world.World destWorld = BukkitAdapter.adapt(playSpawn.getWorld());
        BlockVector3 srcCenter = Util.locationToBlockVector3(sourceSpawn);
        BlockVector3 destCenter = Util.locationToBlockVector3(playSpawn);

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(destWorld).fastMode(false).build()) {
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(srcWorld, sourceRegion, srcCenter, editSession, destCenter);
            forwardExtentCopy.setCopyingEntities(true);
            Operations.complete(forwardExtentCopy);
        }
    }

    public void refreshAndCatchExceptions() {
        try {
            refresh();
        } catch (WorldEditException e) {
            CustomLogger.severe("The map will not be copied");
            e.printStackTrace();
        } catch (NullPointerException e) {
            CustomLogger.severe("The map will not be copied. You probably didn't set the source with /snowwars addmap");
            e.printStackTrace();
        } catch (NoSuchMethodError e) {
            CustomLogger.severe("The map will not be copied. There is a problem with WorldEdit");
            e.printStackTrace();
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

    public @Nullable Location getPlaySpawn() {
        return playSpawn;
    }

    public void setPlaySpawn(Location playSpawn) {
        if (playSpawn == null || playSpawn.getWorld() == null) throw new NullPointerException("please give Locations with a set World and don't set to null");
        this.playSpawn = Objects.requireNonNull(playSpawn);
        this.playRegion = calculateRegionAtNewLocation(sourceRegion, sourceSpawn, playSpawn);
    }

    public @NotNull CuboidRegion getSourceRegion() {
        return sourceRegion;
    }

    @SuppressWarnings("ConstantConditions")
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

    public @Nullable CuboidRegion getPlayRegion() {
        return playRegion;
    }

    public @Nullable Location getVoteLocation() {
        return voteLocation;
    }

    public void setVoteLocation(@Nullable Location voteLocation) {
        this.voteLocation = voteLocation;
    }

    @SuppressWarnings("TypeMayBeWeakened")
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
        return name.hashCode();
    }

    public boolean isVoting(Location location) {
        if (voteLocation == null) return false;
        return Math.abs(location.getX() - voteLocation.getX()) <= Config.voteDistance
                && Math.abs(location.getY() - voteLocation.getY()) <= Config.voteDistance
                && Math.abs(location.getZ() - voteLocation.getZ()) <= Config.voteDistance;
    }
}
