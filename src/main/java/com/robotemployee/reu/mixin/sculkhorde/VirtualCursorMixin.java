package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.systems.cursor_system.VirtualCursor;
import com.robotemployee.reu.compat.SculkHordeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VirtualCursor.class)
public class VirtualCursorMixin {

    @Inject(method = "isObstructed", at = @At(value = "HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void isObstructed(BlockState state, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (SculkHordeCompat.isOutOfBounds(pos)) cir.setReturnValue(true);
    }
    @Inject(method = "isTarget", at = @At(value = "HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void isTarget(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (SculkHordeCompat.isOutOfBounds(pos)) cir.setReturnValue(false);
    }
}
