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
import java.util.Map;
import java.util.UUID;

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
        for (FoliantRaid raid: getRaids()) {
            raid.tick();
        }
    }

    @Nullable
    public FoliantRaid createRaidOrNull(BlockPos epicenter) {
        if (!canCreateRaid(epicenter)) return null;
        FoliantRaid newborn = FoliantRaid.create(this, epicenter);
        registerRaid(newborn);
        return newborn;
    }

    public boolean canCreateRaid(BlockPos epicenter) {
        return !RAIDS.containsKey(epicenter);
    }

    public void registerRaid(FoliantRaid raid) {
        RAIDS.put(raid.getEpicenter(), raid);
        this.setDirty();
    }

    public void deregisterRaid(BlockPos epicenter) {
        RAIDS.remove(epicenter);
        this.setDirty();
    }

    public FoliantRaid getRaid(UUID uuid) {
        return RAIDS.get(uuid);
    }

    public ServerLevel getLevel() {
        return ServerLifecycleHooks.getCurrentServer().getLevel(dimension);
    }

    public void stopAll() {
        for (Map.Entry<BlockPos, FoliantRaid> entry : RAIDS.entrySet()) {
            BlockPos epicenter = entry.getKey();
            FoliantRaid raid = entry.getValue();
            raid.stop();
            deregisterRaid(epicenter);
            this.setDirty();
        }
    }
}
