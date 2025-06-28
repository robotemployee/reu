package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.systems.cursor_system.VirtualSurfaceInfestorCursor;
import com.robotemployee.reu.compat.SculkHordeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VirtualSurfaceInfestorCursor.class)
public class VirtualSurfaceInfestorCursorMixin {

    @Inject(method = "isObstructed", at = @At(value = "HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void isObstructed(BlockState state, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (SculkHordeCompat.isOutOfBounds(pos)) cir.setReturnValue(true);
    }
}
