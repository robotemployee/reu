package com.robotemployee.reu.mixin.base.loot.condition;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BonusLevelTableCondition.class)
public interface BonusLevelTableConditionAccessor {
    @Accessor("enchantment")
    Enchantment getEnchantment();

    @Accessor("values")
    float[] getValues();
}
