package com.robotemployee.reu.mixin.base.loot.condition;

import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InvertedLootItemCondition.class)
public interface InvertedLootItemConditionAccessor {
    @Accessor("term")
    LootItemCondition getTerm();
}
