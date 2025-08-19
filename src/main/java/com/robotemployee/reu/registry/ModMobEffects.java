package com.robotemployee.reu.registry;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.mobeffect.TummyAcheMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMobEffects {

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RobotEmployeeUtils.MODID);

    public static final RegistryObject<MobEffect> TUMMY_ACHE = EFFECTS.register("tummy_ache", () -> new TummyAcheMobEffect(MobEffectCategory.HARMFUL, 0xEEFFB0));
}
