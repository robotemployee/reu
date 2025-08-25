package com.robotemployee.reu.mixin.base;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Invoker("getInputVector")
    static Vec3 getInputVector(Vec3 vec, float rotYaw, float speed) {
        throw new AssertionError();
    }
}
