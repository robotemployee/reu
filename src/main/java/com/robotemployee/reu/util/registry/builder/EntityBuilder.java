package com.robotemployee.reu.util.registry.builder;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.util.registry.entry.ItemRegistryEntry;
import com.robotemployee.reu.util.registry.tools.EntityTools;
import com.robotemployee.reu.util.datagen.DatagenInstance;
import com.robotemployee.reu.util.registry.entry.EntityRegistryEntry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class EntityBuilder<T extends Entity> {

    public static final Logger LOGGER = LogUtils.getLogger();

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

        public BiConsumer<Supplier<EntityType<? extends Entity>>, Supplier<EntityRendererProvider<? extends Entity>>> rendererReciever;
        public Manager(DatagenInstance datagenInstance, DeferredRegister<EntityType<?>> register, ItemBuilder.Manager itemManager) {
            this.datagenInstance = datagenInstance;
            this.register = register;
            this.itemManager = itemManager;
        }

        public <T extends Entity> EntityBuilder<T> createBuilder() {
            EntityBuilder<T> newborn = new EntityBuilder<>(datagenInstance, register, itemManager);
            if (rendererReciever != null) {
                newborn.withRendererReciever(rendererReciever);
            }
            return newborn;
        }

        // this is required if you are using a custom renderer
        // attach this to something that will register the renderer to the entity, it's a ClientModEvent. make a queue out of an ArrayList or something
        // did not add that functionality directly here because events are static
        public <T extends Entity> Manager withRendererReciever(BiConsumer<Supplier<EntityType<? extends T>>, Supplier<EntityRendererProvider<T>>> rendererReciever) {
            this.rendererReciever = (BiConsumer<Supplier<EntityType<? extends Entity>>, Supplier<EntityRendererProvider<? extends Entity>>>)(Object) rendererReciever;
            return this;
        }
    }

    private final DatagenInstance datagenInstance;
    private final DeferredRegister<EntityType<?>> register;
    private final ItemBuilder.Manager itemManager;
    private BiConsumer<Supplier<EntityType<? extends Entity>>, Supplier<EntityRendererProvider<? extends Entity>>> rendererReciever;

    private EntityBuilder(DatagenInstance datagenInstance, DeferredRegister<EntityType<?>> register, ItemBuilder.Manager itemManager) {
        this.datagenInstance = datagenInstance;
        this.register = register;
        this.itemManager = itemManager;
    }

    private EntityBuilder<T> withRendererReciever(BiConsumer<Supplier<EntityType<? extends Entity>>, Supplier<EntityRendererProvider<? extends Entity>>> rendererReciever) {
        // sjut up
        this.rendererReciever = rendererReciever;
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

    public EntityBuilder<T> customRenderer(Supplier<Supplier<EntityRendererProvider<T>>> rendererProviderSupplier) {
        if (FMLEnvironment.dist.isClient()) {
            this.rendererProvider = rendererProviderSupplier.get().get();
        }

        return this;
    }

    public EntityRegistryEntry<T> build() {
        checkForInsufficientParams();
        Supplier<EntityType<T>> entityTypeSupplier = () -> entityTypeBuilderSupplier.get().build(name);
        Supplier<EntityType<T>> newborn = register.register(name, entityTypeSupplier);

        if (attributesBuilderSupplier != null) {
            EntityTools.addAttributeRequest((Supplier<EntityType<? extends LivingEntity>>)(Object) newborn, () -> attributesBuilderSupplier.get().build());
            //(RegistryObject<EntityType<? extends LivingEntity>>)(Object)newborn, () -> attributesBuilderSupplier.get().build())
        }

        ItemRegistryEntry egg;
        if (hasEgg) {
            egg = itemManager.createBuilder()
                    .withName(name + "_spawn_egg")
                    .withSupplier(() ->
                        new DeferredSpawnEggItem(() -> (EntityType<? extends Mob>) newborn.get(), eggColorA, eggColorB, new Item.Properties())
                    )
                    .customDatagen(DatagenInstance::spawnEgg)
                    .build();
        } else egg = null;

        //EntityRegistryEntry<T> entry = new EntityRegistryEntry<>(newborn);

        if (FMLEnvironment.dist.isClient()) {
            if (rendererProvider != null) rendererReciever.accept(newborn::get, () -> rendererProvider);
        }

        return new EntityRegistryEntry<>(newborn, egg);
    }

    public void checkForInsufficientParams() {
        if (name == null) throw new IllegalStateException("Must assign a name");
        if (entityTypeBuilderSupplier == null) throw new IllegalStateException("Must assign an entity type");
    }
}
