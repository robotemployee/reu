package com.robotemployee.reu.banana;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class BananaRaidSavedData extends SavedData {

    private final ArrayList<BananaRaid> RAIDS = new ArrayList<>();

    private static final String ONGOING_RAIDS_PATH = "OngoingRaids";

    public BananaRaidSavedData() {
        this.setDirty();
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (BananaRaid raid : RAIDS) list.add(raid.save(new CompoundTag()));
        tag.put(ONGOING_RAIDS_PATH, list);
        return tag;
    }

    public static BananaRaidSavedData load(CompoundTag tag) {
        BananaRaidSavedData savedData = new BananaRaidSavedData();

        ListTag list = tag.getList(ONGOING_RAIDS_PATH, Tag.TAG_COMPOUND);
        for (Tag value : list) savedData.registerRaid(BananaRaid.load((CompoundTag) value));

        return savedData;
    }

    public void registerRaid(BananaRaid raid) {
        RAIDS.add(raid);
    }

    public BananaRaid getRaid(int index) {
        return RAIDS.get(index);
    }
}
