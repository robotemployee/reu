package com.robotemployee.reu.mixin.mimi;

import com.mojang.logging.LogUtils;
import io.github.tofodroid.mods.mimi.common.tile.TileEffectEmitter;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEffectEmitter.class)
public abstract class TileEffectEmitterMixin {

    @Unique
    Logger reu$LOGGER = LogUtils.getLogger();
    @Shadow private Float _volume;

    @Shadow public abstract Float getVolume();

    @Inject(method = "cacheEffectSettings", at = @At("TAIL"), remap = false)
    private void cacheEffectSettings(CallbackInfo ci) {
        reu$LOGGER.info("Setting new volume to " + this._volume * 100);
        this._volume *= 100;
    }

    @Inject(method = "playSoundLocal", at = @At("TAIL"), remap = false)
    private void playSoundLocal(CallbackInfo ci) {
        reu$LOGGER.info("playing local sound with volume " + this.getVolume());
    }
}
