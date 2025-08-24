package com.robotemployee.reu.banana.entity.ai;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.util.MobUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;

public class FollowMobTypeGoal extends Goal {

    protected final Mob follower;
    // in the case that you want to store the target elsewhere, there are methods you can override for getting and setting it
    private LivingEntity target;
    protected final Class<? extends LivingEntity> targetedClass;
    protected final double speedModifier;
    protected final double scanRange;
    protected final double desiredMaxDistance;
    protected final double desiredMinDistance;
    protected final int scanInterval;
    protected int ticksUntilScan;

    static final Logger LOGGER = LogUtils.getLogger();

    public FollowMobTypeGoal(Mob follower, Class<? extends LivingEntity> targetedClass, double speedModifier, double desiredMinDistance, double desiredMaxDistance, double scanRange, int scanInterval) {
        this.follower = follower;
        this.targetedClass = targetedClass;
        this.speedModifier = speedModifier;
        this.desiredMaxDistance = desiredMaxDistance;
        this.desiredMinDistance = desiredMinDistance;
        this.scanRange = scanRange;
        this.scanInterval = scanInterval;
    }

    protected Comparator<? super LivingEntity> getComparator() {
        // note that the entities are swapped around because the filter actually uses max
        // it is this way so that if you override this function, it is intuitive enough to just say "bigger number wins"
        return (entityA, entityB) -> Double.compare(getWeightForEntity(entityA), getWeightForEntity(entityB));
    }

    protected double getWeightForEntity(LivingEntity entity) {
        return Math.pow(scanRange, 2) - follower.distanceToSqr(entity);
    }

    @Override
    public boolean canUse() {
        // LOGGER.info(ticksUntilScan + " is ticks until scan");
        if (ticksUntilScan-- > 0) return false;
        ticksUntilScan = scanInterval;

        LivingEntity candidate = scan();

        if (candidate == null) {
            if (getTarget() != null) removeTarget();
            return false;
        }
        setTarget(candidate);

        return true;
    }

    @Nullable
    public LivingEntity scan() {
        LOGGER.info("Scanning for entities...");
        List<? extends LivingEntity> foundEntities = follower.level().getEntitiesOfClass(targetedClass, follower.getBoundingBox().inflate(scanRange));

        if (foundEntities.isEmpty()) return null;
        LOGGER.info("Found potentially valid entities: " + foundEntities.size());
        LivingEntity candidate = foundEntities.stream().filter(this::isValidTarget).max(getComparator()).orElse(null);
        LOGGER.info("Candidate is " + candidate);
        return candidate;
    }

    public boolean isValidTarget(@NotNull LivingEntity entity) {
        return MobUtils.entityIsValidForTargeting(entity) && entity.getUUID() != follower.getUUID() && follower.distanceToSqr(entity) < Math.pow(scanRange, 2);
    }

    protected void move() {
        BlockPos targetPos = getMoveToPos();
        Path path = pathFromPositionOfTarget(targetPos);
        if (path == null) return;
        //LOGGER.info("Moving to " + path);
        follower.getNavigation().moveTo(path, speedModifier);
    }

    protected Path pathFromPositionOfTarget(BlockPos targetPos) {
        return follower.getNavigation().createPath(targetPos, (int)Math.ceil(scanRange) + 5);
    }

    // override this if you want a simple circle around the target
    protected BlockPos getMoveToPos() {
        return getTarget().blockPosition();
    }

    @Override
    public boolean canContinueToUse() {
        return isValidTarget(getTarget());
    }

    @Override
    public void start() {
        if (getTarget() == null) return;
        move();
    }

    @Override
    public void tick() {
        if (getTarget() == null) return;
        float distance = follower.distanceTo(getTarget());
        boolean badDistance = distance < desiredMinDistance || distance > desiredMaxDistance;
        boolean navReady = follower.getNavigation().isDone() || follower.getNavigation().isStuck();
        if (badDistance && navReady) {
            move();
        }
    }

    @Override
    public void stop() {
        removeTarget();
        follower.getNavigation().stop();
    }

    public LivingEntity getTarget() {
        return target;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    public void removeTarget() {
        setTarget(null);
    }
}
