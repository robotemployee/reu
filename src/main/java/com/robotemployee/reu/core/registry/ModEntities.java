package com.robotemployee.reu.core.registry;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.banana.entity.DevilEntity;
import com.robotemployee.reu.banana.render.DevilRenderer;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.core.registry.help.builder.EntityBuilder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.function.Consumer;

// least favorite annotation
@Mod.EventBusSubscriber(modid = RobotEmployeeUtils.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {

    static Logger LOGGER = LogUtils.getLogger();
    private static final ArrayList<Consumer<EntityAttributeCreationEvent>> attributeCreationRequests = new ArrayList<>();
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RobotEmployeeUtils.MODID);

    public static final RegistryObject<EntityType<DevilEntity>> DEVIL =
            new EntityBuilder<>(
                    () -> EntityType.Builder.of(DevilEntity::new, MobCategory.MONSTER)
                            .sized(0.5f, 0.5f))
                    .withName("devil")
                    .withAttributes(DevilEntity::createAttributes)
                    .customRenderer(DevilRenderer::new)
                    .build();

    public static void addAttributeRequest(Consumer<EntityAttributeCreationEvent> consumer) {
        attributeCreationRequests.add(consumer);
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        LOGGER.info(attributeCreationRequests.size() + " attributes were registered");
        for (Consumer<EntityAttributeCreationEvent> consumer : attributeCreationRequests) consumer.accept(event);
    }
}
