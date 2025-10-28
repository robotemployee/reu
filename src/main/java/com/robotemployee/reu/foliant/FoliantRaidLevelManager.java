package com.robotemployee.reu.foliant;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;

public class FoliantRaidLevelManager extends SavedData {

    private static final String SAVED_DATA_PATH = "FoliantRaidData";
    private final HashMap<BlockPos, FoliantRaid> RAIDS = new HashMap<>();
    private static final String ONGOING_RAIDS_PATH = "OngoingRaids";

    private ResourceKey<Level> dimension;

    private static final Logger LOGGER = LogUtils.getLogger();

    public FoliantRaidLevelManager() {
        this.setDirty();
    }

    public Collection<FoliantRaid> getRaids() {
        return RAIDS.values();
    }

    public static final int RAID_RADIUS = 256;

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (FoliantRaid raid : getRaids()) list.add(raid.save(new CompoundTag()));
        tag.put(ONGOING_RAIDS_PATH, list);
        return tag;
    }

    private static FoliantRaidLevelManager loadFromTag(CompoundTag tag) {
        FoliantRaidLevelManager savedData = new FoliantRaidLevelManager();

        ListTag list = tag.getList(ONGOING_RAIDS_PATH, Tag.TAG_COMPOUND);
        for (Tag value : list) savedData.registerRaid(FoliantRaid.load((CompoundTag) value));

        return savedData;
    }

    // call this method when the level loads
    public static FoliantRaidLevelManager onLevelLoaded(ServerLevel level) {
        FoliantRaidLevelManager data = getFromSaveData(level);
        data.init(level);
        return data;
    }

    // this one just gets you an instance from save data
    private static FoliantRaidLevelManager getFromSaveData(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(FoliantRaidLevelManager::loadFromTag, FoliantRaidLevelManager::new, SAVED_DATA_PATH);
    }

    // this one is for propagating "hey the server is loaded now" to every raid
    private void init(ServerLevel level) {
        this.dimension = level.dimension();
        for (FoliantRaid raid : getRaids()) raid.init(level, this);
    }

    public void tick(ServerLevel level) {
        getRaids().forEach(FoliantRaid::tick);
    }

    @Nullable
    public FoliantRaid startRaid(BlockPos epicenter) {
        if (!canCreateRaidAt(epicenter)) return null;
        FoliantRaid newborn = FoliantRaid.create(this, epicenter);
        registerRaid(newborn);
        return newborn;
    }

    public static final int RAID_EXCLUSION_RADIUS = 300;
    public boolean canCreateRaidAt(BlockPos epicenter) {
        if (RAIDS.containsKey(epicenter)) return false;

        RAIDS.values().stream().anyMatch(raid -> isRaidNearby(epicenter, RAID_EXCLUSION_RADIUS));

        return true;
    }

    protected void registerRaid(FoliantRaid raid) {
        RAIDS.put(raid.getEpicenter(), raid);
        this.setDirty();
    }

    protected void deregisterRaid(BlockPos epicenter) {
        RAIDS.remove(epicenter);
        this.setDirty();
    }

    protected void deregisterAllRaids() {
        RAIDS.clear();
        this.setDirty();
    }

    public FoliantRaid getRaid(BlockPos epicenter) {
        return RAIDS.get(epicenter);
    }

    public ServerLevel getLevel() {
        return ServerLifecycleHooks.getCurrentServer().getLevel(dimension);
    }

    public void stopAll() {
        for (FoliantRaid raid : RAIDS.values()) {
            raid.onStopped();
        }
        deregisterAllRaids();
    }

    public void stopRaid(BlockPos epicenter) {
        stopRaid(getRaid(epicenter));
    }

    public void stopRaid(FoliantRaid raid) {
        BlockPos epicenter = raid.getEpicenter();
        raid.onStopped();
        deregisterRaid(epicenter);
    }

    public boolean isRaidNearby(BlockPos pos) {
        return getRaids().stream().anyMatch(raid -> raid.isPositionContained(pos));
    }

    public boolean isRaidNearby(BlockPos pos, int radius) {
        int radiusSqr = (int)Math.pow(radius, 2);
        return getRaids().stream().anyMatch(raid -> pos.distSqr(raid.getEpicenter()) <= radiusSqr);
    }

    @Nullable
    public FoliantRaid getRaidNearby(BlockPos pos) {
        return getRaids().stream().filter(raid -> raid.isPositionContained(pos)).findFirst().orElse(null);
    }
}
