package com.robotemployee.reu.banana.entity.ai;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;

public class WanderGoal extends Goal {

    static final Logger LOGGER = LogUtils.getLogger();

    final PathfinderMob wanderer;
    final float chance;
    final int range;

    public WanderGoal(LivingEntity wanderer, float chance, int range) {
        this.wanderer = Objects.requireNonNull((PathfinderMob)wanderer);
        this.chance = chance;
        this.range = range;
    }

    @Override
    public boolean canUse() {
        return wanderer.getNavigation().isDone() && wanderer.getRandom().nextFloat() < chance;
    }

    @Override
    public boolean canContinueToUse() {
        return wanderer.getNavigation().isInProgress() && !wanderer.getNavigation().isStuck();
    }

    @Override
    public void start() {
        // FIXME logger
        LOGGER.info("wandering");
        BlockPos wanderPos = getWanderPos();
        if (wanderPos == null) return;
        Path path = wanderer.getNavigation().createPath(wanderPos, range + 5);
        if (path == null) {
            //LOGGER.info("No path found.");
            return;
        }
        //LOGGER.info("info: " + path.canReach() + path.getNodeCount() + path.getEndNode().asBlockPos() + path.getTarget());
        wanderer.getNavigation().moveTo(path, 1);
        //LOGGER.info("wandering to " + wanderPos);
    }

    @Nullable
    public BlockPos getWanderPos() {
        // todo: implement
        return null;
    }
}
