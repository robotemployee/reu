package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.systems.chunk_cursor_system.ChunkCursorHelper;
import com.robotemployee.reu.compat.SculkHordeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkCursorHelper.class)
public class ChunkCursorHelperMixin {
    @Inject(method = "tryToInfestBlock", at = @At(value = "HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    private static void tryToInfestBlock(ServerLevel world, BlockPos targetPos, Boolean noSpawners, CallbackInfo ci) {
        if (SculkHordeCompat.isOutOfBounds(targetPos, world)) ci.cancel();
    }
}
