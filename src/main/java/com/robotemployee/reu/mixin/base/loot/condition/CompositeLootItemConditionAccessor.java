package com.robotemployee.reu.mixin.base.loot.condition;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

@Mixin(CompositeLootItemCondition.class)
public interface CompositeLootItemConditionAccessor {
    @Accessor("terms")
    LootItemCondition[] getTerms();

    @Accessor("composedPredicate")
    Predicate<LootContext> getComposedPredicate();
}
