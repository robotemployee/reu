package com.robotemployee.reu.core;

import com.robotemployee.reu.core.registry_help.builder.ItemBuilder;
import com.robotemployee.reu.item.FryingPanItem;
import com.robotemployee.reu.item.ReconstructorItem;
import com.robotemployee.reu.item.SculkReconstructorItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    // note that this is not the only place items are registered.
    // for example, items are registered by BlockBuilder in order to automatically make block items
    // they are also registered by FluidBuilder if a bucket and bottle for the fluid are required
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RobotEmployeeUtils.MODID);
    //public static final RegistryObject<Item> RECONSTRUCTOR = ITEMS.register("reconstructor", () -> new ReconstructorItem(new Item.Properties().rarity(Rarity.RARE)));
    //public static final RegistryObject<Item> SCULK_RECONSTRUCTOR = ITEMS.register("sculk_reconstructor", () -> new SculkReconstructorItem(new Item.Properties().rarity(Rarity.EPIC)));
    //public static final RegistryObject<Item> INJECTOR = ITEMS.register("injector", () -> new InjectorItem(new Item.Properties()));



    public static final RegistryObject<Item> RECONSTRUCTOR = new ItemBuilder()
            .withName("reconstructor")
            .withSupplier(() -> new ReconstructorItem(
                    new Item.Properties()
                            .rarity(Rarity.RARE)
                    ))
            .build();

    public static final RegistryObject<Item> SCULK_RECONSTRUCTOR = new ItemBuilder()
            .withName("sculk_reconstructor")
            .withSupplier(() -> new SculkReconstructorItem(
                    new Item.Properties()
                            .rarity(Rarity.EPIC)
                    ))
            .build();


    public static final RegistryObject<Item> BLINDING_STEW = new ItemBuilder()
            .withName("one_day_blinding_stew")
            .withSupplier(() -> new Item(
                    new Item.Properties()
                            .food((new FoodProperties.Builder())
                                    .alwaysEat()
                                    .effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 24000), 1f)
                                    .nutrition(4)
                                    .build()
                            )))
            .build();

    public static final RegistryObject<Item> FRYING_PAN = new ItemBuilder()
            .withName("frying_pan")
            .withSupplier(() -> new FryingPanItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .defaultDurability(10)
            ))
            .build();
}
