package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.systems.infestation_systems.block_infestation_system.BlockInfestationSystem;
import com.robotemployee.reu.compat.SculkHordeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(BlockInfestationSystem.class)
public class BlockInfestationSystemMixin {
    @Inject(method = "isInfectable", at = @At(value = "HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    private static void isInfectable(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (SculkHordeCompat.isOutOfBounds(pos, level)) cir.setReturnValue(false);
    }

    @Inject(method = "tryToInfestBlock", at = @At(value = "HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    private static void tryToInfestBlock(ServerLevel world, BlockPos targetPos, CallbackInfo ci) {
        if (SculkHordeCompat.isOutOfBounds(targetPos, world)) ci.cancel();
    }

    // replace dirt with grass block if it can see the sky
    // 50% chance
    @Inject(method = "tryToCureBlock", at = @At(value = "TAIL"), remap = SculkHordeCompat.remapNormalSculkHorde)
    private static void tryToCureBlock(ServerLevel world, BlockPos targetPos, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            BlockState blockState = world.getBlockState(targetPos);
            if (blockState.is(Blocks.DIRT) && !reu$isGrassObstructingBlock(world.getBlockState(targetPos.above())) && world.random.nextBoolean()) {
                world.setBlock(targetPos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            }
        }
    }

    @Unique
    private static boolean reu$isGrassObstructingBlock(@NotNull BlockState bs) {
        return !(bs.isAir() || bs.is(Blocks.GRASS));
    }
}
