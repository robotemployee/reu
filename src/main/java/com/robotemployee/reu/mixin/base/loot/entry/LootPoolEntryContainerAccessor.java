package com.robotemployee.reu.mixin.base.loot.entry;

import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

@Mixin(LootPoolEntryContainer.class)
public interface LootPoolEntryContainerAccessor {
    @Accessor("conditions")
    LootItemCondition[] getConditions();

    @Accessor("compositeCondition")
    Predicate<LootContext> getCompositeCondition();
}
