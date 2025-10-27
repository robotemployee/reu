package com.robotemployee.reu.registry;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.foliant.entity.AsteirtoEntity;
import com.robotemployee.reu.foliant.entity.DevilEntity;
import com.robotemployee.reu.foliant.entity.GregEntity;
import com.robotemployee.reu.foliant.entity.ThrownSemisolidEntity;
import com.robotemployee.reu.foliant.render.AsteirtoRenderer;
import com.robotemployee.reu.foliant.render.DevilRenderer;
import com.robotemployee.reu.foliant.render.GregRenderer;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.registry.help.builder.EntityBuilder;
import com.robotemployee.reu.registry.help.entry.EntityRegistryEntry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

// least favorite annotation
@Mod.EventBusSubscriber(modid = RobotEmployeeUtils.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {

    static Logger LOGGER = LogUtils.getLogger();
    private static final ArrayList<Consumer<EntityAttributeCreationEvent>> attributeCreationRequests = new ArrayList<>();
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RobotEmployeeUtils.MODID);

    public static final int BANANA_EGG_COLOR = 0xE2EE1A;

    public static final EntityRegistryEntry<DevilEntity> DEVIL =
            new EntityBuilder<>(
                    () -> EntityType.Builder.of(DevilEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 0.5f))
                    .withName("devil")
                    .withAttributes(DevilEntity::createAttributes)
                    .customRenderer(DevilRenderer::new)
                    .eggColor(0x4CC9E1, BANANA_EGG_COLOR)
                    .build();

    public static final EntityRegistryEntry<GregEntity> GREG =
            new EntityBuilder<>(
                    () -> EntityType.Builder.of(GregEntity::new, MobCategory.MONSTER)
                            .sized(1, 0.65f))
                    .withName("greg")
                    .withAttributes(GregEntity::createAttributes)
                    .customRenderer(GregRenderer::new)
                    .eggColor(0xAC3232, BANANA_EGG_COLOR)
                    .build();

    public static final EntityRegistryEntry<AsteirtoEntity> ASTEIRTO =
            new EntityBuilder<>(
                    () -> EntityType.Builder.of(AsteirtoEntity::new, MobCategory.MONSTER)
                            .sized(2, 2.5f))
                    .withName("asteirto")
                    .withAttributes(AsteirtoEntity::createAttributes)
                    .customRenderer(AsteirtoRenderer::new)
                    .eggColor(0x9BE468, BANANA_EGG_COLOR)
                    .build();

    public static final EntityRegistryEntry<ThrownSemisolidEntity> THROWN_SEMISOLID =
            new EntityBuilder<>(
                    () -> EntityType.Builder.of(ThrownSemisolidEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f))
                    .withName("thrown_item")
                    .customRenderer(context -> new ThrownItemRenderer<>(context, 1, true))
                    .build();

    private static void addAttributeRequest(Consumer<EntityAttributeCreationEvent> consumer) {
        attributeCreationRequests.add(consumer);
    }

    public static void addAttributeRequest(RegistryObject<EntityType<? extends LivingEntity>> entityType, Supplier<AttributeSupplier> supplier) {
        addAttributeRequest(event -> {
            //LOGGER.info("Attempting to register attributes for " + entityType.get());
            event.put(entityType.get(), supplier.get());

            //LOGGER.info("Success!");
        });
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        //LOGGER.info(attributeCreationRequests.size() + " attributes were registered");
        for (Consumer<EntityAttributeCreationEvent> consumer : attributeCreationRequests) consumer.accept(event);
    }
}
