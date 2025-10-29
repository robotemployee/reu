package com.robotemployee.reu.foliant.entity.ai;

import com.robotemployee.reu.foliant.entity.FoliantRaidMob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

public class ConvergeOnRaidEpicenterGoal extends Goal {
    public final FoliantRaidMob foliant;
    public final double speedMod;
    public static final int TARGET_DISTANCE_SQR = (int)Math.pow(128, 2);
    public ConvergeOnRaidEpicenterGoal(FoliantRaidMob foliant, double speedMod) {
        this.foliant = foliant;
        this.speedMod = speedMod;
    }

    @Override
    public boolean canUse() {
        if (!foliant.isInRaid()) return false;
        if (foliant.getRandom().nextInt(8) > 0) return false;
        if (foliant.getTarget() != null) return false;

        return true;
    }

    @Override
    public void start() {
        BlockPos raidPos = foliant.getParentRaid().getEpicenter();
        foliant.getNavigation().moveTo(raidPos.getX(), raidPos.getY(), raidPos.getZ(), speedMod);
    }

    @Override
    public boolean canContinueToUse() {
        if (!foliant.isInRaid()) return false;
        BlockPos raidPos = foliant.getParentRaid().getEpicenter();
        return foliant.blockPosition().distSqr(raidPos) > TARGET_DISTANCE_SQR;
    }
}
