package com.robotemployee.reu.registry;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.registry.builder.EntityBuilder;
import com.robotemployee.reu.util.registry.entry.EntityRegistryEntry;
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
