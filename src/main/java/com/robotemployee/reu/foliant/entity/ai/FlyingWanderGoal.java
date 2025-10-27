package com.robotemployee.reu.foliant.entity.ai;

import com.robotemployee.reu.util.MobUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class FlyingWanderGoal extends WanderGoal {

    final int normalizedRange;
    final int minHeightAboveGround;

    public FlyingWanderGoal(LivingEntity wanderer, float chance, int range, int minHeightAboveGround) {
        super(wanderer, chance, range);
        normalizedRange = (int)Math.ceil(range * (Math.sqrt(2) / 2));
        this.minHeightAboveGround = minHeightAboveGround;
    }

    @Override
    public @Nullable BlockPos getWanderPos() {
        Vec3 view = wanderer.getViewVector(0);
        Vec3 hover = HoverRandomPos.getPos(wanderer, normalizedRange, normalizedRange, view.x, view.z, (float) Math.PI, 3, 1);
        Vec3 result = hover != null ? hover : AirAndWaterRandomPos.getPos(wanderer, 8, 4, -2, view.x, view.z, Math.PI);

        if (result == null) return null;

        BlockPos pos = BlockPos.containing(result);
        return MobUtils.getBlockPosOfMinHeight(pos, wanderer.level(), minHeightAboveGround);
    }
}
