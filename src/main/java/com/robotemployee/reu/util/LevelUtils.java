package com.robotemployee.reu.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Function;

public class LevelUtils {
    /**
     * <p>Given a Level and start position, it returns the first BlockPos it finds that matches given conditions. After each failed condition check, it applies a transformation.</p>
     * @implNote It will stop evaluating and return null if it can't find a result within either the maxSearchDepth or 256 tries.
     * @param level The level to use.
     * @param startingPos The first block position checked and to be transformed.
     * @param evaluator Predicate which returns true when the block is what you're looking for.
     * @param transformer Returns the next block position to evaluate.
     * @return The block that satisfies the condition. Null in the case that it cannot find a block.
     * */
    @Nullable
    public static BlockPos iterateBlockWithTransformation(int maxSearchDepth, Level level, BlockPos startingPos, BiPredicate<Level, BlockPos> evaluator, Function<BlockPos, BlockPos> transformer) {
        final int maxDepth = Math.min(maxSearchDepth, 256);
        int searches = 0;
        BlockPos result = startingPos;
        while (!evaluator.test(level, result)) {
            result = transformer.apply(result);
            if (searches++ >= maxDepth) return null;
        }
        return result;
    }

    @Nullable
    public static BlockPos findSolidGroundBelow(Level level, BlockPos startingPos) {
        return findSolidGroundBelow(128, level, startingPos);
    }

    @Nullable
    public static BlockPos findSolidGroundBelow(int maxDepth, Level level, BlockPos startingPos) {

        BiPredicate<Level, BlockPos> evaluator = (lvl, pos) -> {
            BlockState state = lvl.getBlockState(pos);
            // air comparison is present to make this hopefully faster?? since that is almost certainly the thing you will be comparing the most
            return !state.isAir() && !state.getCollisionShape(lvl, pos).isEmpty();
        };
        Function<BlockPos, BlockPos> transformer = BlockPos::below;
        return iterateBlockWithTransformation(maxDepth, level, startingPos, evaluator, transformer);
    }

    @Nullable
    public static BlockPos findSolidGroundBelow(Entity entity) {
        return findSolidGroundBelow(entity.level(), entity.blockPosition());
    }

    @Nullable
    public static BlockPos findSolidGroundBelow(int maxDepth, Entity entity) {
        return findSolidGroundBelow(maxDepth, entity.level(), entity.blockPosition());
    }
}
