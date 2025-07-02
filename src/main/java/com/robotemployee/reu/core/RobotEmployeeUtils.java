package com.robotemployee.reu.core;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.compat.BaseGame;
import com.robotemployee.reu.compat.BornInChaosCompat;
import com.robotemployee.reu.compat.SculkHordeCompat;
import com.robotemployee.reu.core.registry_help.datagen.DataGenerators;
import com.robotemployee.reu.core.registry_help.datagen.Datagen;
import com.robotemployee.reu.core.registry_help.generics.FilledBottleItem;
import com.robotemployee.reu.item.InjectorItem;
import com.robotemployee.reu.item.ReconstructorItem;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(RobotEmployeeUtils.MODID)
public class RobotEmployeeUtils
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "reu";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // sequence of registering:
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
        MinecraftForge.EVENT_BUS.register(DataGenerators.class);

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
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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

            ItemProperties.register(ModItems.INJECTOR.get(), new ResourceLocation(MODID, "is_injecting"), (stack, level, entity, seed) -> {
                InjectorItem.Mode mode = InjectorItem.getMode(stack);
                boolean isActivated = mode != InjectorItem.Mode.NONE && mode != InjectorItem.Mode.CLEAR;
                return isActivated ? 1.0f : 0.0f;
            });

            ItemProperties.register(ModItems.INJECTOR.get(), new ResourceLocation(MODID, "fill_amount"), (stack, level, entity, seed) -> {
                int fillAmount = InjectorItem.getModeDependentFillAmount(stack);
                int fillMax = InjectorItem.getFillAmount(stack, InjectorItem.FILL_MAX_PATH);
                return Mth.clamp((float)fillAmount / fillMax, 0, 1);
            });
        }

        @SubscribeEvent
        public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
            for (FilledBottleItem bottle : ModFluids.getBottles()) {
                int tint = ModFluidTypes.getTintFromType(ModFluids.MOB_FLUID.get().getFluidType());
                event.register((stack, index) -> {
                    return index == 1 ? tint : -1;
                }, bottle);
            }
        }
    }
}
