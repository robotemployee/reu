package com.robotemployee.reu.mobeffect;

import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.registry.ModDamageTypes;
import com.robotemployee.reu.core.registry.ModMobEffects;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class TummyAcheMobEffect extends MobEffect {

    static Logger LOGGER = LogUtils.getLogger();

    // At minimum severity. Effective chance increases with severity
    protected static final float CHANCE_TO_BLIND = 0.1f;
    protected static final float CHANCE_TO_WEAKEN = 0.02f;
    protected static final float CHANCE_TO_SLOW = 0.05f;
    protected static final long TICKS_TILL_SYMPTOMS = 2400; // two minutes //1200;
    protected static final long TICKS_TILL_MAX_SEVERITY = 24000; // 20 minutes
    protected static final long LIFETIME = TICKS_TILL_SYMPTOMS + TICKS_TILL_MAX_SEVERITY;
    protected static final float CHANCE_TO_AILMENT_ON_TICK = 0.2f; // increases with severity
    protected static final float CHANCE_TO_KILL = 0.001f; // multiplied by severity squared

    public static final int TICKS_ACCELERATED_ON_BREAD = (int)(TICKS_TILL_MAX_SEVERITY / 12);
    public static final int TICKS_DECELERATED_ON_STEW = -(int)(TICKS_TILL_MAX_SEVERITY / 3);
    public static final int MAX_DECELERATION = -(int)(TICKS_TILL_MAX_SEVERITY + (0.8 * TICKS_TILL_SYMPTOMS));
    private static final float SEVERITY_TO_HURT_VICTIM = 0.5f;

    public static final int COMMON_BREAD_BOONS_DURATION = 9600; // 8 minutes
    public static final int UNCOMMON_BREAD_BOONS_DURATION = 4800; // 4 minutes
    public static final int RARE_BREAD_BOONS_DURATION = 2400; // 2 minutes

    protected static final String ROOT_PATH = "Asbestosis";
    protected static final String TIMESTAMP_PATH = "Time";
    protected static final String APPLIED_PATH = "isActive";
    protected static final String TIME_ADVANCE_PATH = "BonusTime";

    protected static final int INTERVAL = 21;
    protected static final int INTERVALS_TO_AILMENT = 8;
    protected static final int OFFSET = 10;

    public TummyAcheMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    public static void bestowInterestingBreadBoons(LivingEntity victim, float severity) {
        RandomSource random = victim.level().getRandom();

        boolean doUncommonEffects = severity < 0.45;
        boolean doRareEffects = severity < 0.1;
        boolean reallyBadSeverity = severity > 0.8;

        if (!reallyBadSeverity && !doUncommonEffects && random.nextFloat() > Math.pow(severity, 2)) victim.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        if (!reallyBadSeverity && !doRareEffects && random.nextFloat() > 0.2f + severity) victim.removeEffect(MobEffects.DIG_SPEED);

        if (reallyBadSeverity) {
            victim.sendSystemMessage(Component.literal("§3It doesn't make you feel good anymore."));
            victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
            victim.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            victim.removeEffect(MobEffects.DIG_SPEED);
            victim.removeEffect(MobEffects.DAMAGE_BOOST);
            victim.removeEffect(MobEffects.REGENERATION);
        } else {
            victim.heal(6);
        }
        victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, COMMON_BREAD_BOONS_DURATION, 0));
        victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, COMMON_BREAD_BOONS_DURATION, 0));
        victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, doUncommonEffects ? COMMON_BREAD_BOONS_DURATION : UNCOMMON_BREAD_BOONS_DURATION, 0));
        if (!doUncommonEffects) return;
        if (!doRareEffects) return;
        victim.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, RARE_BREAD_BOONS_DURATION, 2));
        victim.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, RARE_BREAD_BOONS_DURATION, 0));
        victim.addEffect(new MobEffectInstance(AMEffectRegistry.KNOCKBACK_RESISTANCE.get(), RARE_BREAD_BOONS_DURATION, 0));
    }

    @Override
    public boolean isDurationEffectTick(int tick, int amp) {
        return true;
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }

    @Override
    public void applyEffectTick(@NotNull LivingEntity victim, int amp) {
        //LOGGER.info("This code is executing");
        Level level = victim.level();
        long tick = level.getGameTime();
        if (tick % INTERVAL != OFFSET) return;
        if (level.isClientSide()) return;

        CompoundTag info = getInfoFrom(victim);
        if (info.isEmpty()) activateInfo(victim);
        suffocateVictim(victim);
        // one minute -> you get a random ailment
        long time = getTime(victim, info, tick);
        //LOGGER.info("time applied for: " + time);
        if (time < TICKS_TILL_SYMPTOMS) return;
        //LOGGER.info("applying ailment");
        float severity = getSeverity(time, info);
        //LOGGER.info("Time:" + time + " Severity:" + severity);
        tickInflictAilment(victim, severity);
    }

    public static void suffocateVictim(LivingEntity victim) {
        int air = victim.getAirSupply();
        int max = victim.getMaxAirSupply();
        // tripled air consumption
        if (air < max) victim.setAirSupply(air - 2 * INTERVAL);
        float airFraction = air / (float)max;

        if (air <= 0 && !victim.hasEffect(MobEffects.BLINDNESS)) victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400));
        else if (airFraction < 0.7 && !victim.hasEffect(MobEffects.DARKNESS)) victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 400));
    }

    public static void tickInflictAilment(LivingEntity victim, float severity) {
        Level level = victim.level();
        long time = level.getGameTime();

        //LOGGER.info(String.valueOf(time % (8 * INTERVAL)));
        if (time % (INTERVALS_TO_AILMENT * INTERVAL) != OFFSET) return;
        victim.level().playSound(null, victim.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.6f, 0.8f);

        inflictAilment(victim, severity);
    }

    public static void inflictAilment(LivingEntity victim, float severity) {
        Level level = victim.level();
        //LOGGER.info("ailment.");
        RandomSource random = level.getRandom();

        if (random.nextFloat() + severity < CHANCE_TO_AILMENT_ON_TICK) return;

        if ((victim instanceof Player player && !player.isCreative()) && random.nextFloat() < CHANCE_TO_KILL + 0.05 * Math.pow(severity, 4)) {
            obliterateVictim(victim, severity);
            return;
        }

        ArrayList<MobEffect> effects = new ArrayList<>();

        if (random.nextFloat() < CHANCE_TO_BLIND + severity) {
            victim.removeEffect(MobEffects.DIG_SPEED);
            effects.add(severity > 0.7 ? MobEffects.BLINDNESS : MobEffects.DARKNESS);
        }
        if (random.nextFloat() < CHANCE_TO_WEAKEN + 0.3 * severity) victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 + (int)(20 * severity), 0));
        if (random.nextFloat() < CHANCE_TO_SLOW + severity) effects.add(MobEffects.MOVEMENT_SLOWDOWN);


        if (severity > 0.5) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10 + (int)Math.floor((10 * severity)), 3));
        }

        for (MobEffect effect : effects) victim.addEffect(
                new MobEffectInstance(
                        effect,
                        80 + (int)(140 * Math.pow(severity, 2)),
                        (int)(2 * Math.pow(severity, 1.5)),
                        false, false, true
                ));
        hurtVictim(victim, severity);
    }

    // an easy way to inflict a tummy ache ailment from outside of this with no context besides the victim
    public static void inflictAilment(LivingEntity victim) {
        CompoundTag info = getInfoFrom(victim);
        long time = getTime(victim, info, victim.level().getGameTime());
        inflictAilment(victim, TummyAcheMobEffect.getSeverity(time, info));
    }

    public static CompoundTag getInfoFrom(LivingEntity victim) {
        CompoundTag data = victim.getPersistentData();
        if (!data.contains(RobotEmployeeUtils.MODID)) return new CompoundTag();
        CompoundTag mod = data.getCompound(RobotEmployeeUtils.MODID);
        if (!mod.contains(ROOT_PATH)) return new CompoundTag();
        return mod.getCompound(ROOT_PATH);
    }

    // this one has optional severity modifiers applied

    public static float getSeverity(LivingEntity victim) {
        CompoundTag info = getInfoFrom(victim);
        return getSeverity(victim, info);
    }
    public static float getSeverity(LivingEntity victim, CompoundTag info) {
        return getSeverity(getTime(victim, info, victim.level().getGameTime()), info);
    }
    protected static float getSeverity(long timeWithAffliction, CompoundTag info) {
        return severityFormula(timeWithAffliction + info.getLong(TIME_ADVANCE_PATH));
    }

    protected static float severityFormula(long time) {
        return Math.max(0, Math.min(((time - TICKS_TILL_SYMPTOMS) / (float) TICKS_TILL_MAX_SEVERITY), 1));
    }

    // note that this also normalizes the time to only be as long as the maximum duration
    public static long getTime(LivingEntity victim, CompoundTag info, long currentTick) {
        if (info == null || info.isEmpty()) return 0;
        if (!info.contains(TIMESTAMP_PATH)) return 0;
        long timestamp = info.getLong(TIMESTAMP_PATH);
        // the timestamp is adjusted to have a maximum
        long rawTime = currentTick - timestamp;
        long adjustedTime = Math.min(rawTime, LIFETIME);
        long adjustedTimestamp = timestamp + rawTime - adjustedTime;
        info.putLong(TIMESTAMP_PATH, adjustedTimestamp);
        saveInfoTo(info, victim);
        long returned = Math.min(currentTick - adjustedTimestamp, LIFETIME);
        //LOGGER.info("Time: " + returned);
        return returned;
    }

    public static boolean getIsApplied(CompoundTag info) {
        if (info == null || info.isEmpty()) return false;
        if (!info.contains(APPLIED_PATH)) return false;
        return info.getBoolean(APPLIED_PATH);
    }

    // note that this is also intended to be used with negative numbers
    public static CompoundTag advanceTime(CompoundTag info, LivingEntity victim, long addedAmount) {
        //long currentTime = getTime(victim, info, victim.level().getGameTime());
        long start = info.getLong(TIME_ADVANCE_PATH);

        if (addedAmount < 0) start = Math.min(start, 0);
        else start = Math.max(start, 0);

        long advance = start + addedAmount;

        // it can at maximum put it to min severity
        // and the farthest it can bring it back is also like. there. and stuff
        info.putLong(TIME_ADVANCE_PATH, Math.max(advance, MAX_DECELERATION));
        saveInfoTo(info, victim);
        return info;
    }

    public static CompoundTag advanceTime(LivingEntity victim, long addedAmount) {
        return advanceTime(getInfoFrom(victim), victim, addedAmount);
    }

    public static void saveInfoTo(CompoundTag info, LivingEntity victim) {
        CompoundTag data = victim.getPersistentData();
        CompoundTag mod = data.getCompound(RobotEmployeeUtils.MODID);
        CompoundTag saved = mod.getCompound(ROOT_PATH).merge(info);
        mod.put(ROOT_PATH, saved);
        data.put(RobotEmployeeUtils.MODID, mod);
    }

    public static void activateInfo(LivingEntity victim) {
        CompoundTag persistent = victim.getPersistentData();
        CompoundTag mod = persistent.getCompound(RobotEmployeeUtils.MODID);
        CompoundTag newborn = new CompoundTag();
        newborn.putLong(TIMESTAMP_PATH, victim.level().getGameTime());
        newborn.putLong(TIME_ADVANCE_PATH, 0);
        newborn.putBoolean(APPLIED_PATH, true);
        mod.put(ROOT_PATH, newborn);
        persistent.put(RobotEmployeeUtils.MODID, mod);
        //LOGGER.info("Asbestosis activated. " + newborn);
    }

    public static void deactivateInfo(LivingEntity victim) {
        CompoundTag persistent = victim.getPersistentData();
        CompoundTag mod = persistent.getCompound(RobotEmployeeUtils.MODID);
        mod.remove(ROOT_PATH);
        persistent.put(RobotEmployeeUtils.MODID, mod);
    }

    public static MobEffectInstance defaultInstance() {
        return new MobEffectInstance(ModMobEffects.TUMMY_ACHE.get(), MobEffectInstance.INFINITE_DURATION, 0, false, false, false);
    }

    public static void hurtVictim(LivingEntity victim, float severity) {
        RandomSource random = victim.level().getRandom();
        float damage = (0.05f + (0.4f * severity)) * victim.getMaxHealth() +  (victim.hasEffect(MobEffects.BLINDNESS) ? 6 : 0);
        if (damage < 2) return; //|| random.nextFloat() < severity - 0.15
        if (victim.hasEffect(MobEffects.REGENERATION) && damage > 4) victim.removeEffect(MobEffects.REGENERATION);
        if (severity < SEVERITY_TO_HURT_VICTIM) return;
        victim.hurt(ModDamageTypes.getDamageSource(victim.level(), ModDamageTypes.ASBESTOSIS), damage);
    }

    public static void obliterateVictim(LivingEntity victim, float severity) {
        victim.hurt(ModDamageTypes.getDamageSource(victim.level(), ModDamageTypes.ASBESTOSIS), Math.max(35, victim.getMaxHealth() - 1 - severity));
        victim.sendSystemMessage(Component.literal("§3You lost the lottery."));
    }

    public static void inflictUpon(LivingEntity victim) {
        if (victim.hasEffect(ModMobEffects.TUMMY_ACHE.get())) return;
        activateInfo(victim);
        victim.addEffect(TummyAcheMobEffect.defaultInstance());
        victim.sendSystemMessage(Component.literal("§3You feel odd. ☹"));
    }

    public static void cure(LivingEntity victim) {
        victim.sendSystemMessage(Component.literal("§3Your tummy ache is gone! ☺"));
        victim.removeEffect(ModMobEffects.TUMMY_ACHE.get());
        TummyAcheMobEffect.deactivateInfo(victim);
    }
}
