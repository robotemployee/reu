package com.robotemployee.reu.util.registry.entry;

import com.robotemployee.reu.util.registry.builder.FluidBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class FluidRegistryEntry {

    private final Supplier<Fluid> SOURCE;
    private final Supplier<Fluid> FLOW;
    private final Supplier<Item> BUCKET;
    private final Supplier<Item> BOTTLE;
    private final BlockRegistryEntry BLOCK;

    public final FluidBuilder.Mode MODE;

    public FluidRegistryEntry(Supplier<Fluid> flow, Supplier<Fluid> source, @Nullable BlockRegistryEntry block, @Nullable Supplier<Item> bucket, @Nullable Supplier<Item> bottle) {
        this.SOURCE = source;
        this.FLOW = flow;
        this.BLOCK = block;
        this.BUCKET = bucket;
        this.BOTTLE = bottle;
        this.MODE = flow != null ? FluidBuilder.Mode.FLOWING : FluidBuilder.Mode.NON_FLOWING;
    }

    public FluidRegistryEntry(Supplier<Fluid> flow, Supplier<Fluid> source) {
        this(flow, source, null, null, null);
    }

    public FluidRegistryEntry(Supplier<Fluid> source) {
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
    public Supplier<Fluid> getFlowRegistry() { return (MODE.flowing()) ? FLOW : null; }
    public Supplier<Fluid> getSourceRegistry() { return SOURCE; }

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
