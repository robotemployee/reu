package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.common.entity.infection.CursorEntity;
import com.github.sculkhorde.systems.cursor_system.CursorSystem;
import com.robotemployee.reu.extra.SculkHordeCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CursorSystem.SortedCursorList.class)
public class SortedCursorListMixin {
    @Inject(method = "shouldCursorBeDeleted", at = @At("HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void shouldCursorBeDeleted(CursorEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (SculkHordeCompat.isOutOfBounds(entity.blockPosition(), entity.level())) cir.setReturnValue(true);
    }
}