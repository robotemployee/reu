package com.robotemployee.reu.core;

import com.robotemployee.reu.core.registry_help.builder.FluidBuilder;
import com.robotemployee.reu.core.registry_help.entry.FluidRegistryEntry;
import com.robotemployee.reu.core.registry_help.generics.FilledBottleItem;
import com.robotemployee.reu.item.MobFluidFilledBottleItem;
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


    public static final FluidRegistryEntry MOB_FLUID = new FluidBuilder()
            .withName("mob_fluid")
            .flowing(() -> ModFluids.MOB_FLUID_PROPERTIES)
            .withBottleSupplier(() -> new MobFluidFilledBottleItem(ModFluids.MOB_FLUID.getSourceRegistry(), new Item.Properties().stacksTo(16)))
            .build();
    public static final ForgeFlowingFluid.Properties MOB_FLUID_PROPERTIES = new ForgeFlowingFluid.Properties(ModFluidTypes.MOB_FLUID, MOB_FLUID::get, MOB_FLUID::getFlow);



    @Nullable
    public static Item getBucketFromFluid(Fluid fluid) { return findAndGet(fluid, bucketLookupTable); }

    @Nullable
    public static Item getBottleFromFluid(Fluid fluid) { return findAndGet(fluid, bottleLookupTable); }


    // Only run this AFTER items and stuff are registered
    public static ArrayList<Item> getBuckets() {
        ArrayList<Item> result = new ArrayList<>();
        for (RegistryObject<Item> item : bucketLookupTable.values()) {
            if (!item.isPresent()) throw new IllegalStateException("Called ModFluids.getBuckets() when one of the buckets is not registered");
            result.add(item.get());
        }
        return result;
    }

    // Only run this AFTER items and stuff are registered
    public static ArrayList<FilledBottleItem> getBottles() {
        ArrayList<FilledBottleItem> result = new ArrayList<>();
        for (RegistryObject<Item> item : bottleLookupTable.values()) {
            if (!item.isPresent()) throw new IllegalStateException("Called ModFluids.getBottles() when one of the bottles is not registered");
            result.add((FilledBottleItem) item.get());
        }
        return result;
    }

    @Nullable
    private static Item findAndGet(Fluid fluid, HashMap<RegistryObject<Fluid>, RegistryObject<Item>> map) {
        for (Map.Entry<RegistryObject<Fluid>, RegistryObject<Item>> entry : map.entrySet()) {
            if (entry.getKey().get().isSame(fluid)) return entry.getValue().get();
        }
        return null;
    }
}
