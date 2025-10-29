package com.robotemployee.reu.foliant;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Supplier;

public class FoliantRaid {

    // todo
    // TEST make it save spawners to the SavedData DONE
    // make it place spawners around DONE
    // determine rules for when it should make a second spawner DONE
    // make them mine or make an explosion or something if there are no valid paths anywhere
    // ... though asteirto might help with that

    // spawns per minute system: based on the power, there is a target "spawns per minute"
    // spawners automatically adjust their spawn time by taking this rate and dividing it by the amount of spawners
    // amount of spawners = power / 5 or something, max to 6 i'd say

    private static final Logger LOGGER = LogUtils.getLogger();

    private FoliantRaidLevelManager manager;

    private final ArrayList<UUID> spawnedEntities = new ArrayList<>();

    private ServerLevel level;
    private final BlockPos epicenter;

    // gives attribute modifiers to spawned enemies, unlocks abilities
    protected float power = 0;

    // radius of the raid
    protected float radius = 32;
    // if this is true, the raid should end
    protected boolean isPoop = false;

    private final HashMap<BlockPos, Spawner> spawners = new HashMap<>();
    private final HashMap<EnemyType, ReplacementCooldown> replacementCooldowns = new HashMap<>();

    private final HashMap<EnemyType, Integer> population = new HashMap<>();

    private final HashSet<ChunkPos> loadedChunks = new HashSet<>();

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

        startForceloadingChunks();
    }

    private static final String EPICENTER_PATH = "Epicenter";
    private static final String SPAWNED_ENTITIES_PATH = "SpawnedEntityUUIDs";
    private static final String SPAWNERS_PATH = "Spawners";

    public CompoundTag save(CompoundTag tag) {
        ListTag spawnedEntitiesTag = new ListTag();
        for (UUID uuid: spawnedEntities) spawnedEntitiesTag.add(NbtUtils.createUUID(uuid));
        tag.put(SPAWNED_ENTITIES_PATH, spawnedEntitiesTag);

        ListTag spawnersTag = new ListTag();
        for (BlockPos pos : getSpawners()) spawnersTag.add(NbtUtils.writeBlockPos(pos));
        tag.put(SPAWNED_ENTITIES_PATH, spawnersTag);

        //tag.putUUID(RAID_UUID_PATH, raidUUID);
        tag.putLong(EPICENTER_PATH, epicenter.asLong());

        return tag;
    }

    public static FoliantRaid load(CompoundTag tag) {

        BlockPos epicenter = BlockPos.of(tag.getLong(EPICENTER_PATH));

        FoliantRaid newborn = FoliantRaid.fromEpicenter(epicenter);

        ListTag spawnedEntities = tag.getList(SPAWNED_ENTITIES_PATH, Tag.TAG_INT_ARRAY);
        for (Tag value : spawnedEntities) newborn.registerSpawnedEntity(NbtUtils.loadUUID(value));

        ListTag existingSpawners = tag.getList(SPAWNERS_PATH, Tag.TAG_COMPOUND);
        for (Tag value : existingSpawners) newborn.createSpawner(NbtUtils.readBlockPos((CompoundTag) value));

        return newborn;
    }

    // gee guess when this is called.
    public void tick() {
        tickSpawners();
        tickReplacementCooldowns();
    }

    protected void tickSpawners() {
        // this function is responsible for placing new spawners and ticking existing ones



        Iterator<Map.Entry<BlockPos, Spawner>> iterator = spawners.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Spawner> entry = iterator.next();
            Spawner spawner = entry.getValue();
            if (spawner.isPoop()) {
                spawner.onRemoved();
                iterator.remove();
            }

            if (!spawner.canReachEpicenter()) {
                //LOGGER.info("Spawner can't reach epicenter :(");
            }

            spawner.tick();
        }

        int amountOfSpawners = spawners.size();
        // amount of spawners = power / 10
        int desiredSpawners = ((int)power / 10) + 1;
        //LOGGER.info("desiredSpawners: " + desiredSpawners + " amountOfSpawners: " + amountOfSpawners);
        if (amountOfSpawners < desiredSpawners) {
            int spawnersLeftToCreate = desiredSpawners - amountOfSpawners;
            int maxIterations = spawnersLeftToCreate * 3;
            for (int i = 0; i < maxIterations; i++) {
                BlockPos potentialPosition = tryToFindValidSpawnerPosition();
                if (potentialPosition == null) continue;
                //LOGGER.info("Successfully created spawner at " + potentialPosition);
                createSpawner(potentialPosition);
                if (--spawnersLeftToCreate <= 0) break;
            }
        }
    }

    protected void tickReplacementCooldowns() {
        for (ReplacementCooldown cooldown : replacementCooldowns.values()) {
            cooldown.tick();
        }
    }

    @Nullable
    protected BlockPos tryToFindValidSpawnerPosition() {
        RandomSource random = level.getRandom();
        int targetDistance = random.nextInt((int)(radius * 0.5), (int)(radius - 10));
        double angle = random.nextFloat() * 2 * Math.PI;

        BlockPos randomizedLocation = new BlockPos((int)(Math.cos(angle) * targetDistance), 0, (int)(Math.sin(angle) * targetDistance)).offset(epicenter);

        BlockPos groundBelowPos = LevelUtils.findSolidGroundBelow(32, level, randomizedLocation);
        BlockPos groundAbovePos = LevelUtils.iterateBlockWithTransformation(32, level, randomizedLocation,
                (lvl, pos) -> {
                    BlockState state = lvl.getBlockState(pos);
                    BlockState aboveState = lvl.getBlockState(pos.above());
                    return (!state.isAir() && !state.getCollisionShape(lvl, pos).isEmpty()) && (!aboveState.isAir() && !aboveState.getCollisionShape(lvl, pos).isEmpty());
                },
                BlockPos::above
                );

        // ignore this horrendous code

        BlockPos result = null;

        int distanceToGroundBelow = 0;
        if (groundBelowPos != null) distanceToGroundBelow = randomizedLocation.getY() - groundBelowPos.getY();

        int distanceToGroundAbove = 0;
        if (groundAbovePos != null) distanceToGroundAbove = groundAbovePos.getY() - randomizedLocation.getY();

        if ((groundBelowPos == null || distanceToGroundBelow > distanceToGroundAbove) && groundAbovePos != null) {
            result = groundAbovePos.above();
        } else if (groundBelowPos != null) {
            result = groundBelowPos.above();
        }
        return result;
    }

    // These three are called by FoliantRaidMob, it is a way to keep track of population safely without WeakReference or anything like that
    // population is INCREMENTED when a Spawner adds them to the queue. this is so that multiple spawners don't cover the same basis
    // population is DECREMENTED when the entity is removed from the raid.
    public void incrementPopulation(EnemyType type) {
        incrementPopulation(type, 1);
    }

    public void incrementPopulation(EnemyType type, int amount) {
        population.compute(type, (key, value) -> value == null ? amount : value + amount);
    }

    public void decrementPopulation(EnemyType type) {
        decrementPopulation(type, 1);
    }

    public void decrementPopulation(EnemyType type, int amount) {
        population.compute(type, (key, value) -> value == null ? 0 : value - amount);
    }

    public int getPopulation(EnemyType type) {
        return population.computeIfAbsent(type, (key) -> 0);
    }

    public int getPopulation() {
        return Arrays.stream(EnemyType.values()).mapToInt(this::getPopulation).sum();
    }

    protected void startForceloadingChunks() {
        int chunkRadius = Math.min(1, (int)Math.ceil(radius / 16) - 1);

        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                ChunkPos pos = new ChunkPos(x + epicenter.getX() / 16, z + epicenter.getZ() / 16);
                loadedChunks.add(pos);
                ForgeChunkManager.forceChunk(level, RobotEmployeeUtils.MODID, epicenter, pos.x, pos.z, true, true);
            }
        }
    }

    protected void stopForceloadingChunks() {
        loadedChunks.forEach(pos -> {
            // remove the forced chunk
            ForgeChunkManager.forceChunk(level, RobotEmployeeUtils.MODID, epicenter, pos.x, pos.z, false, true);
        });
        loadedChunks.clear();
    }

    public void registerSpawnedEntity(UUID uuid) {
        spawnedEntities.add(uuid);
    }

    public BlockPos getEpicenter() {
        return epicenter;
    }

    public ServerLevel getLevel() {
        return level;
    }

    // you stop raids through FoliantRaidLevelManager
    // this is only to notify the raid that it's been stopped
    void onStopped() {
        isPoop = true;
        stopForceloadingChunks();
    }

    public boolean isPoop() {
        return isPoop;
    }

    public boolean isPositionContained(BlockPos pos) {
        return pos.distSqr(epicenter) <= Math.pow(radius, 2);
    }

    public boolean isSpawnerCenteredAt(BlockPos pos) {
        return spawners.values().stream().anyMatch(spawner -> spawner.center == pos);
    }

    // does not create a spawner if there's one already there

    public boolean createSpawner(BlockPos center) {
        if (isSpawnerCenteredAt(center)) return false;
        Spawner spawner = new Spawner(this, center, level);
        spawners.put(center, spawner);
        return true;
    }

    // wave-based system maybe??
    public boolean isReadyToSpawn() {
        return true;
    }

    public boolean isSpawnOffCooldown(EnemyType type) {
        ReplacementCooldown cooldown = replacementCooldowns.computeIfAbsent(type, key -> {
            return new ReplacementCooldown(key, this);
        });

        return cooldown.isCooldownOver();
    }

    public void addSpawnCooldownFor(EnemyType type, int amount) {
        ReplacementCooldown cooldown = replacementCooldowns.computeIfAbsent(type, key -> {
            return new ReplacementCooldown(key, this);
        });
        cooldown.onSpawned();
    }

    public void refreshSpawnerCooldowns() {
        spawners.forEach((pos, spawner) -> spawner.resetSpawnCooldown());
    }

    public void removeSpawner(BlockPos center) {
        spawners.remove(center).onRemoved();
    }

    protected void removeSpawner(Spawner spawner) {
        removeSpawner(spawner.center);
    }

    public void removeAllSpawners() {
        spawners.replaceAll((pos, spawner) -> {
            spawner.onRemoved();
            return null;
        });
    }

    @Nullable
    public BlockPos getSpawnerNear(BlockPos center) {
        return spawners.values().stream().filter(spawner -> spawner.isWithinRadius(center)).findFirst().map(spawner -> spawner.center).orElse(null);
    }

    public void addPower(int added) {
        power += added;
    }

    public void subtractPower(int subtracted) {
        power -= subtracted;
    }

    public void setPower(int newPower) {
        power = newPower;
    }

    // i know. im lazy
    public int getPower() {
        return (int)Math.floor(power);
    }

    public float getPowerFloat() {
        return power;
    }

    public Set<BlockPos> getSpawners() {
        return spawners.keySet();
    }

    public enum EnemyType {
        GREG(ModEntities.GREG::get, 3, 8, 1, 4, 60),
        DEVIL(ModEntities.DEVIL::get, 1, 4, 5, 1, 300),
        ASTEIRTO(ModEntities.ASTEIRTO::get, 1, 2, 15, 2, 1600),
        // posterboy is a special enemy and does not spawn normally
        POSTERBOY(ModEntities.GREG::get, 1, 0, 0, 0, 0),
        AMELIE(ModEntities.GREG::get, 1, -5, -5, 1, 300);

        private final Supplier<EntityType<? extends FoliantRaidMob>> registry;
        // when it is decided that this thing will be spawned, this is the amount to spawn
        // you can consider this as "how many of these is 1 unit?"
        // population is divided by this in overpopulation calculations
        private final int amountToSpawn;
        // the higher this number, the more likely this is to be spawned when there is multiple options
        private final int spawnWeight;
        // if the raid power is at or above this amount, this entity can be spawned. also factors into overpopulation
        private final int powerNeededToSpawn;
        // as this number increases, less of this entity are needed for them to be considered overpopulated
        private final float overpopulationWeight;
        private final int baseRespawnCooldownTicks;
        EnemyType(Supplier<EntityType<? extends FoliantRaidMob>> registry, int amountToSpawn, int spawnWeight, int powerToSpawn, float overpopulationWeight, int baseRespawnCooldownTicks) {
            this.registry = registry;
            this.amountToSpawn = amountToSpawn;
            this.spawnWeight = spawnWeight;
            this.powerNeededToSpawn = powerToSpawn;
            this.overpopulationWeight = overpopulationWeight;
            this.baseRespawnCooldownTicks = baseRespawnCooldownTicks;
        }

        public boolean spawnsNormally() {
            return spawnWeight > 0 && powerNeededToSpawn > 0;
        }

        public int getSpawnWeight() {
            return spawnWeight;
        }

        public int getPowerNeededToSpawn() {
            return powerNeededToSpawn;
        }

        public boolean raidHasEnoughPowerToSpawn(FoliantRaid raid) {
            return getPowerNeededToSpawn() <= raid.getPower();
        }

        public int getAmountToSpawn() {
            return amountToSpawn;
        }

        public boolean isOverpopulated(FoliantRaid raid) {
            int power = raid.getPower();
            int population = raid.getPopulation(this);
            //LOGGER.info(String.format("pee:%s pop:%s cost:%s weight:%s amt:%s pow:%s res:%s", this, population, getPowerNeededToSpawn(), overpopulationWeight, getAmountToSpawn(), power, ((population * getPowerNeededToSpawn() * overpopulationWeight) / getAmountToSpawn()) >= power));
            return ((population * getPowerNeededToSpawn() * overpopulationWeight) / getAmountToSpawn()) >= power;
        }

        public boolean isOffCooldown(FoliantRaid raid) {
            // there are no cooldowns when you get to 100 power. die
            if (raid.getPower() >= 100) return true;
            return raid.isSpawnOffCooldown(this);
        }

        // shut up shut up shut up shut up shut p supt shut up shut up
        public boolean overallCanBeSpawned(FoliantRaid raid) {
            //fixme logger
            LOGGER.info(this + " " + spawnsNormally() + raidHasEnoughPowerToSpawn(raid) + !isOverpopulated(raid) + isOffCooldown(raid));
            return spawnsNormally() &&
                    raidHasEnoughPowerToSpawn(raid) &&
                    !isOverpopulated(raid) &&
                    isOffCooldown(raid);
        }

        public int getTicksBetweenRespawns(FoliantRaid raid) {
            int power = raid.getPower();
            //double cooldownSpeedCoefficient = Math.pow(power, 1.2) * 0.02 + 1; i forgot
            //double cooldownSpeedCoefficient = Math.pow(power, 3) * 0.00005 + 1; too exponential

            // 8% faster respawns for each unit of power; by 40 power, respawns will be 4x as fast. sounds fine enough to me
            // i think 40 is a good "you are very far" point
            double cooldownSpeedCoefficient = power * 0.08 + 1;
            return (int) Math.ceil(baseRespawnCooldownTicks / cooldownSpeedCoefficient);
        }

        public EntityType<? extends FoliantRaidMob> getEntityType() {
            return registry.get();
        }

        public <T extends FoliantRaidMob> T create(ServerLevel level) {
            return (T) getEntityType().create(level);
        }
    }

    // when something is removed from the raid by death or something else, the raid wants to replace them
    // since there will no longer be enough of them
    // when the thing is
    public static class ReplacementCooldown {
        private final FoliantRaid raid;
        private final EnemyType type;
        private final ArrayList<Integer> cooldowns = new ArrayList<>();

        public ReplacementCooldown(EnemyType type, FoliantRaid raid) {
            this.type = type;
            this.raid = raid;
        }

        public void addCooldown() {
            int cooldown = getTicksBetweenRespawns();
            cooldowns.add(cooldown);
        }

        public int getTicksBetweenRespawns() {
            return type.getTicksBetweenRespawns(raid);
        }

        public boolean isCooldownOver() {
            return getCount() == 0 || cooldowns.stream().anyMatch(cooldown -> cooldown <= 0);
        }

        public void tick() {
            cooldowns.replaceAll(old -> old - 1);
        }

        public void onSpawned() {
            Iterator<Integer> iterator = cooldowns.iterator();
            while (iterator.hasNext()) {
                if (iterator.next() <= 0) {
                    iterator.remove();
                    break;
                }
            }
            addCooldown();
        }

        protected int getCount() {
            return cooldowns.size();
        }

        @Override
        public String toString() {
            return super.toString() + " " + cooldowns;
        }
    }
}
