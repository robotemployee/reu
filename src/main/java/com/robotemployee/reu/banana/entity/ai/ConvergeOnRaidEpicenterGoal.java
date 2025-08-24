package com.robotemployee.reu.banana.entity.ai;

import com.robotemployee.reu.banana.entity.BananaRaidMob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

public class ConvergeOnRaidEpicenterGoal extends Goal {

    public final BananaRaidMob banana;
    public final double speedMod;
    public final double maxDistanceSqr;
    public ConvergeOnRaidEpicenterGoal(BananaRaidMob banana, double speedMod, double maxDistance) {
        this.banana = banana;
        this.speedMod = speedMod;
        this.maxDistanceSqr = Math.pow(maxDistance, 2);
    }

    @Override
    public boolean canUse() {
        if (!banana.isInRaid()) return false;
        if (banana.getRandom().nextInt(8) > 0) return false;
        if (banana.getTarget() != null) return false;

        return true;
    }

    @Override
    public void start() {
        BlockPos raidPos = banana.getParentRaid().getEpicenter();
        banana.getNavigation().moveTo(raidPos.getX(), raidPos.getY(), raidPos.getZ(), speedMod);
    }

    @Override
    public boolean canContinueToUse() {
        if (!banana.isInRaid()) return false;
        BlockPos raidPos = banana.getParentRaid().getEpicenter();
        return banana.blockPosition().distSqr(raidPos) > maxDistanceSqr;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return false;
    }
}
