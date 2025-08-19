package com.robotemployee.reu.registry.help.entry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistryEntry<T extends Entity> {

    private final RegistryObject<EntityType<T>> entityReg;
    private final RegistryObject<Item> eggReg;

    public EntityRegistryEntry(RegistryObject<EntityType<T>> entityReg, RegistryObject<Item> eggReg) {
        this.entityReg = entityReg;
        this.eggReg = eggReg;
    }

    public RegistryObject<EntityType<T>> getRegistryObject() {
        return entityReg;
    }

    public EntityType<T> get() {
        return getRegistryObject().get();
    }
}
