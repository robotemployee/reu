package com.robotemployee.reu.core;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.compat.*;
import com.robotemployee.reu.core.registry_help.datagen.Datagen;
import com.robotemployee.reu.item.ReconstructorItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(RobotEmployeeUtils.MODID)
public class RobotEmployeeUtils
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "reu";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // sequence of registering:
    // sounds
    // blocks
    // block entities
    // fluid types
    // fluids
    // items
    // entities

    // THE EGG LIFE

    public RobotEmployeeUtils(@NotNull FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so items get registered
        ModSounds.SOUNDS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModFluidTypes.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        // same for sculk horde compat
        MinecraftForge.EVENT_BUS.register(SculkHordeCompat.class);
        MinecraftForge.EVENT_BUS.register(BornInChaosCompat.class);
        MinecraftForge.EVENT_BUS.register(BaseGame.class);
        MinecraftForge.EVENT_BUS.register(FriendsAndFoesCompat.class);
        MinecraftForge.EVENT_BUS.register(AlexsCavesCompat.class);

        //MinecraftForge.EVENT_BUS.register(Datagen.class);
        MinecraftForge.EVENT_BUS.register(ClientModEvents.class);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
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
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            ItemProperties.register(ModItems.RECONSTRUCTOR.get(), new ResourceLocation(MODID, "is_auto_repairing"), (stack, level, entity, seed) -> {
                boolean isActivated = stack.getOrCreateTag().getInt(ReconstructorItem.LAST_AUTO_REPAIR_TAG) > 0;
                return isActivated ? 1.0f : 0.0f;
            });
            ItemProperties.register(ModItems.SCULK_RECONSTRUCTOR.get(), new ResourceLocation(MODID, "is_auto_repairing"), (stack, level, entity, seed) -> {
                boolean isActivated = stack.getOrCreateTag().getInt(ReconstructorItem.LAST_AUTO_REPAIR_TAG) > 0;
                return isActivated ? 1.0f : 0.0f;
            });
        }

        @SubscribeEvent
        public static void onToolTip(ItemTooltipEvent event) {
            if (event.getItemStack().getItem() == ModItems.BLINDING_STEW.get()) {
                Component translatable = Component.translatable("item.reu.one_day_blinding_stew.tooltip");
                List<Component> lines = Arrays.stream(translatable.getString().split("\n"))
                        .map(Component::literal)
                        .collect(Collectors.toList());

                event.getToolTip().addAll(1, lines);
            }
        }
    }
}
