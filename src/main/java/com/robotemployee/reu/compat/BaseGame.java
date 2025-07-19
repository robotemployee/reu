package com.robotemployee.reu.compat;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.ModItems;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import javax.swing.text.html.parser.Entity;

public class BaseGame {

    static Logger LOGGER = LogUtils.getLogger();
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().is(DamageTypes.SONIC_BOOM)) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        //LOGGER.info("Living died");
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Chicken)) return;
        //LOGGER.info("Living is chicken");
        //LOGGER.info(event.getSource().getMsgId());
        if (!event.getSource().is(DamageTypes.LIGHTNING_BOLT)) return;
        //LOGGER.info("and it died to lightning");
        Level level = entity.level();
        if (level.isClientSide()) return;
        //LOGGER.info("and the level isn't clientside");

        ItemEntity newborn = new ItemEntity(
                level,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                new ItemStack(ModItems.MUSIC_DISC_FINLEY.get())
        );
        level.addFreshEntity(newborn);
    }
}
