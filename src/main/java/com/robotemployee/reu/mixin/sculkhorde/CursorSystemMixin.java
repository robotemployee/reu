package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.systems.cursor_system.CursorSystem;
import com.github.sculkhorde.systems.cursor_system.VirtualSurfaceInfestorCursor;
import com.robotemployee.reu.compat.SculkHordeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(CursorSystem.class)
public class CursorSystemMixin {

    @Inject(method = "createSurfaceInfestorVirtualCursor", at = @At("HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    private static void createSurfaceInfestorVirtualCursor(Level level, BlockPos pos, CallbackInfoReturnable<Optional<VirtualSurfaceInfestorCursor>> cir) {
        if (SculkHordeCompat.isOutOfBounds(pos, level)) cir.setReturnValue(Optional.empty());
    }
}
