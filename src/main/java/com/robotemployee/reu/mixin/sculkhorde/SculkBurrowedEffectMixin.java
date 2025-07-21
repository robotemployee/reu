package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.common.effect.SculkBurrowedEffect;
import com.robotemployee.reu.extra.SculkHordeCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkBurrowedEffect.class)
public class SculkBurrowedEffectMixin {

    // remap = false unconditionally for these because,,,,, this is something that is extended from vanilla

    // note that this is a vanilla method and so it should be remapped
    // alright well, it wouldn't generate the remappings but y'know the onPotionExpire thing works and that's ! good ! enough!
    /*
    @Inject(method = "applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)V", at = @At("TAIL"), cancellable = true, remap = SculkHordeCompat.remapDeobfSculkHorde)
    void applyEffectTick(LivingEntity entity, int amplifier, CallbackInfo ci) {
        if (SculkHordeCompat.isOutOfBounds(entity)) {
            entity.removeEffect(ModMobEffects.SCULK_INFECTION.get());
            ci.cancel();
        }
    }
    */


    @Inject(method = "onPotionExpire", at = @At("HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void onPotionExpire(MobEffectEvent.Expired event, CallbackInfo ci) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide &&  SculkHordeCompat.isOutOfBounds(entity)) ci.cancel();
    }

    @Inject(method = "placeSculkMass", at = @At("HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    private static void placeSculkMass(LivingEntity entity, CallbackInfo ci) {
        if (SculkHordeCompat.isOutOfBounds(entity)) ci.cancel();
    }
}
