package fr.bananasmoothii.snowwars;


import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;

public class BlockBreaker {

    private final int limit;
    private final List<Block> toBreak;
    private boolean breakBlocksOnFinish;
    private boolean boundToSolidBlock;

    public BlockBreaker(Block initialBlock) {
        this(initialBlock, Config.snowBlockBreakLimit, true);
    }

    public BlockBreaker(Block initialBlock, int limit, boolean breakBlocksOnFinish) {
        this.breakBlocksOnFinish = breakBlocksOnFinish;
        this.limit = limit;
        toBreak = new ArrayList<>(limit + 1);
        if (initialBlock.getType() != Material.SNOW_BLOCK && initialBlock.getType() != Material.SNOW)
            throw new IllegalArgumentException("not a snow block");
        scheduler.runTaskAsynchronously(inst, () -> {
            checkBlock(initialBlock);
            if (this.breakBlocksOnFinish) breakAll();
        });
    }

    // just for avoiding some calls that will happen often
    private static final BukkitScheduler scheduler = Bukkit.getScheduler();
    private static final SnowWarsPlugin inst = SnowWarsPlugin.inst();

    private void checkBlock(final Block block) {
        Material type = block.getType();
        if (boundToSolidBlock || !Config.canPlaceSnowOn.contains(type) || toBreak.contains(block)) return;
        if ((type != Material.SNOW_BLOCK && type != Material.SNOW) || toBreak.size() >= limit) {
            boundToSolidBlock = true;
            breakBlocksOnFinish = false;
            return;
        }
        toBreak.add(block);

        checkBlock(block.getRelative(0, 0, -1));
        checkBlock(block.getRelative(1, 0, 0));
        checkBlock(block.getRelative(0, 0, 1));
        checkBlock(block.getRelative(-1, 0, 0));
        checkBlock(block.getRelative(0, 1, 0));
        checkBlock(block.getRelative(0, -1, 0));
    }

    private void breakAll() {
        Iterator<Block> iterator = toBreak.iterator();
        taskId = scheduler.scheduleSyncRepeatingTask(inst, () -> {
            if (iterator.hasNext()) iterator.next().breakNaturally(new ItemStack(Material.STONE_SHOVEL));
            else scheduler.cancelTask(taskId);
        }, 1, Config.snowBlockBreakInterval);
    }

    private int taskId = -1;
    public int getTaskId() {
        return taskId;
    }

    public List<Block> getToBreak() {
        return toBreak;
    }

    public boolean isBreakBlocksOnFinish() {
        return breakBlocksOnFinish;
    }

    public void setBreakBlocksOnFinish(boolean breakBlocksOnFinish) {
        this.breakBlocksOnFinish = breakBlocksOnFinish;
    }
}
