package com.robotemployee.reu.core.registry;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public class ModDamageTypes {
    public static final ResourceKey<DamageType> ASBESTOSIS = register("asbestosis");

    public static ResourceKey<DamageType> register(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(RobotEmployeeUtils.MODID, id));
    }

    public static void idk() {}

    public static DamageSource getDamageSource(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key));
    }
}
