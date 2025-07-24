package com.robotemployee.reu.extra;

import com.mojang.logging.LogUtils;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public class BaseGame {

    static Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().is(DamageTypes.SONIC_BOOM) && !event.getEntity().isDeadOrDying()) {
            float damage = event.getAmount();
            damage *= 1.3;
            event.setAmount(damage);
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }
    }


}
