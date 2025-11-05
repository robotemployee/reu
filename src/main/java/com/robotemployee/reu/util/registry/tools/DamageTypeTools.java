package com.robotemployee.reu.util.registry.tools;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public class DamageTypeTools {

    public static ResourceKey<DamageType> register(ResourceLocation loc) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, loc);
    }

    public static void shhhhh() {}

    public static DamageSource getDamageSource(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key));
    }
}
