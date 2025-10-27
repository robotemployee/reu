package com.robotemployee.reu.foliant;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.foliant.entity.FoliantRaidMob;
import com.robotemployee.reu.registry.ModEntities;
import com.robotemployee.reu.util.LevelUtils;
import com.robotemployee.reu.util.MobUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import org.slf4j.Logger;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Supplier;

public class FoliantRaid {

    private static final Logger LOGGER = LogUtils.getLogger();

    private FoliantRaidLevelManager manager;

    private final ArrayList<UUID> spawnedEntities = new ArrayList<>();
    private static final String SPAWNED_ENTITIES_PATH = "SpawnedEntityUUIDs";

    private static final String RAID_UUID_PATH = "RaidUUID";

    private ServerLevel level;
    private final BlockPos epicenter;
    private static final String EPICENTER_PATH = "Epicenter";

    private final HashMap<BlockPos, Spawner> SPAWNERS = new HashMap<>();

    // use this constructor if you are making a new raid
    private FoliantRaid(FoliantRaidLevelManager manager, ServerLevel level, BlockPos epicenter) {
        this(epicenter);
        init(level, manager);
    }

    // use this constructor if you are loading from NBT
    private FoliantRaid(BlockPos epicenter) {
        this.epicenter = epicenter;
    }

    static FoliantRaid create(FoliantRaidLevelManager manager, BlockPos epicenter) {
        return new FoliantRaid(manager, manager.getLevel(), epicenter);
    }

    private static FoliantRaid fromEpicenter(BlockPos epicenter) {
        return new FoliantRaid(epicenter);
    }

    public void init(ServerLevel level, FoliantRaidLevelManager manager) {
        this.level = level;
        this.manager = manager;
        for (UUID uuid : spawnedEntities) {
            Entity entity = level.getEntity(uuid);
            if ((!(entity instanceof FoliantRaidMob raidMob)) || !MobUtils.entityIsValidForTargeting(raidMob)) {
                spawnedEntities.remove(uuid);
                continue;
            }
            raidMob.init(this);
        }
    }

    public void onFoliantMobRemoved(FoliantRaidMob raidMob) {

    }

    public CompoundTag save(CompoundTag tag) {

        ListTag spawnedEntitiesTag = new ListTag();
        for (UUID uuid: spawnedEntities) spawnedEntitiesTag.add(NbtUtils.createUUID(uuid));
        tag.put(SPAWNED_ENTITIES_PATH, spawnedEntitiesTag);

        //tag.putUUID(RAID_UUID_PATH, raidUUID);
        tag.putLong(EPICENTER_PATH, epicenter.asLong());

        return tag;
    }

    public static FoliantRaid load(CompoundTag tag) {

        BlockPos epicenter = BlockPos.of(tag.getLong(EPICENTER_PATH));

        FoliantRaid newborn = FoliantRaid.fromEpicenter(epicenter);

        ListTag spawnedEntities = tag.getList(SPAWNED_ENTITIES_PATH, Tag.TAG_INT_ARRAY);
        for (Tag value : spawnedEntities) newborn.registerSpawnedEntity(NbtUtils.loadUUID(value));

        return newborn;
    }

    // gee guess when this is called.
    public void tick() {
        // fixme logger
        LOGGER.info("Ticking raid at epicenter " + getEpicenter());

        if (level.getGameTime() % 100 > 0) return;

        Entity newborn = EntityType.PIG.create(level);
        newborn.moveTo(epicenter.getCenter());
        level.addFreshEntity(newborn);
    }

    // todo document
    /*
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
     */

    public void registerSpawnedEntity(UUID uuid) {
        spawnedEntities.add(uuid);
    }

    /*
    public UUID getRaidUUID() {
        return raidUUID;
    }
     */

    public BlockPos getEpicenter() {
        return epicenter;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public void stop() {

    }

    public static class Spawner {

        public static final int RADIUS = 5;

        // when it finds this amount of good positions, it'll stop looking for more
        public static final int MAX_POSITIONS = 5;
        // if it does this many random searches for a position
        public static final int MAX_ITERATIONS = 10;
        // if it can't find at least this many positions, it says it failed
        public static final int MIN_POSITIONS = 3;

        protected static ArrayList<BlockPos> positions = new ArrayList<>();

        public final BlockPos center;
        public final ServerLevel level;

        protected int ticks = 0;

        // place the center in an already pretty neat spot
        public Spawner(BlockPos center, ServerLevel level) {
            this.center = center;
            this.level = level;
        }

        // returns whether or not it was able to generate positions
        public boolean generatePositions() {
            positions.clear();

            Entity copeZombie = EntityType.ZOMBIE.create(level);
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                if (positions.size() >= MAX_ITERATIONS) break;
                RandomSource random = level.getRandom();
                BlockPos tested = center
                        .east(RADIUS).west(random.nextInt(RADIUS * 2))
                        .north(RADIUS).south(random.nextInt(RADIUS * 2));

                // don't spawn them inside a block
                // todo put this all into a separate function for overriding GOODNIGHTHT

                if (level.getBlockState(tested).isSuffocating(level, tested)) continue;

                BlockPos groundPos = LevelUtils.findSolidGroundBelow(5, level, tested);

                if (groundPos == null) continue;

                BlockPos air1AboveGround = groundPos.above();
                BlockPos air2AboveGround = air1AboveGround.above();
                if (level.getBlockState(air1AboveGround).isSuffocating(level, air1AboveGround)) continue;
                if (level.getBlockState(air2AboveGround).isSuffocating(level, air2AboveGround)) continue;

                positions.add(tested);
            }

            return positions.size() >= MIN_POSITIONS;
        }

        public enum State {
            INITIALIZIING,
            WORKING
        }
    }

    public enum EnemyType {
        GREG(ModEntities.GREG::get),
        DEVIL(ModEntities.DEVIL::get),
        ASTEIRTO(ModEntities.ASTEIRTO::get),
        POSTERBOY(ModEntities.GREG::get),
        AMELIE(ModEntities.GREG::get);

        private final Supplier<EntityType<? extends FoliantRaidMob>> registry;
        EnemyType(Supplier<EntityType<? extends FoliantRaidMob>> registry) {
            this.registry = registry;
        }

        public Supplier<EntityType<? extends FoliantRaidMob>> getRegistry() {
            return registry;
        }
    }
}
