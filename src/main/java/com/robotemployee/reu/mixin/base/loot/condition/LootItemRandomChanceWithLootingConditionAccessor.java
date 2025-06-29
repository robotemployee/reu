package com.robotemployee.reu.mixin.base.loot.condition;

import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithLootingCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LootItemRandomChanceWithLootingCondition.class)
public interface LootItemRandomChanceWithLootingConditionAccessor {
    @Accessor("percent")
    float getProbability();

    @Accessor("lootingMultiplier")
    float getLootingMultiplier();
}
