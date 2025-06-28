package com.robotemployee.reu.compat;

import com.github.sculkhorde.common.entity.SculkCreeperEntity;
import com.github.sculkhorde.common.entity.SculkRavagerEntity;
import com.github.sculkhorde.common.entity.SculkSporeSpewerEntity;
import com.github.sculkhorde.common.entity.SculkWitchEntity;
import com.github.sculkhorde.core.ModSounds;
import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.slf4j.Logger;

import java.util.ArrayList;

public class SculkHordeCompat {

    // what to use for isVeryOutOfBounds
    // which is used for determining whether to report a death
    public static final int INFLATE_AMOUNT = 200;

    public static final Logger LOGGER = LogUtils.getLogger();

    public static boolean isOutOfBounds(BlockPos pos) {
        ArrayList<Vector2i> borders = Config.getSculkBorders();
        Vector2i a = borders.get(0); // negative
        Vector2i b = borders.get(1); // positive
        return pos.getX() < a.x || pos.getX() > b.y || pos.getZ() < a.y || pos.getZ() > b.y;
    }

    public static boolean isOutOfBounds(BlockPos pos, Level level) {
        return Config.areBordersEnabled && (isLevelOutOfBounds(level) || isOutOfBounds(pos));
    }

    public static boolean isOutOfBounds(Entity entity) {
        if (entity == null) return true;
        return isOutOfBounds(entity.blockPosition(), entity.level());
    }

    public static boolean isVeryOutOfBounds(@NotNull BlockPos pos) {
        ArrayList<Vector2i> borders = Config.getSculkBorders();
        Vector2i a = borders.get(0); // negative
        Vector2i b = borders.get(1); // positive
        return pos.getX() < a.x - INFLATE_AMOUNT || pos.getX() > b.x + INFLATE_AMOUNT || pos.getZ() < a.y - INFLATE_AMOUNT || pos.getZ() > b.y + INFLATE_AMOUNT;
    }

    public static boolean isVeryOutOfBounds(BlockPos pos, Level level) {
        return isLevelOutOfBounds(level) || isVeryOutOfBounds(pos);
    }

    public static boolean isVeryOutOfBounds(Entity entity) {
        if (entity == null) return true;
        return isVeryOutOfBounds(entity.blockPosition(), entity.level());
    }

    public static boolean isLevelOutOfBounds(Level level) {
        if (level == null) return true;
        return level.dimension() != Level.OVERWORLD;
    }

    public static final boolean remapNormalSculkHorde = false;
    public static final boolean remapDeobfSculkHorde = true;

    // within bounds, certain sculk mobs will get special effects
    // this is some of the shittiest code i've ever written but it works

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        //LOGGER.info("Sculk horde compat workywork living damage event");
        LivingEntity entity = event.getEntity();
        if (event.getSource().is(DamageTypes.ON_FIRE) && entity.getTags().contains("sculkhorde:sculk_entity")) {
            event.setAmount((int)Math.ceil(1.5 * event.getAmount()));
        }

        //LOGGER.info("still here");
        //if (isVeryOutOfBounds(entity)) return;
        getDamagedEffect(event);
    }

    /* summary:
        - the ravager gets a lot of resistances right after being shot, scales with the amount of damage it recieved
        this is to incentivize addressing them over time instead of bursting them down, since we have guns and they have 50hp
        - the creeper gets a speed boost when you attack them
        - the witch has a 50% chance to feign death
     */

    // idea - maybe make the creeper slowdown when you hit it and then speed up after

    // this is the amount of one-shot damage needed to instant kill things regardless of anything else
    // for example it will bypass the witch's dead ringer effect
    public static final int RAVAGER_INSTAKILL_AMOUNT = 80;
    public static final int WITCH_INSTAKILL_AMOUNT = 30;
    public static void getDamagedEffect(@NotNull LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        ArrayList<MobEffectInstance> result = new ArrayList<>();
        if (entity instanceof SculkRavagerEntity) {
            boolean doPityDamage = event.getAmount() < 1;
            // this is to handle instant kill
            if (entity.getHealth() > 25 && event.getAmount() > entity.getMaxHealth() && event.getAmount() < RAVAGER_INSTAKILL_AMOUNT) event.setAmount(entity.getHealth() - 5);
            // trying to burst these guys down will result in a very bad day
            if (entity.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                entity.playSound(SoundEvents.AXE_WAX_OFF, 2.0F, 0.7F);
                entity.playSound(ModSounds.BURROWED_BURST.get(), 1.0F, 2.0F);
                final RandomSource random = entity.level().getRandom();
                for (int i=0; i < 20; i++) {
                    double x = entity.getX() + 2 * random.nextFloat();
                    double y = entity.getY() + 2 * random.nextFloat();
                    double z = entity.getZ() + 2 * random.nextFloat();
                    entity.level().addParticle(ParticleTypes.SCULK_SOUL, x, y, z, 0D, 0D, 0D);
                }
            }
            // if we're getting a fair bit of damage, don't apply absorption
            // also don't apply it if we're on fire
            if (!doPityDamage && !entity.isOnFire()) {
                int absorptionLevel = Mth.clamp((int) event.getAmount() / 4 + 2, 1, 3);
                int absorptionTicks = absorptionLevel * 20 + 20;
                entity.removeEffect(MobEffects.ABSORPTION);
                result.add(new MobEffectInstance(MobEffects.ABSORPTION, absorptionTicks, absorptionLevel));
            }
            result.add(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1));
            result.add(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            result.add(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
            result.add(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
            if (doPityDamage) {
                event.setAmount(1);
            }
            // this is to handle dealing a large amount of damage as the first shot in general
            if (entity.getHealth() > entity.getMaxHealth() - 10 && !entity.isOnFire() && event.getAmount() > 8 && event.getAmount() < RAVAGER_INSTAKILL_AMOUNT) event.setAmount(Math.max(0, event.getAmount() - 8));
        } else if (entity instanceof SculkSporeSpewerEntity && !entity.isOnFire()) {
            // you want to burst these guys down
            result.add(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
        } else if (entity instanceof SculkCreeperEntity) {
            // the amount of speed stacks every time you attack
            // if it's attacked while it has speed, it gets absorption for a short duration
            int level;
            MobEffectInstance effect = entity.getEffect(MobEffects.MOVEMENT_SPEED);
            if (effect != null) {
                level = Math.min(3, effect.getAmplifier() + 1);
                if (entity.getEffect(MobEffects.ABSORPTION) == null && entity.getHealth() / entity.getMaxHealth() < 0.5 && !entity.isOnFire()) {
                    //result.add(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 4));
                    result.add(new MobEffectInstance(MobEffects.ABSORPTION, 30, 6));
                    //entity.removeEffect(MobEffects.MOVEMENT_SPEED);
                    //level = 0;
                }
                entity.playSound(SoundEvents.GUARDIAN_AMBIENT_LAND, 0.5F, Math.min(0.1F + (level * 0.5F), 2.0F));
            } else level = 1;
            result.add(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, level));
        } else if (entity instanceof SculkWitchEntity) {
            if (!entity.isOnFire()) result.add(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0));
            else if (event.getAmount() >= entity.getHealth()) {
                entity.level().explode(entity, entity.getX(), entity.getY(), entity.getZ(), 4, Level.ExplosionInteraction.NONE);
            }
        }

        for (MobEffectInstance effect : result) entity.addEffect(effect);
    }
}

/* witch dead ringer functionality
else if (entity instanceof SculkWitchEntity && !entity.isOnFire() && !isOutOfBounds(entity) &&
                entity.getHealth() > 6 && event.getAmount() < WITCH_INSTAKILL_AMOUNT && event.getAmount() >= entity.getHealth() && Math.random() > 0.25) {
            // note that the witch is literally invulnerable to non-instantkill stuff unless they're on fire
            // dead ringer witch lmfao
            if (!entity.hasEffect(MobEffects.INVISIBILITY)) {
                entity.playSound(SoundEvents.WITCH_DRINK);
                entity.playSound(SoundEvents.BELL_RESONATE, 2.0F, 0.5F);
                entity.playSound(SoundEvents.DOLPHIN_DEATH, 2.0F, 0.5F);
                RandomSource random = entity.level().getRandom();
                for (int i=0; i < 20; i++) {
                    double x = entity.getX() + 2 * random.nextFloat();
                    double y = entity.getY() + 2 * random.nextFloat();
                    double z = entity.getZ() + 2 * random.nextFloat();
                    entity.level().addParticle(ParticleTypes.SMOKE, x, y, z, 0D, 0D, 0D);
                }
            }
            entity.setHealth(1);
            // very short term invulnerability
            result.add(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4));
            result.add(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 4));
            result.add(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0));
            result.add(new MobEffectInstance(MobEffects.ABSORPTION, 60, 2));
            result.add(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            event.setAmount(0);
        }
 */

