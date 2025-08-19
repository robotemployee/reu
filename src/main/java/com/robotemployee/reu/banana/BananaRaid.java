package com.robotemployee.reu.banana;

import com.robotemployee.reu.banana.entity.BananaRaidMob;
import com.robotemployee.reu.util.MobUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.UUID;

public class BananaRaid {

    private BananaRaidSavedData manager;

    private final ArrayList<UUID> spawnedEntities = new ArrayList<>();
    private static final String SPAWNED_ENTITIES_PATH = "SpawnedEntityUUIDs";

    private final UUID raidUUID;
    private static final String RAID_UUID_PATH = "RaidUUID";

    private ServerLevel level;

    private final BlockPos epicenter;
    private static final String EPICENTER_PATH = "Epicenter";

    // use this constructor if you are making a new raid
    public BananaRaid(BananaRaidSavedData manager, ServerLevel level, BlockPos epicenter) {
        this(epicenter, UUID.randomUUID());
        init(level, manager);
    }

    // use this constructor if you are loading from NBT
    public BananaRaid(BlockPos epicenter, UUID raidUUID) {
        this.raidUUID = raidUUID;
        this.epicenter = epicenter;
    }

    public void init(ServerLevel level, BananaRaidSavedData manager) {
        this.level = level;
        this.manager = manager;
        for (UUID bananaUUID : spawnedEntities) {
            Entity entity = level.getEntity(bananaUUID);
            if (!(entity instanceof BananaRaidMob raidMob) || !MobUtils.entityIsValidForTargeting(raidMob)) {
                spawnedEntities.remove(bananaUUID);
                continue;
            }
            raidMob.init(this);
        }
    }

    public CompoundTag save(CompoundTag tag) {

        ListTag spawnedEntitiesTag = new ListTag();
        for (UUID uuid: spawnedEntities) spawnedEntitiesTag.add(NbtUtils.createUUID(uuid));
        tag.put(SPAWNED_ENTITIES_PATH, spawnedEntitiesTag);

        tag.putUUID(RAID_UUID_PATH, raidUUID);
        tag.putLong(EPICENTER_PATH, epicenter.asLong());

        return tag;
    }

    public static BananaRaid load(CompoundTag tag) {

        BlockPos epicenter = BlockPos.of(tag.getLong(EPICENTER_PATH));
        UUID raidUUID = tag.getUUID(RAID_UUID_PATH);

        BananaRaid newborn = new BananaRaid(epicenter, raidUUID);

        ListTag spawnedEntities = tag.getList(SPAWNED_ENTITIES_PATH, Tag.TAG_INT_ARRAY);
        for (Tag value : spawnedEntities) newborn.registerSpawnedEntity(NbtUtils.loadUUID(value));



        return newborn;
    }

    public void requestAirlift(UUID requester, BlockPos destination) {
        //TODO: implement
    }

    public void registerSpawnedEntity(UUID uuid) {
        spawnedEntities.add(uuid);
    }

    public UUID getRaidUUID() {
        return raidUUID;
    }

    public BlockPos getEpicenter() {
        return epicenter;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public enum EnemyTypes {
        GREG,
        ASCENDER,
        COMMANDER,
        ORCHESTRATOR,
        DEVIL,
        LIEUTENANT
    }
}
