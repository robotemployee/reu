package com.robotemployee.reu.core.registry.help.entry;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.RegistryObject;

public class EntityRegistryEntry<T extends Entity> {

    private final RegistryObject<EntityType<T>> ENTITY;

    public EntityRegistryEntry(RegistryObject<EntityType<T>> newborn) {
        ENTITY = newborn;
    }

    public RegistryObject<EntityType<T>> getRegistryObject() {
        return ENTITY;
    }

    public EntityType<T> get() {
        return getRegistryObject().get();
    }
}
