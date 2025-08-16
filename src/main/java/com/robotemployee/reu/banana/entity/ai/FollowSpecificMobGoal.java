package com.robotemployee.reu.banana.entity.ai;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;

public class FollowSpecificMobGoal extends Goal {

    protected final Mob follower;
    protected LivingEntity target;
    protected final Class<? extends LivingEntity> targetedClass;
    protected final double speedModifier;
    protected final double maxDistance;
    protected final int scanInterval;
    protected int ticksUntilScan;

    static final Logger LOGGER = LogUtils.getLogger();

    public FollowSpecificMobGoal(Mob follower, Class<? extends LivingEntity> targetedClass, double speedModifier, double maxDistance, int scanInterval) {
        this.follower = follower;
        this.targetedClass = targetedClass;
        this.speedModifier = speedModifier;
        this.maxDistance = maxDistance;
        this.scanInterval = scanInterval;
    }

    protected Comparator<? super LivingEntity> getComparator() {
        // note that the entities are swapped around because the filter actually uses max
        // it is this way so that if you override this function, it is intuitive enough to just say "bigger number wins"
        return (entityA, entityB) -> Double.compare(getWeightForEntity(entityA), getWeightForEntity(entityB));
    }

    protected double getWeightForEntity(LivingEntity entity) {
        return Math.pow(maxDistance, 2) - follower.distanceToSqr(entity);
    }

    @Override
    public boolean canUse() {
        if (ticksUntilScan-- > 0) return false;
        ticksUntilScan = scanInterval;
        List<? extends LivingEntity> foundEntities = follower.level().getEntitiesOfClass(targetedClass, follower.getBoundingBox().inflate(maxDistance));

        if (foundEntities.isEmpty()) return false;
        target = foundEntities.stream().filter(this::isValidTarget).max(getComparator()).orElse(null);

        // LOGGER.info("Target is " + target);

        return target != null;
    }

    public boolean isValidTarget(LivingEntity entity) {
        return entity.getUUID() != follower.getUUID();
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && follower.distanceToSqr(target) < Math.pow(maxDistance, 2);
    }

    protected void move() {
        follower.getNavigation().moveTo(target, speedModifier);
    }

    @Override
    public void start() {
        if (target == null) return;
        move();
    }

    @Override
    public void tick() {
        if (target == null) return;
        move();
    }

    @Override
    public void stop() {
        target = null;
        follower.getNavigation().stop();
    }
}
