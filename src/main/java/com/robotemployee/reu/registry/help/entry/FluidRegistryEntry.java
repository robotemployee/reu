package com.robotemployee.reu.registry.help.entry;

import com.robotemployee.reu.registry.help.builder.FluidBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

public class FluidRegistryEntry {

    private final RegistryObject<Fluid> SOURCE;
    private final RegistryObject<Fluid> FLOW;
    private final RegistryObject<Item> BUCKET;
    private final RegistryObject<Item> BOTTLE;
    private final BlockRegistryEntry BLOCK;

    public final FluidBuilder.Mode MODE;

    public FluidRegistryEntry(RegistryObject<Fluid> flow, RegistryObject<Fluid> source, @Nullable BlockRegistryEntry block, @Nullable RegistryObject<Item> bucket, @Nullable RegistryObject<Item> bottle) {
        this.SOURCE = source;
        this.FLOW = flow;
        this.BLOCK = block;
        this.BUCKET = bucket;
        this.BOTTLE = bottle;
        this.MODE = flow != null ? FluidBuilder.Mode.FLOWING : FluidBuilder.Mode.NON_FLOWING;
    }

    public FluidRegistryEntry(RegistryObject<Fluid> flow, RegistryObject<Fluid> source) {
        this(flow, source, null, null, null);
    }

    public FluidRegistryEntry(RegistryObject<Fluid> source) {
        this(null, source);
    }

    @Nullable
    public Fluid getFlow() {
        return (MODE.flowing()) ? FLOW.get() : null;
    }

    public Fluid getSource() {
        return SOURCE.get();
    }

    @Nullable
    public RegistryObject<Fluid> getFlowRegistry() { return (MODE.flowing()) ? FLOW : null; }
    public RegistryObject<Fluid> getSourceRegistry() { return SOURCE; }

    @Nullable
    public Item getBucket() { return BUCKET != null ? BUCKET.get() : null; }

    @Nullable
    public Item getBottle() { return BOTTLE != null ? BOTTLE.get() : null; }

    @Nullable
    public LiquidBlock getBlock() { return BLOCK != null ? (LiquidBlock)BLOCK.get() : null; }

    // only works when there's no flowing fluid
    @Nullable
    public Fluid get() {
        return SOURCE.get();
    }
}
