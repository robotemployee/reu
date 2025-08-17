package com.robotemployee.reu.core;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.banana.BananaRaidEvents;
import com.robotemployee.reu.core.registry.*;
import com.robotemployee.reu.extra.*;
import com.robotemployee.reu.extra.music_disc_obtainment.ClientDiscEvents;
import com.robotemployee.reu.extra.music_disc_obtainment.GenericDiscEvents;
import com.robotemployee.reu.item.ReconstructorItem;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(RobotEmployeeUtils.MODID)
public class RobotEmployeeUtils
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "reu";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public RobotEmployeeUtils(@NotNull FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        ModEntityDataSerializers.SERIALIZERS.register(modEventBus);
        ModMobEffects.EFFECTS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModFluidTypes.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        // the rest of these don't care as much about the ordering
        ModAdvancements.register();
        ModDamageTypes.idk();

        ModCreativeModeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        // same for eeeverything else that wants to hook into events
        MinecraftForge.EVENT_BUS.register(SculkHordeCompat.class);
        //MinecraftForge.EVENT_BUS.register(BornInChaosCompat.class); // born in chaos is being removed from the pack
        MinecraftForge.EVENT_BUS.register(BaseGame.class);
        MinecraftForge.EVENT_BUS.register(FriendsAndFoesCompat.class);
        MinecraftForge.EVENT_BUS.register(AlexsCavesCompat.class);
        MinecraftForge.EVENT_BUS.register(GenericDiscEvents.class);
        MinecraftForge.EVENT_BUS.register(CuriosCompat.class);
        MinecraftForge.EVENT_BUS.register(TummyAcheEvents.class);
        MinecraftForge.EVENT_BUS.register(BananaRaidEvents.class);

        // only for the client :)
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modEventBus.addListener(ClientModEvents::onClientSetup);
            MinecraftForge.EVENT_BUS.register(ClientModEvents.class);
            MinecraftForge.EVENT_BUS.register(ClientDiscEvents.class);
        });

        //MinecraftForge.EVENT_BUS.register(Datagen.class);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }


    private void commonSetup(final FMLCommonSetupEvent event) {
        ServerboundPreciseArrowPacket.register();
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
    public static class ClientModEvents
    {
        private static final ArrayList<Consumer<FMLClientSetupEvent>> CLIENT_SETUP_REQUESTS = new ArrayList<>();

        public static <T extends Entity> void addCustomRenderer(Supplier<EntityType<T>> entity, EntityRendererProvider<T> renderer) {
            CLIENT_SETUP_REQUESTS.add(fmlClientSetupEvent -> {
                EntityRenderers.register(entity.get(), renderer);
            });
        }

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

            for (Consumer<FMLClientSetupEvent> requests : CLIENT_SETUP_REQUESTS) {
                requests.accept(event);
            }
            //EntityRenderers.register(ModEntities.DEVIL.get(), DevilRenderer::new);
        }

        @SubscribeEvent
        public static void onToolTip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            Item item = stack.getItem();
            if (item == ModItems.ONE_DAY_BLINDING_STEW.get()) {
                Component translatable = Component.translatable("item.reu.one_day_blinding_stew.tooltip");
                List<Component> lines = Arrays.stream(translatable.getString().split("\n"))
                        .map(Component::literal)
                        .collect(Collectors.toList());

                event.getToolTip().addAll(1, lines);
                return;
            }

            if (item == ModItems.MUSIC_DISC_KOKOROTOLUNANOFUKAKAI.get()) {
                boolean showShotData = Screen.hasShiftDown();
                List<Component> tooltip = GenericDiscEvents.ArrowShotStatistics.getTooltip(stack, showShotData);
                if (tooltip != null) {
                    event.getToolTip().addAll(tooltip);
                } else {
                    event.getToolTip().add(Component.empty());
                    event.getToolTip().add(Component.literal("§8(No shot information.)"));
                }
                return;
            }

            if (item == ModItems.MIRACLE_PILL.get()) {
                event.getToolTip().add(Component.literal("§3Creative-only tummy ache removal."));
            }
        }
    }
}
