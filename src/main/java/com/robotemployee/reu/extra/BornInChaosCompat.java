package com.robotemployee.reu.extra;

import com.mojang.logging.LogUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

public class BornInChaosCompat {

    // note that i don't know how to get born in chaos as a maven thingy on here.
    // as such, we will be using. dreadful ways to check entities

    public static final String NIGHTMARE_STALKER = "born_in_chaos_v1:nightmare_stalker";
    public static final String LIFESTEALER = "born_in_chaos_v1:lifestealer";
    public static final String LIFESTEALER_TRUE_FORM = "born_in_chaos_v1:lifestealer_true_form";

    public static final int RANGE = 64; // how close is "nearby"

    static Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (Objects.equals(event.getEntity().getEncodeId(), NIGHTMARE_STALKER)
                && event.getEntity().getHealth() < 25
                && !event.getEntity().hasEffect(MobEffects.POISON)) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, 1000000, 1));
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1000000, 0));
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {

        if (Objects.equals(event.getEntity().getEncodeId(), NIGHTMARE_STALKER) && event.getNewTarget() instanceof Player player) {
            player.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 0.8F, 0.5F);
        }
    }


    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent event) {
        if (event.getLevel().isClientSide()) return;

        final String id = event.getEntity().getEncodeId();
        if (Objects.equals(id, NIGHTMARE_STALKER)) {
            onNightmareStalkerSpawned(event);
        } else if (Objects.equals(id, LIFESTEALER)/* || Objects.equals(id, LIFESTEALER_TRUE_FORM)*/) {
            onLifestealerSpawned(event);
        }

    }

    public static void onNightmareStalkerSpawned(MobSpawnEvent event) {
        List<Player> players = event.getLevel().getEntitiesOfClass(Player.class, (new AABB(event.getEntity().blockPosition())).inflate(RANGE), EntitySelector.NO_SPECTATORS);
        int resultingLevel = 1;
        if (players.isEmpty()) return;
        for (Player player : players) {
            if (player.getMaxHealth() >= 30) {
                if (player.getMaxHealth() > 40) {
                    boolean nightmare = player.getMaxHealth() >= 60;
                    // hard difficulty
                    event.getEntity().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1000000, 0, true, !nightmare));
                    event.getEntity().addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1000000, 1, true, !nightmare));

                    // nightmare difficulty
                    if (nightmare) {
                        event.getEntity().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 800, 1, true, false));
                        event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1000000, 0, true, false));
                        resultingLevel = 4;
                        break;
                    }
                    resultingLevel = 3;
                    // no going down from here
                }
                // medium difficulty
                // . . .
            } else if (resultingLevel == 1) {
                // easy difficulty
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1000000, 0, true, true));
                event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1000000, 1, true, true));
                resultingLevel = 2;
            }
        }
    }

    public static void onLifestealerSpawned(MobSpawnEvent event) {
        List<Player> players = event.getLevel().getEntitiesOfClass(Player.class, (new AABB(event.getEntity().blockPosition())).inflate(RANGE), EntitySelector.NO_SPECTATORS);
        if (!players.stream().anyMatch(player -> player.getMaxHealth() > 40)) {
            event.getEntity().setHealth(30);
        }
    }

    // max amount of blocks away the phantom could spawn. ish
    static final int SPREAD = 2;

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!Objects.equals(event.getEntity().getEncodeId(), NIGHTMARE_STALKER)) return;
        if (!event.getEntity().hasEffect(MobEffects.MOVEMENT_SPEED)) return;
        Vec3 pos = event.getEntity().position();
        RandomSource random = event.getEntity().getRandom();
        for (int i = 0; i < 6; i++) {
            float x = random.nextFloat() * 2 * SPREAD - SPREAD;
            float y = random.nextFloat() * 2 * SPREAD - SPREAD + 1;
            float z = random.nextFloat() * 2 * SPREAD - SPREAD;

            pos.add(x, y, z);
            Phantom newborn = new Phantom(EntityType.PHANTOM, event.getEntity().level());
            newborn.setPos(pos);
            newborn.lookAt(event.getEntity(), 1, 1);
            event.getEntity().level().addFreshEntity(newborn);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().getGameTime() % 60 == 0
        && Objects.equals(event.getEntity().getEncodeId(), LIFESTEALER_TRUE_FORM)
        && event.getEntity().level().isDay() && event.getEntity().level().canSeeSky(event.getEntity().blockPosition())
        && !event.getEntity().isInWater()) {
            event.getEntity().setRemainingFireTicks(540);
        }
    }
}
