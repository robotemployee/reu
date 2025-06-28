package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.systems.chunk_cursor_system.ChunkCursorInfector;
import com.robotemployee.reu.compat.SculkHordeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkCursorInfector.class)
public class ChunkCursorInfectorMixin {
    @Inject(method = "isObstructed", at = @At("HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void isObstructed(ServerLevel serverLevel, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (SculkHordeCompat.isOutOfBounds(pos, serverLevel)) cir.setReturnValue(true);
    }
}
