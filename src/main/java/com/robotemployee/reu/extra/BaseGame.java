package com.robotemployee.reu.extra;

import com.github.alexthe666.alexsmobs.entity.EntityGorilla;
import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.ModAdvancements;
import com.robotemployee.reu.core.ModItems;
import com.supermartijn642.rechiseled.ChiselItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
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
