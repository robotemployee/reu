package com.robotemployee.reu.foliant;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.foliant.entity.FoliantRaidMob;
import com.robotemployee.reu.util.IntegerWeightedRandomList;
import com.robotemployee.reu.util.LevelUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.level.pathfinder.Path;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Spawner {

    private static final Logger LOGGER = LogUtils.getLogger();

    protected static final int RADIUS = 5;

    // when it finds this amount of good positions, it'll stop looking for more
    protected static final int MAX_POSITIONS = 5;
    // if it does this many random searches for a position
    protected static final int MAX_ITERATIONS = 10;
    // if it can't find at least this many positions, it says it failed
    protected static final int MIN_POSITIONS = 3;

    protected ArrayList<BlockPos> positions = new ArrayList<>();

    protected final Deque<SpawnEntry> queue = new ArrayDeque<>();

    public final BlockPos center;
    public final ServerLevel level;
    public final FoliantRaid parentRaid;

    protected static final int INIT_TICKS = 100;
    protected int ticksUntilStateChange = INIT_TICKS;
    protected static final int TICKS_BETWEEN_REVALIDATIONS = 200;
    protected int ticksUntilRevalidation = TICKS_BETWEEN_REVALIDATIONS;

    protected static final int TICKS_BETWEEN_SPAWNS = 100;
    protected int ticksUntilNextSpawn = 100;

    protected boolean isPoop = false;
    protected boolean canReachEpicenter = true;

    State state = State.INITIALIZING;

    // place the center in an already pretty neat spot
    Spawner(FoliantRaid parentRaid, BlockPos center, ServerLevel level) {
        this.parentRaid = parentRaid;
        this.center = center;
        this.level = level;
        generatePositions();
    }

    // returns whether or not it was able to generate positions
    public boolean generatePositions() {
        positions.clear();

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            if (positions.size() >= MAX_ITERATIONS) break;
            RandomSource random = level.getRandom();
            BlockPos tested = center
                    .east(RADIUS).west(random.nextInt(RADIUS * 2))
                    .north(RADIUS).south(random.nextInt(RADIUS * 2));

            if (!isValidPosition(tested)) continue;

            positions.add(tested);
            if (positions.size() >= MAX_POSITIONS) break;
        }

        return positions.size() >= MIN_POSITIONS;
    }

    protected boolean isValidPosition(BlockPos tested) {
        //LOGGER.info("Testing position " + tested);
        if (level.getBlockState(tested).isSuffocating(level, tested)) return false;

        BlockPos groundPos = LevelUtils.findSolidGroundBelow(5, level, tested);

        if (groundPos == null) return false;

        BlockPos air1AboveGround = groundPos.above();
        BlockPos air2AboveGround = air1AboveGround.above();
        if (level.getBlockState(air1AboveGround).isSuffocating(level, air1AboveGround)) return false;
        if (level.getBlockState(air2AboveGround).isSuffocating(level, air2AboveGround)) return false;

        //LOGGER.info("Test passed!");
        return true;
    }

    public void tick() {
        if (isPoop()) return;
        manageState();
        if (isPoop()) return;
        revalidateIfReady();
        if (isPoop()) return;
        populateQueueIfNeeded();
        doSpawns();
    }

    public void manageState() {
        switch (state) {
            case INITIALIZING -> {
                if (isReadyToSwitch()) {
                    /*
                    level.players().forEach(player ->
                            player.sendSystemMessage(Component.literal("Spawner @ " + center + " is initiating"))
                    );
                     */
                    state = State.WORKING;
                    ticksUntilStateChange = 1200;
                }
            }
            case WORKING -> {
                if (isReadyToSwitch()) {
                    // expire due to old age
                    imFucked();
                }
            }
        }
        ticksUntilStateChange--;
    }
    protected boolean isReadyToSwitch() {
        return ticksUntilStateChange <= 0;
    }

    protected void revalidateIfReady() {
        if (ticksUntilRevalidation-- > 0) return;
        ticksUntilRevalidation = TICKS_BETWEEN_REVALIDATIONS;
        trimInvalidPositions();
        // expire because we just cant do it anymore
        if (positions.size() < MIN_POSITIONS) imFucked();

        if (isPoop()) return;

        Bat copeBat = EntityType.BAT.create(level);
        PathNavigation pathNavigation = new FlyingPathNavigation(copeBat, level);
        Path path = pathNavigation.createPath(parentRaid.getEpicenter(), (int) (1.5 * parentRaid.radius));
        canReachEpicenter = !(path == null || !path.canReach() || path.getDistToTarget() > 16);
    }

    protected void doSpawns() {
        //LOGGER.info("DOING SPAWNS. Queue: " + queue);
        if (isReadyToSpawn()) {
            //LOGGER.info("ready to spawn");
            spawnNextEntry();
            ticksUntilNextSpawn = TICKS_BETWEEN_SPAWNS;
        }
        ticksUntilNextSpawn--;
    }

    protected boolean isReadyToSpawn() {
        //LOGGER.info("checking spawn readiness: " + parentRaid.isReadyToSpawn() + " " + (ticksUntilNextSpawn <= 0) + " ticks: " + ticksUntilNextSpawn);
        return parentRaid.isReadyToSpawn() && ticksUntilNextSpawn <= 0;
    }

    protected void populateQueueIfNeeded() {
        if (hasQueuedSpawns()) return;

        //LOGGER.info("ready: " + isReadyToSpawn() + " has queued: " + hasQueuedSpawns());

        //LOGGER.info("POPULATING QUEUE...");

        SpawnEntry entry = generateEntry();
        addToQueue(entry);
    }

    protected SpawnEntry generateEntry() {
        SpawnEntry result = new SpawnEntry();
        int power = getPower();
        IntegerWeightedRandomList<FoliantRaid.EnemyType> candidates = populateWeightedCandidates(new IntegerWeightedRandomList<>(), power);

        // evil evil evil. hate. hate.
        while (candidates.size() > 0) {
            FoliantRaid.EnemyType enemyTypeToSpawn = candidates.pickRandom(level.getRandom());
            int amountToSpawn = enemyTypeToSpawn.getAmountToSpawn();

            result.add(enemyTypeToSpawn.getEntityType(), amountToSpawn);
            // fixme get rid of this SHITASS SOLUTION and put this somewhere it's actually being added to the queue
            parentRaid.incrementPopulation(enemyTypeToSpawn, amountToSpawn);
            parentRaid.addSpawnCooldownFor(enemyTypeToSpawn, amountToSpawn);

            power -= enemyTypeToSpawn.getPowerNeededToSpawn();
            if (power <= 0) break;
            candidates = populateWeightedCandidates(candidates, power);
        }

        //LOGGER.info("resulting spawn entry: " + result);
        return result;
    }

    // ignore this it's not real
    protected IntegerWeightedRandomList<FoliantRaid.EnemyType> populateWeightedCandidates(IntegerWeightedRandomList<FoliantRaid.EnemyType> list, int power) {
        //LOGGER.info("populating weighted candidates...");
        list.clear();
        for (FoliantRaid.EnemyType type : FoliantRaid.EnemyType.values()) {
            //LOGGER.info("Examining enemy type " + type);
            if (!type.overallCanBeSpawned(parentRaid)) continue;
            list.add(Map.entry(type, type.getSpawnWeight()));
        }
        //LOGGER.info(String.format("resulting list [%s]: %s", list.size(), list));
        return list;
    }

    protected int getPower() {
        return parentRaid.getPower();
    }

    protected void trimInvalidPositions() {
        positions = (ArrayList<BlockPos>) positions.stream().filter(this::isValidPosition).collect(Collectors.toList());
    }

    public BlockPos getRandomSpawnPosition() {
        int index = level.getRandom().nextInt(positions.size());
        return positions.get(index);
    }

    public void resetSpawnCooldown() {
        ticksUntilNextSpawn = 0;
    }

    public void addToQueue(SpawnEntry spawnEntry) {
        queue.addFirst(spawnEntry);
    }

    public void addToQueue(Collection<SpawnEntry> otherQueue) {
        queue.addAll(otherQueue);
    }

    public Deque<SpawnEntry> getQueue() {
        return queue;
    }

    public boolean hasQueuedSpawns() {
        return queue.size() > 0;
    }

    protected void spawnNextEntry() {
        SpawnEntry entry = queue.removeLast();

        //LOGGER.info("Spawning the next entry in the queue: " + entry);

        entry.get().forEach(e -> {
            EntityType<? extends FoliantRaidMob> entityType = e.getKey();
            int amount = e.getValue();
            //LOGGER.info(String.format("Spawning %sx %s...", amount, entityType));

            for (int i = 0; i < amount; i++) {
                FoliantRaidMob newborn = entityType.create(level);
                newborn.moveTo(getRandomSpawnPosition().getCenter());
                newborn.init(parentRaid);
                level.addFreshEntity(newborn);
                //LOGGER.info("Added entity to level: " + newborn);
            }
        });
    }

    public boolean isWithinRadius(BlockPos pos) {
        return pos.distSqr(center) <= Math.pow(RADIUS, 2);
    }

    protected void imFucked() {
        isPoop = true;
    }

    public boolean isPoop() {
        return isPoop;
    }

    public boolean canReachEpicenter() {
        return canReachEpicenter;
    }

    public void onRemoved() {

    }

    // in hindsight this isn't really necessary to make its own class
    // i was unsure of how i wanted to represent queued spawns, and i've been developing it with a class and...
    // it's not gonna be unperformant or anything, it'll just be weird. that's fine
    public static class SpawnEntry {
        // in one bundle, you can spawn different amounts of different enemy types
        // this will make it easier to scale later
        private final HashMap<EntityType<? extends FoliantRaidMob>, Integer> enemiesToSpawn = new HashMap<>();

        private SpawnEntry() {

        }

        public static SpawnEntry of() {
            return new SpawnEntry();
        }

        public void add(EntityType<? extends FoliantRaidMob> mob, Integer amount) {
            enemiesToSpawn.compute(mob, (key, value) -> value == null ? amount : amount + value);
        }

        public Stream<Map.Entry<EntityType<? extends FoliantRaidMob>, Integer>> get() {
            return enemiesToSpawn.entrySet().stream();
        }

        @Override
        public String toString() {
            return super.toString() + " containing " + enemiesToSpawn;
        }
    }

    public enum State {
        INITIALIZING,
        WORKING
    }
}
