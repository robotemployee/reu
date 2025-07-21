package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.core.ModSavedData;
import com.robotemployee.reu.extra.SculkHordeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModSavedData.class)
public class ModSavedDataMixin {
    @Inject(method = "reportDeath", at = @At("HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void reportDeath(ServerLevel level, BlockPos deathPosition, CallbackInfo ci) {
        if (SculkHordeCompat.isOutOfBounds(deathPosition, level)) ci.cancel();
    }
}
