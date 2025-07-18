package com.robotemployee.reu.compat;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public class BaseGame {

    static Logger LOGGER = LogUtils.getLogger();
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().is(DamageTypes.SONIC_BOOM)) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        }
    }
}
