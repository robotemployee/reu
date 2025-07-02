package com.robotemployee.reu.core;

import com.robotemployee.reu.item.InjectorItem;
import com.robotemployee.reu.item.ReconstructorItem;
import com.robotemployee.reu.item.SculkReconstructorItem;
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
    public static final RegistryObject<Item> RECONSTRUCTOR = ITEMS.register("reconstructor", () -> new ReconstructorItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SCULK_RECONSTRUCTOR = ITEMS.register("sculk_reconstructor", () -> new SculkReconstructorItem(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> INJECTOR = ITEMS.register("injector", () -> new InjectorItem(new Item.Properties()));




}
