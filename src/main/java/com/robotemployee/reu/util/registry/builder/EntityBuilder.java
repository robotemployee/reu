package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.registry.ModEntities;
import com.robotemployee.reu.util.datagen.DatagenInstance;
import com.robotemployee.reu.util.registry.entry.EntityRegistryEntry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class EntityBuilder<T extends Entity> {

    private String name;
    private Supplier<EntityType.Builder<T>> entityTypeBuilderSupplier;
    private Supplier<AttributeSupplier.Builder> attributesBuilderSupplier;

    boolean hasEgg = false;
    private int eggColorA;
    private int eggColorB;
    private ItemBuilder itemBuilder;

    private final DatagenInstance datagenInstance;
    private final DeferredRegister<EntityType<?>> register;

    private EntityBuilder(DatagenInstance datagenInstance, DeferredRegister<EntityType<?>> register) {
        this.datagenInstance = datagenInstance;
        this.register = register;
    }

    /*
    public static <T extends Entity> EntityBuilder<T> of(Supplier<EntityType.Builder<T>> builder) {
        EntityBuilder<T> newborn = new EntityBuilder<T>();
        newborn.entityTypeBuilderSupplier = builder;
        return newborn;
    }
     */

    public EntityBuilder<T> withTypeSupplier(Supplier<EntityType.Builder<T>> entityTypeBuilderSupplier) {
        this.entityTypeBuilderSupplier = entityTypeBuilderSupplier;
        return this;
    }

    public EntityBuilder<T> withName(String name) {
        this.name = name;
        return this;
    }

    public EntityBuilder<T> withAttributes(Supplier<AttributeSupplier.Builder> attributesBuilderSupplier) {
        this.attributesBuilderSupplier = attributesBuilderSupplier;
        return this;
    }

    public EntityBuilder<T> eggColor(ItemBuilder.Manager manager, int eggColorA, int eggColorB) {
        hasEgg = true;
        this.eggColorA = eggColorA;
        this.eggColorB = eggColorB;
        this.itemBuilder = manager.createBuilder();
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    private EntityRendererProvider<T> rendererProvider;
    // AAAAAHHHHH I LOVE BEING AUTISTIC THIS SHIT IS FUCKING GREAT AAAAHHH

    @OnlyIn(Dist.CLIENT)
    public EntityBuilder<T> customRenderer(EntityRendererProvider<T> rendererProvider) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            this.rendererProvider = rendererProvider;
        });
        return this;
    }

    public EntityRegistryEntry<T> build() {
        checkForInsufficientParams();
        Supplier<EntityType<T>> entityTypeSupplier = () -> entityTypeBuilderSupplier.get().build(name);
        RegistryObject<EntityType<T>> newborn = register.register(name, entityTypeSupplier);

        if (attributesBuilderSupplier != null) {
            ModEntities.addAttributeRequest((RegistryObject<EntityType<? extends LivingEntity>>)(Object) newborn, () -> attributesBuilderSupplier.get().build());
            //(RegistryObject<EntityType<? extends LivingEntity>>)(Object)newborn, () -> attributesBuilderSupplier.get().build())
        }

        RegistryObject<Item> egg = null;
        if (hasEgg) {
            egg = itemBuilder
                    .withName(name + "_spawn_egg")
                    .withSupplier(() ->
                        new ForgeSpawnEggItem(() -> (EntityType<? extends Mob>) newborn.get(), eggColorA, eggColorB, new Item.Properties())
                    )
                    .customDatagen((datagen, itemRegistryObject) -> datagen.spawnEgg())
                    .build();
        }

        //EntityRegistryEntry<T> entry = new EntityRegistryEntry<>(newborn);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (rendererProvider != null) RobotEmployeeUtils.ClientModEvents.addCustomRenderer(newborn, rendererProvider);
        });

        return new EntityRegistryEntry<>(newborn, egg);
    }

    public void checkForInsufficientParams() {
        if (name == null) throw new IllegalStateException("Must assign a name");
        if (entityTypeBuilderSupplier == null) throw new IllegalStateException("Must assign an entity type");
    }
}
