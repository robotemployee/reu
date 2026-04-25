package com.robotemployee.reu.util.registry.entry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public record ItemRegistryEntry(DeferredHolder<Item, Item> holder) implements Supplier<Item> {

    @Override
    public Item get() {
        return holder().value();
    }
}
