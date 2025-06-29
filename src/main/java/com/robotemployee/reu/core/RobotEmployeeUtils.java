package com.robotemployee.reu.core;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.block.BlankEggBlock;
import com.robotemployee.reu.compat.BaseGame;
import com.robotemployee.reu.compat.BornInChaosCompat;
import com.robotemployee.reu.compat.SculkHordeCompat;
import com.robotemployee.reu.block.InfusedEggBlock;
import com.robotemployee.reu.block.entity.InfusedEggBlockEntity;
import com.robotemployee.reu.item.InjectorItem;
import com.robotemployee.reu.item.ReconstructorItem;
import com.robotemployee.reu.item.SculkReconstructorItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
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
    // items
    // blocks
    // block items
    // block entities
    // entities

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> RECONSTRUCTOR = ITEMS.register("reconstructor", () -> new ReconstructorItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SCULK_RECONSTRUCTOR = ITEMS.register("sculk_reconstructor", () -> new SculkReconstructorItem(new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> INJECTOR = ITEMS.register("injector", () -> new InjectorItem(new Item.Properties()));

    // THE EGG LIFE
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final RegistryObject<Block> BLANK_EGG = BLOCKS.register("blank_egg", () -> new BlankEggBlock(BlockBehaviour.Properties.copy(Blocks.SNIFFER_EGG)));
    public static final RegistryObject<Item> BLANK_EGG_ITEM = ITEMS.register("blank_egg", () -> new BlockItem(BLANK_EGG.get(), new Item.Properties()));
    public static final RegistryObject<Block> INFUSED_EGG = BLOCKS.register("infused_egg", () -> new InfusedEggBlock(BlockBehaviour.Properties.copy(Blocks.SNIFFER_EGG)));

    public static final RegistryObject<Item> INFUSED_EGG_ITEM = ITEMS.register("infused_egg", () -> new BlockItem(INFUSED_EGG.get(), new Item.Properties()));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);

    public static final RegistryObject<BlockEntityType<InfusedEggBlockEntity>> INFUSED_EGG_BLOCK_ENTITY = BLOCK_ENTITIES.register("infused_egg_entity", () ->
            BlockEntityType.Builder.of(InfusedEggBlockEntity::new, INFUSED_EGG.get()).build(null));

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RegistryObject<CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("reu_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(RECONSTRUCTOR.get()))
                    .title(Component.translatable("creativetab.reu_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(RECONSTRUCTOR.get());
                        output.accept(SCULK_RECONSTRUCTOR.get());
                        output.accept(INJECTOR.get());
                        output.accept(BLANK_EGG_ITEM.get());
                        output.accept(INFUSED_EGG_ITEM.get());
                    })
                    .build()
    );

    public RobotEmployeeUtils(@NotNull FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);

        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        // same for sculk horde compat
        MinecraftForge.EVENT_BUS.register(SculkHordeCompat.class);
        MinecraftForge.EVENT_BUS.register(BornInChaosCompat.class);
        MinecraftForge.EVENT_BUS.register(BaseGame.class);

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
            ItemProperties.register(RECONSTRUCTOR.get(), new ResourceLocation(MODID, "is_auto_repairing"), (stack, level, entity, seed) -> {
                boolean isActivated = stack.getOrCreateTag().getInt(ReconstructorItem.LAST_AUTO_REPAIR_TAG) > 0;
                return isActivated ? 1.0f : 0.0f;
            });
            ItemProperties.register(SCULK_RECONSTRUCTOR.get(), new ResourceLocation(MODID, "is_auto_repairing"), (stack, level, entity, seed) -> {
                    boolean isActivated = stack.getOrCreateTag().getInt(ReconstructorItem.LAST_AUTO_REPAIR_TAG) > 0;
                    return isActivated ? 1.0f : 0.0f;
            });
        }
    }
}
