package com.robotemployee.reu.mixin.sculkhorde;

import com.robotemployee.reu.compat.SculkHordeCompat;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(com.github.sculkhorde.common.entity.components.TargetParameters.class)
public class TargetParametersMixin {
    @Inject(method = "isEntityValidTarget", at = @At("HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void isEntityValidTarget(LivingEntity e, boolean validatingExistingTarget, CallbackInfoReturnable<Boolean> cir) {
        if (validatingExistingTarget && SculkHordeCompat.isOutOfBounds(e)) cir.setReturnValue(false);
    }
}
