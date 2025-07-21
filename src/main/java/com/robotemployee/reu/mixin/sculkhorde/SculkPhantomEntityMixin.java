package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.common.entity.SculkPhantomEntity;
import com.robotemployee.reu.extra.SculkHordeCompat;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkPhantomEntity.class)
public class SculkPhantomEntityMixin {

    @Inject(method = "isAnchorPosValid", at = @At(value = "HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void isAnchorPosValid(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (SculkHordeCompat.isOutOfBounds(pos)) cir.setReturnValue(false);
    }



    /*
    @Inject(method = "tick", at = @At(value = "HEAD"), cancellable = true)
    protected void tick(CallbackInfo ci) {
        SculkPhantomEntity self = (SculkPhantomEntity)(Object)this;
        if (SculkHordeCompat.isOutOfBounds(self.blockPosition())) {
            self.remove(Entity.RemovalReason.DISCARDED);
            ci.cancel();
        }
    }
     */
}
