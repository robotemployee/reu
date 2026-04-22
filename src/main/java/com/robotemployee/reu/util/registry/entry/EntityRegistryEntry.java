package com.robotemployee.reu.util.registry.entry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class EntityRegistryEntry<T extends Entity> {

    private final Supplier<EntityType<T>> entityReg;
    private final Supplier<Item> eggReg;

    public EntityRegistryEntry(Supplier<EntityType<T>> entityReg, Supplier<Item> eggReg) {
        this.entityReg = entityReg;
        this.eggReg = eggReg;
    }

    public Supplier<EntityType<T>> getRegistryObject() {
        return entityReg;
    }

    public EntityType<T> get() {
        return getRegistryObject().get();
    }

    public Item getEgg() {
        return getEggRegistry().get();
    }

    public Supplier<Item> getEggRegistry() {
        return eggReg;
    }
}
