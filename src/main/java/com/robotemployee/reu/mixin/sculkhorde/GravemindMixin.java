package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.systems.gravemind_system.Gravemind;
import com.github.sculkhorde.systems.gravemind_system.entity_factory.ReinforcementRequest;
import com.robotemployee.reu.extra.SculkHordeCompat;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gravemind.class)
public class GravemindMixin {

    @Inject(method = "processReinforcementRequest", at = @At("HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void processReinforcementRequest(ReinforcementRequest context, CallbackInfo ci) {
        // first check the dimension. then check every position
        if (SculkHordeCompat.isLevelOutOfBounds(context.dimension)) ci.cancel();
        else for (BlockPos pos : context.positions) if (pos != null && SculkHordeCompat.isOutOfBounds(pos)) ci.cancel();
    }
}
