package com.robotemployee.reu.core;

import com.robotemployee.reu.core.registry_help.builder.FluidBuilder;
import com.robotemployee.reu.core.registry_help.entry.FluidRegistryEntry;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ModFluids {

    public static final HashMap<RegistryObject<Fluid>, RegistryObject<Item>> bucketLookupTable = new HashMap<>();
    public static final HashMap<RegistryObject<Fluid>, RegistryObject<Item>> bottleLookupTable = new HashMap<>();

    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, RobotEmployeeUtils.MODID);
    //public static final RegistryObject<FlowingFluid> FLOWING_MOB_FLUID = FLUIDS.register("flowing_mob_fluid", MobFluid.Flowing::new);
    //public static final RegistryObject<FlowingFluid> MOB_FLUID = FLUIDS.register("flowing_mob_fluid", MobFluid.Source::new);

    // Only run this AFTER items and stuff are registered
}
