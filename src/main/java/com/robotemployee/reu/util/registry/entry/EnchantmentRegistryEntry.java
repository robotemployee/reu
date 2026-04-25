package com.robotemployee.reu.util.registry.entry;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public record EnchantmentRegistryEntry(ResourceKey<Enchantment> key) {
    public Holder<Enchantment> holder(HolderLookup.Provider lookupProvider) {
        return lookupProvider.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
}
