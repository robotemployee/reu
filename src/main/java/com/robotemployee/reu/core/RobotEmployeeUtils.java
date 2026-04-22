package com.robotemployee.reu.core;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.util.registry.tools.*;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(RobotEmployeeUtils.MODID)
public class RobotEmployeeUtils
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "reu";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // do not try to do runClient - accessories' jar in jars not being deobfuscated and me thusly setting it to compileOnly fucks that up
    // this boolean is here to ensure that accessories things are never loaded during datagen
    public static final boolean developmentEnvironment = !FMLEnvironment.production;

    public RobotEmployeeUtils(IEventBus modEventBus, ModContainer modContainer)
    {

        /* here is your ideal load order:

        entity data serializers
        mob effects
        sound events
        entity types
        blocks
        block entities
        fluid types
        fluids
        items
        creative mode tabs

        and then, datagen...
        >> call a function in the classes that have your datagen stuff loaded, do whatever you have to to queue those requests, i just use a parameterless void function to load it into the jvm
        advancements
        damage types

         */

        // the rest of these don't care as much about the ordering

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);

        //MinecraftForge.EVENT_BUS.register(Datagen.class);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void addCreative(BuildCreativeModeTabContentsEvent event) {
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static class ClientModEvents {
        private static final ArrayList<Consumer<FMLClientSetupEvent>> CLIENT_SETUP_REQUESTS = new ArrayList<>();

        public static <T extends Entity> void addCustomRenderer(Supplier<EntityType<? extends T>> entity, EntityRendererProvider<T> renderer) {
            CLIENT_SETUP_REQUESTS.add(fmlClientSetupEvent -> {
                EntityRenderers.register(entity.get(), renderer);
            });
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            for (Consumer<FMLClientSetupEvent> requests : CLIENT_SETUP_REQUESTS) {
                requests.accept(event);
            }
            //EntityRenderers.register(ModEntities.DEVIL.get(), DevilRenderer::new);
        }
    }
}
