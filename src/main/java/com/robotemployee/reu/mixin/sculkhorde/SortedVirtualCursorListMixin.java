package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.systems.cursor_system.CursorSystem;
import com.github.sculkhorde.systems.cursor_system.ICursor;
import com.robotemployee.reu.compat.SculkHordeCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//@Mixin(targets = "com/github/sculkhorde/systems/cursor_system/CursorSystem$SortedVirtualCursorList")
@Mixin(CursorSystem.SortedVirtualCursorList.class)
public class SortedVirtualCursorListMixin {
    @Inject(method = "shouldCursorBeDeleted", at = @At("HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void shouldCursorBeDeleted(ICursor cursor, CallbackInfoReturnable<Boolean> cir) {
        if (SculkHordeCompat.isOutOfBounds(cursor.getBlockPosition(), cursor.getLevel())) cir.setReturnValue(true);
    }
}