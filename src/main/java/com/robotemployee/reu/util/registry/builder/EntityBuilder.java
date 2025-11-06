package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.registry.tools.EntityTools;
import com.robotemployee.reu.util.datagen.DatagenInstance;
import com.robotemployee.reu.util.registry.entry.EntityRegistryEntry;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class EntityBuilder<T extends Entity> {

    private String name;
    private Supplier<EntityType.Builder<T>> entityTypeBuilderSupplier;
    private Supplier<AttributeSupplier.Builder> attributesBuilderSupplier;

    boolean hasEgg = false;
    private int eggColorA;
    private int eggColorB;

    public static class Manager {
        public final DatagenInstance datagenInstance;
        public final DeferredRegister<EntityType<?>> register;
        public final ItemBuilder.Manager itemManager;

        public BiConsumer<Supplier<EntityType<? extends Entity>>, EntityRendererProvider<? extends Entity>> rendererReciever;
        public Manager(DatagenInstance datagenInstance, DeferredRegister<EntityType<?>> register, ItemBuilder.Manager itemManager) {
            this.datagenInstance = datagenInstance;
            this.register = register;
            this.itemManager = itemManager;
        }

        public <T extends Entity> EntityBuilder<T> createBuilder() {
            EntityBuilder<T> newborn = new EntityBuilder<>(datagenInstance, register, itemManager);
            if (rendererReciever != null) newborn.withRendererReciever(rendererReciever);
            return newborn;
        }

        // this is required if you are using a custom renderer
        // attach this to something that will register the renderer to the entity, it's a ClientModEvent. make a queue out of an ArrayList or something
        // did not add that functionality directly here because events are static
        public <T extends Entity> Manager withRendererReciever(BiConsumer<Supplier<EntityType<? extends T>>, EntityRendererProvider<? extends T>> rendererReciever) {
            this.rendererReciever = (BiConsumer<Supplier<EntityType<? extends Entity>>, EntityRendererProvider<? extends Entity>>) rendererReciever;
            return this;
        }
    }

    private final DatagenInstance datagenInstance;
    private final DeferredRegister<EntityType<?>> register;
    private final ItemBuilder.Manager itemManager;
    private BiConsumer<Supplier<EntityType<T>>, EntityRendererProvider<T>> rendererReciever;

    private EntityBuilder(DatagenInstance datagenInstance, DeferredRegister<EntityType<?>> register, ItemBuilder.Manager itemManager) {
        this.datagenInstance = datagenInstance;
        this.register = register;
        this.itemManager = itemManager;
    }

    private EntityBuilder<T> withRendererReciever(BiConsumer<Supplier<EntityType<? extends Entity>>, EntityRendererProvider<? extends Entity>> rendererReciever) {
        // sjut up
        this.rendererReciever = (BiConsumer<Supplier<EntityType<T>>, EntityRendererProvider<T>>) (Object) rendererReciever;
        return this;
    }

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

    public EntityBuilder<T> eggColor(int eggColorA, int eggColorB) {
        hasEgg = true;
        this.eggColorA = eggColorA;
        this.eggColorB = eggColorB;
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
            EntityTools.addAttributeRequest((RegistryObject<EntityType<? extends LivingEntity>>)(Object) newborn, () -> attributesBuilderSupplier.get().build());
            //(RegistryObject<EntityType<? extends LivingEntity>>)(Object)newborn, () -> attributesBuilderSupplier.get().build())
        }

        RegistryObject<Item> egg = null;
        if (hasEgg) {
            egg = itemManager.createBuilder()
                    .withName(name + "_spawn_egg")
                    .withSupplier(() ->
                        new ForgeSpawnEggItem(() -> (EntityType<? extends Mob>) newborn.get(), eggColorA, eggColorB, new Item.Properties())
                    )
                    .customDatagen((datagen, itemRegistryObject) -> datagen.spawnEgg())
                    .build();
        }

        //EntityRegistryEntry<T> entry = new EntityRegistryEntry<>(newborn);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (rendererProvider != null) rendererReciever.accept(newborn::get, rendererProvider);
        });

        return new EntityRegistryEntry<>(newborn, egg);
    }

    public void checkForInsufficientParams() {
        if (name == null) throw new IllegalStateException("Must assign a name");
        if (entityTypeBuilderSupplier == null) throw new IllegalStateException("Must assign an entity type");
    }
}
