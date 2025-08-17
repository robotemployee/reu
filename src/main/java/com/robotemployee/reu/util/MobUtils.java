package com.robotemployee.reu.util;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class MobUtils {

    static final Logger LOGGER = LogUtils.getLogger();

    public static BlockPos getBlockPosOfMinHeight(BlockPos pos, Level level, int minHeightAboveGround) {
        int heightAboveGround;
        for (heightAboveGround = 0; heightAboveGround < minHeightAboveGround; heightAboveGround++) {
            BlockState examined = level.getBlockState(pos.below(heightAboveGround));
            if (!examined.getCollisionShape(level, pos).isEmpty()) break;
        }
        return pos.above(minHeightAboveGround - heightAboveGround);
    }

    public static boolean entityIsValidForTargeting(LivingEntity entity) {
        //LOGGER.info(String.format("Alive: %s, Present: %s", entity.isAlive(), entity.getRemovalReason() == null));
        return entity.isAlive() && entity.getRemovalReason() == null;
    }
}
