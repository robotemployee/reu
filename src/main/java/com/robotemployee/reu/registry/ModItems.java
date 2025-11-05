package com.robotemployee.reu.registry;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.registry.builder.ItemBuilder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ModItems {

    // note that this is not the only place items are registered.
    // for example, items are registered by BlockBuilder in order to automatically make block items
    // they are also registered by FluidBuilder if a bucket and bottle for the fluid are required
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RobotEmployeeUtils.MODID);
    //public static final RegistryObject<Item> RECONSTRUCTOR = ITEMS.register("reconstructor", () -> new ReconstructorItem(new Item.Properties().rarity(Rarity.RARE)));
    //public static final RegistryObject<Item> SCULK_RECONSTRUCTOR = ITEMS.register("sculk_reconstructor", () -> new SculkReconstructorItem(new Item.Properties().rarity(Rarity.EPIC)));
    //public static final RegistryObject<Item> INJECTOR = ITEMS.register("injector", () -> new InjectorItem(new Item.Properties()));


    static Logger LOGGER = LogUtils.getLogger();

    public static RegistryObject<Item> createSimpleItem(String id) {
        return new ItemBuilder()
                .withName(id)
                .withSupplier(() -> new Item(
                        new Item.Properties()
                ))
                .build();
    }

    public static RegistryObject<Item> createSimpleFoodItem(String id, FoodProperties properties) {
        return new ItemBuilder()
                .withName(id)
                .withSupplier(() -> new Item(
                        new Item.Properties().food(properties)
                ))
                .build();
    }

    // note that the resulting item id will have "music_disc_" appended to the start of the itemId input
    public static RegistryObject<Item> createDiscItem(String itemId, Supplier<SoundEvent> sound, int ticks) {
        String finalItemId = "music_disc_" + itemId;

        return new ItemBuilder()
                .withName(finalItemId)
                .withSupplier(() -> {
                        assert sound.get() != null;
                        LOGGER.info(String.format("Registering new music disc... id=%s sound=%s duration=%s", finalItemId, sound.get().getLocation(), ticks));
                        return new RecordItem(
                                1,
                                sound,
                                new Item.Properties()
                                        .rarity(Rarity.RARE)
                                        .stacksTo(1)
                                        .fireResistant()
                                ,
                                ticks
                        );
                })
                .addTag(() -> ItemTags.MUSIC_DISCS)
                .build();

    }
}
