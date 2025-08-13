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

    // directly passed to the level.playSoundLocal() call that note blocks do
    public static final int NOTE_BLOCK_VOLUME = 10;
    // the distance where a note could be heard by something like a Listener block
    public static final int MIDI_NOTE_DISTANCE = 196;
    // the minimum fraction of the sound you could hear after it is reduced only by distance
    // does not affect reductions from the volume setting in the instrument or MIDI velocity; only distance
    // not quite linear
    public static final float MIDI_MIN_AUDIBLE_AMOUNT = 0.5f;
    // the distance where a client could hear things???? unsure
    public static final int MIDI_MAX_AUDIBLE_DISTANCE = 2 * MIDI_NOTE_DISTANCE;
    // offset for the distance attenuation equation. use this to make it fade out sooner or later
    // a positive number will make it happen later, a negative number will make it happen sooner
    public static final float MIDI_DISTANCE_EQUATION_OFFSET = 0f;
    // note that the default is 2.5, look at where this is used and at
    public static final float MIDI_DISTANCE_SLOPE_POWER = 2.05f;
    // increase to make sounds quieter from distance
    // decrease to make sounds resist distance-based quietness more
    // set to 1 to leave things be
    // linear and must be above 0
    public static final float MIDI_DISTANCE_QUIETNESS_FACTOR = 1f;
    // this reduces how much sounds are panned from ear to ear, since it feels a little disorienting when you're trying to listen to a song imo.
    // increase this variable to reduce pan
    // decrease to increase pan amount
    // must be above 0
    public static final float MIDI_PAN_REDUCTION = 1.75f;

    // give the player some funny effects when hit by a sonic boom
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
