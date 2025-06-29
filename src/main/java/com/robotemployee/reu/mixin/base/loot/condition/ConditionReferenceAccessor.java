package com.robotemployee.reu.mixin.base.loot.condition;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.ConditionReference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ConditionReference.class)
public interface ConditionReferenceAccessor {
    @Accessor("name")
    ResourceLocation getResourceLocation();
}
