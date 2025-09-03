package com.robotemployee.reu.banana;

import com.robotemployee.reu.banana.entity.AsteirtoEntity;
import com.robotemployee.reu.banana.entity.BananaRaidMob;
import com.robotemployee.reu.util.MobUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.lang.ref.WeakReference;
import java.util.*;

public class BananaRaid {

    private BananaRaidLevelManager manager;

    private final ArrayList<UUID> spawnedEntities = new ArrayList<>();
    private static final String SPAWNED_ENTITIES_PATH = "SpawnedEntityUUIDs";

    private final UUID raidUUID;
    private static final String RAID_UUID_PATH = "RaidUUID";

    private ServerLevel level;
    private final BlockPos epicenter;
    private static final String EPICENTER_PATH = "Epicenter";

    private static final HashMap<UUID, AirliftRequest> airliftRequests = new HashMap<>();

    // use this constructor if you are making a new raid
    public BananaRaid(BananaRaidLevelManager manager, ServerLevel level, BlockPos epicenter) {
        this(epicenter, UUID.randomUUID());
        init(level, manager);
    }

    // use this constructor if you are loading from NBT
    public BananaRaid(BlockPos epicenter, UUID raidUUID) {
        this.raidUUID = raidUUID;
        this.epicenter = epicenter;
    }

    public void init(ServerLevel level, BananaRaidLevelManager manager) {
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

    public void onBananaRemoved(BananaRaidMob raidMob) {
        removeAirliftRequest(raidMob.getUUID());
    }

    public void checkForHanging() {
        for (Map.Entry<UUID, AirliftRequest> entry : airliftRequests.entrySet()) {
            if (entry.getValue().shouldExpire()) airliftRequests.remove(entry.getKey());
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

    public void requestAirlift(BananaRaidMob requester, BlockPos destination) {
        UUID requestUUID = requester.getUUID();
        HashMap<UUID, AirliftRequest> requests = getAirliftRequests();

        requests.computeIfPresent(requestUUID, (uuid, req) -> {
            req.updateRequest(requester, destination);
            return req;
        });

        requests.computeIfAbsent(requestUUID, uuid -> new AirliftRequest(this, requester, destination));
    }

    public void tick() {

    }

    // todo document
    public Collection<BananaRaidMob> findRecycleTargets(BlockPos asteirtoPos, int maxValue) {
        AABB aabb = new AABB(asteirtoPos).inflate(AsteirtoEntity.RECYCLE_RANGE);
        List<BananaRaidMob> raw = getLevel().getEntitiesOfClass(BananaRaidMob.class, aabb)
                .stream()
                .filter(BananaRaidMob::canRecycle)
                .sorted((raidMobA, raidMobB) ->
                        // inverted on purpose; the higher the value, the more reluctant we are to recycle
                        Float.compare(raidMobB.getRecycleImpedance(), raidMobA.getRecycleImpedance())
                ).toList();

        int totalValue = 0;

        List<BananaRaidMob> output = new ArrayList<>();
        for (BananaRaidMob raidMob : raw) {
            int providedValue = (int)Math.floor(raidMob.getRecycleImpedance());
            if (totalValue + providedValue > maxValue) break;
            output.add(raidMob);
        }
        return output;
    }

    public void removeAirliftRequest(UUID requesterUUID) {
        getAirliftRequests().remove(requesterUUID);
    }

    public HashMap<UUID, AirliftRequest> getAirliftRequests() {
        return airliftRequests;
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

    public static class AirliftRequest {
        private final WeakReference<BananaRaid> raid;
        private final WeakReference<BananaRaidMob> requester;
        private final UUID requesterUUID;
        private BlockPos from;
        private BlockPos to;
        private final float weightCoefficient;
        private final float baseDistance;
        private long timestamp;

        private boolean pleaseKillMe = false;

        public static final float MAX_WEIGHT_AT_MIN_DISTANCE = 10;
        public static final float MIN_WEIGHT_FROM_DISTANCE = 0.05f;
        public static final float DISTANCE_UNTIL_ZERO = 40f;

        public static final long TICKS_UNTIL_EXPIRE = 1200;

        public AirliftRequest(BananaRaid raid, BananaRaidMob requester, BlockPos destination) {
            this(raid, requester, requester.blockPosition(), destination);
        }

        public AirliftRequest(BananaRaid raid, BananaRaidMob requester, BlockPos from, BlockPos to) {
            this.raid = new WeakReference<>(raid);
            this.requester = new WeakReference<>(requester);
            this.requesterUUID = requester.getUUID();
            this.from = from;
            this.to = to;
            this.weightCoefficient = requester.getAirliftWeight();
            this.baseDistance = (float)Math.sqrt(from.distSqr(to));
            this.timestamp = requester.level().getGameTime();
        }

        public void updateRequest(BananaRaidMob requester, BlockPos destination) {
            updateRequest(requester, requester.blockPosition(), destination);
        }

        public void updateRequest(BananaRaidMob requester, BlockPos from, BlockPos to) {
            if (requester != getRequester()) throw new IllegalArgumentException("Must give a requester that matches the one that originally created the request");
            this.timestamp = requester.level().getGameTime();
            this.from = from;
            this.to = to;
        }

        public UUID getRequesterUUID() {
            return requesterUUID;
        }

        public BananaRaid getRaid() {
            return getFromWeakReference(raid);
        }

        public BananaRaidMob getRequester() {
            return getFromWeakReference(requester);
        }

        private <T> T getFromWeakReference(WeakReference<T> reference) {
            T deferenced = reference.get();
            if (deferenced == null) {
                pleaseKillMe = true;
            }
            return deferenced;
        }


        public BlockPos getStartingPosition() {
            return from;
        }

        public BlockPos getEndingPosition() {
            return to;
        }

        public float calculateWeight(BlockPos positionOfGreg) {
            float distance = (float)Math.sqrt(positionOfGreg.distSqr(from)) + baseDistance;
            float weightFromProximity = MAX_WEIGHT_AT_MIN_DISTANCE * (1 - Mth.clamp((distance / DISTANCE_UNTIL_ZERO), 0, 1));
            return weightCoefficient * Math.max(MIN_WEIGHT_FROM_DISTANCE, weightFromProximity);
        }

        public boolean shouldExpire() {
            BananaRaidMob raidMob = getFromWeakReference(requester);
            if (raidMob == null) {
                pleaseKillMe = true;
                return true;
            }
            long time = raidMob.level().getGameTime();

            if (time - timestamp > TICKS_UNTIL_EXPIRE) {
                pleaseKillMe = true;
                return true;
            }

            return pleaseKillMe || getFromWeakReference(raid) == null || getFromWeakReference(requester) == null;
        }
    }

    public enum EnemyType {
        GREG,
        DEVIL,
        ASTEIRTO,
        TEMFUR_TEMFUR,
        POSTERBOY,
        FLAPJACK
    }
}
