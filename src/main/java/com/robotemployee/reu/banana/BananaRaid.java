package com.robotemployee.reu.banana;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.UUID;

public class BananaRaid {

    private final ArrayList<UUID> spawnedEntities = new ArrayList<>();
    public static final String SPAWNED_ENTITIES_PATH = "SpawnedEntityUUIDs";

    public CompoundTag save(CompoundTag tag) {
        ListTag spawnedEntitiesTag = new ListTag();
        for (UUID uuid: spawnedEntities) spawnedEntitiesTag.add(NbtUtils.createUUID(uuid));
        tag.put(SPAWNED_ENTITIES_PATH, spawnedEntitiesTag);
        return tag;
    }

    public static BananaRaid load(CompoundTag tag) {
        BananaRaid newborn = new BananaRaid();

        ListTag spawnedEntities = tag.getList(SPAWNED_ENTITIES_PATH, Tag.TAG_INT_ARRAY);
        for (Tag value : spawnedEntities) newborn.registerSpawnedEntity(NbtUtils.loadUUID(value));

        return newborn;
    }

    public void registerSpawnedEntity(UUID uuid) {
        spawnedEntities.add(uuid);
    }

    enum Enemies {
        CRAWLER,
        RECYCLER,
        COMMANDER,
        VISITOR,
        DEVIL,
        LIEUTENANT
    }
}
