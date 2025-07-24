package com.robotemployee.reu.core.registry;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;

public class ModFluids {

    public static final HashMap<RegistryObject<Fluid>, RegistryObject<Item>> bucketLookupTable = new HashMap<>();
    public static final HashMap<RegistryObject<Fluid>, RegistryObject<Item>> bottleLookupTable = new HashMap<>();

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, RobotEmployeeUtils.MODID);
    //public static final RegistryObject<FlowingFluid> FLOWING_MOB_FLUID = FLUIDS.register("flowing_mob_fluid", MobFluid.Flowing::new);
    //public static final RegistryObject<FlowingFluid> MOB_FLUID = FLUIDS.register("flowing_mob_fluid", MobFluid.Source::new);

    // Only run this AFTER items and stuff are registered
}
