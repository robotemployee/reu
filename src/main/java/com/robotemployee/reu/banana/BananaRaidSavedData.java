package com.robotemployee.reu.banana;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class BananaRaidSavedData extends SavedData {

    private static final String SAVED_DATA_PATH = "BananaRaidData";

    private final HashMap<UUID, BananaRaid> RAIDS = new HashMap<>();
    private static final String ONGOING_RAIDS_PATH = "OngoingRaids";

    public BananaRaidSavedData() {
        this.setDirty();
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (BananaRaid raid : RAIDS.values()) list.add(raid.save(new CompoundTag()));
        tag.put(ONGOING_RAIDS_PATH, list);
        return tag;
    }

    private static BananaRaidSavedData loadFromTag(CompoundTag tag) {
        BananaRaidSavedData savedData = new BananaRaidSavedData();

        ListTag list = tag.getList(ONGOING_RAIDS_PATH, Tag.TAG_COMPOUND);
        for (Tag value : list) savedData.registerRaid(BananaRaid.load((CompoundTag) value));

        return savedData;
    }

    // call this method when the level loads
    public static BananaRaidSavedData onLevelLoaded(ServerLevel level) {
        BananaRaidSavedData data = getFromSaveData(level);
        data.init(level);
        return data;
    }

    // this one just gets you an instance from save data
    private static BananaRaidSavedData getFromSaveData(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(BananaRaidSavedData::loadFromTag, BananaRaidSavedData::new, SAVED_DATA_PATH);
    }

    // this one is for propagating "hey we've loaded" to every raid
    private void init(ServerLevel level) {
        for (BananaRaid raid : RAIDS.values()) raid.init(level, this);
    }

    public void registerRaid(BananaRaid raid) {
        RAIDS.put(raid.getRaidUUID(), raid);
        this.setDirty();
    }

    public void removeRaid(UUID uuid) {
        RAIDS.remove(uuid);
        this.setDirty();
    }

    public BananaRaid getRaid(UUID uuid) {
        return RAIDS.get(uuid);
    }
}
