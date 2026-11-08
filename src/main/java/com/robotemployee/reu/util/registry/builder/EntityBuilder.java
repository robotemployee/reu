package com.robotemployee.reu.util.registry.builder;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.util.registry.entry.ItemRegistryEntry;
import com.robotemployee.reu.util.registry.tools.EntityTools;
import com.robotemployee.reu.util.datagen.DatagenInstance;
import com.robotemployee.reu.util.registry.entry.EntityRegistryEntry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EntityBuilder<T extends Entity> {

    public static final Logger LOGGER = LogUtils.getLogger();

    private String name;
    private Supplier<EntityType.Builder<T>> entityTypeBuilderSupplier;
    private Supplier<AttributeSupplier.Builder> attributesBuilderSupplier;

    boolean hasEgg = false;
    private int eggColorA;
    private int eggColorB;

    // im fucking around with this one honestly shsush shhhh shhhhhhhhhhhh if it works it works
    public static class Manager implements IManager {
        private final DeferredRegister<EntityType<?>> register;
        private final ItemBuilder.Manager itemManager;

        @OnlyIn(Dist.CLIENT)
        protected RendererCollector rendererCollector;

        public Manager(ModInformation modInformation, ItemBuilder.Manager itemManager) {
            this.register = DeferredRegister.create(Registries.ENTITY_TYPE, modInformation.getNamespace());
            this.itemManager = itemManager;
            if (FMLEnvironment.dist.isClient()) rendererCollector = new RendererCollector(modInformation.getModEventBus());
        }

        public <T extends Entity> EntityBuilder<T> createBuilder() {
            EntityBuilder<T> newborn = new EntityBuilder<>(this, itemManager);
            return newborn;
        }

        public DeferredRegister<EntityType<?>> getRegister() {
            return register;
        }

        @Override
        public void register(IEventBus modEventBus) {
            register.register(modEventBus);
        }

        @OnlyIn(Dist.CLIENT)
        public static class RendererCollector {
            public RendererCollector(IEventBus modEventBus) {
                modEventBus.addListener(this::onClientSetup);
            }

            private final ArrayList<Consumer<FMLClientSetupEvent>> CLIENT_SETUP_REQUESTS = new ArrayList<>();

            public <T extends Entity> void addCustomRenderer(Supplier<EntityType<T>> entity, EntityRendererProvider<T> renderer) {
                LOGGER.info("Queueing renderer " + renderer);
                CLIENT_SETUP_REQUESTS.add(fmlClientSetupEvent -> {
                    LOGGER.info("Registering renderer " + renderer + " for entity type " + entity.get().getDescriptionId());
                    EntityRenderers.register(entity.get(), renderer);
                });
            }

            @SubscribeEvent
            public void onClientSetup(FMLClientSetupEvent event) {
                for (Consumer<FMLClientSetupEvent> requests : CLIENT_SETUP_REQUESTS) {
                    requests.accept(event);
                }
            }
        }
    }

    private final Manager MANAGER;
    private final ItemBuilder.Manager itemManager;

    private EntityBuilder(EntityBuilder.Manager manager, ItemBuilder.Manager itemManager) {
        this.MANAGER = manager;
        this.itemManager = itemManager;
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
        Supplier<EntityType<T>> newborn = MANAGER.getRegister().register(name, entityTypeSupplier);

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
            if (rendererProvider != null) MANAGER.rendererCollector.addCustomRenderer(newborn, rendererProvider);
        }

        return new EntityRegistryEntry<>(newborn, egg);
    }

    public void checkForInsufficientParams() {
        if (name == null) throw new IllegalStateException("Must assign a name");
        if (entityTypeBuilderSupplier == null) throw new IllegalStateException("Must assign an entity type");
    }
}
