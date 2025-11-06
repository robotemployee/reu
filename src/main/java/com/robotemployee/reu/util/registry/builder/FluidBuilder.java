package com.robotemployee.reu.util.registry.builder;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.datagen.DatagenInstance;
import com.robotemployee.reu.util.registry.entry.BlockRegistryEntry;
import com.robotemployee.reu.util.registry.entry.FluidRegistryEntry;
import com.robotemployee.reu.util.registry.generics.FilledBottleItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class FluidBuilder {

    Logger LOGGER = LogUtils.getLogger();

    private String name;

    private boolean hasBucket = true;
    private boolean hasBottle = true;
    private boolean hasBlock = true;

    Mode mode;

    private Supplier<ForgeFlowingFluid.Properties> propertiesSupplier;

    private Supplier<Fluid> normalSupplier;

    private Supplier<LiquidBlock> blockSupplier;

    private Supplier<Item> bucketSupplier;
    private Supplier<Item> bottleSupplier;

    private ResourceLocation bucketTexture;
    private ResourceLocation bottleTexture;

    int tint = 0xFFFFFF;

    public static class Manager {
        private final DatagenInstance datagenInstance;
        private final ItemBuilder.Manager itemManager;
        private final BlockBuilder.Manager blockManager;
        private final DeferredRegister<Fluid> register;
        public Manager(DatagenInstance datagenInstance, ItemBuilder.Manager itemManager, BlockBuilder.Manager blockManager, DeferredRegister<Fluid> register) {
            this.datagenInstance = datagenInstance;
            this.itemManager = itemManager;
            this.blockManager = blockManager;
            this.register = register;
        }

        public FluidBuilder createBuilder() {
            return new FluidBuilder(datagenInstance, itemManager, blockManager, register);
        }
    }

    private final DatagenInstance datagenInstance;
    private final ItemBuilder.Manager itemManager;
    private final BlockBuilder.Manager blockManager;
    private final DeferredRegister<Fluid> register;
    private FluidBuilder(DatagenInstance datagenInstance, ItemBuilder.Manager itemManager, BlockBuilder.Manager blockManager, DeferredRegister<Fluid> register) {
        this.datagenInstance = datagenInstance;
        this.itemManager = itemManager;
        this.blockManager = blockManager;
        this.register = register;
    }

    // Required things

    public FluidBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public FluidBuilder flowing(Supplier<ForgeFlowingFluid.Properties> propertiesSupplier) {
        if (mode != null) throw new IllegalStateException("Attempted to add flowing fluid parameters to a fluid that was already given a fluid supplier");
        this.propertiesSupplier = propertiesSupplier;
        this.mode = Mode.FLOWING;
        return this;
    }

    public FluidBuilder nonFlowing(Supplier<Fluid> normalSupplier) {
        if (mode != null) throw new IllegalStateException("Attempted to add non-flowing fluid parameters to a fluid that was already given a fluid supplier");
        this.normalSupplier = normalSupplier;
        this.mode = Mode.NON_FLOWING;
        return this.intangible();
    }

    // Building

    public FluidRegistryEntry build() {

        DeferredRegister<Fluid> FLUIDS = register;

        checkForInsufficientParams();

        Supplier<ForgeFlowingFluid.Properties> newPropertiesSupplier = () -> {
            ForgeFlowingFluid.Properties props = propertiesSupplier.get();
            if (hasBucket) props = props.bucket(() -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(RobotEmployeeUtils.MODID, name + "_bucket")));
            if (hasBlock) props = props.block(() -> (LiquidBlock)ForgeRegistries.BLOCKS.getValue(new ResourceLocation(RobotEmployeeUtils.MODID, name)));
            return props;
        };



        RegistryObject<Fluid> sourceFluid;
        RegistryObject<Fluid> flowingFluid = null;

        if (mode.flowing()) {
            sourceFluid = FLUIDS.register(name, () -> new ForgeFlowingFluid.Source(newPropertiesSupplier.get()));
            flowingFluid = FLUIDS.register("flowing_" + name, () -> new ForgeFlowingFluid.Flowing(newPropertiesSupplier.get()));
        } else {
            sourceFluid = FLUIDS.register(name, normalSupplier);
        }


        RegistryObject<Item> bucket = null;
        RegistryObject<Item> bottle = null;
        BlockRegistryEntry block = null;

        if (hasBucket) {
            Supplier<Item> supplier = getBucketSupplier(sourceFluid);
            bucket = itemManager.createBuilder().withName(name + "_bucket").withSupplier(supplier).build();
            // fixme FluidTools.bucketLookupTable.put(sourceFluid, bucket);
            queueBucketDatagen(bucket);
        }
        if (hasBottle) {
            Supplier<Item> supplier = getBottleSupplier(sourceFluid);
            bottle = itemManager.createBuilder().withName(name + "_bottle").withSupplier(supplier).build();
            // fixme FluidTools.bottleLookupTable.put(sourceFluid, bottle);
            queueBottleDatagen(bottle);
        }
        if (hasBlock) {
            block = blockManager.createBuilder().withName(name).withSupplier(getBlockSupplier(() -> (ForgeFlowingFluid.Source)sourceFluid.get())).noCreativeTab().build();
            //block = BLOCKS.register(name, getBlockSupplier());
        }

        return new FluidRegistryEntry(flowingFluid, sourceFluid, block, bucket, bottle);
    }

    // Optional stuff

    public FluidBuilder noBucket() {
        if (bucketSupplier != null) throw new IllegalStateException("Attempted to create a fluid with no bucket after specifying a bucket supplier");
        hasBucket = false;
        return this;
    }

    public FluidBuilder noBottle() {
        if (bottleSupplier != null) throw new IllegalStateException("Attempted to create a fluid with no bottle after specifying a bottle supplier");
        hasBottle = false;
        return this;
    }

    public FluidBuilder noBlock() {
        if (blockSupplier != null) throw new IllegalStateException("Attempted to create a fluid with no block after specifying a block supplier");
        hasBlock = false;
        return this;
    }

    // macro to remove the block, bucket, and bottle
    public FluidBuilder intangible() {
        return this.noBlock().noBucket().noBottle();
    }

    public FluidBuilder withBlockSupplier(Supplier<LiquidBlock> blockSupplier) {
        if (!this.hasBlock) throw new IllegalStateException("Attempted to provide a block supplier to a fluid which was specified to have no block");
        this.blockSupplier = blockSupplier;
        return this;
    }

    public FluidBuilder withBucketSupplier(Supplier<Item> bucketSupplier) {
        if (!hasBucket) throw new IllegalStateException();
        this.bucketSupplier = bucketSupplier;
        return this;
    }

    public FluidBuilder withBottleSupplier(Supplier<Item> bottleSupplier) {
        if (!hasBottle) throw new IllegalStateException();
        this.bottleSupplier = bottleSupplier;
        return this;
    }

    // Other

    public int getTint() {
        return tint;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public Supplier<LiquidBlock> getBlockSupplier(Supplier<ForgeFlowingFluid.Source> sourceSupplier) {
        if (!hasBlock) return null;
        if (blockSupplier != null) return blockSupplier;
        if (mode.nonFlowing()) throw new IllegalStateException("Attempted to register the block for a fluid that is registered as non-flowing");

        return () -> new LiquidBlock(sourceSupplier, BlockBehaviour.Properties.copy(Blocks.WATER));
    }

    @Nullable
    public Supplier<Item> getBucketSupplier(RegistryObject<Fluid> fluid) {
        if (!hasBucket) return null;
        if (bucketSupplier != null) return bucketSupplier;
        return () -> new BucketItem(fluid, (new Item.Properties()).craftRemainder(Items.BUCKET).stacksTo(1));
    }

    @Nullable
    public Supplier<Item> getBottleSupplier(RegistryObject<Fluid> fluid) {
        if (!hasBottle) return null;
        if (bottleSupplier != null) return bottleSupplier;

        return () -> new FilledBottleItem(fluid, (new Item.Properties()).stacksTo(16));
    }

    // misc

    // note that these won't create a texture if there's already a texture there in not generated

    public boolean insufficientParams() {
        if (getName() == null) return true;
        switch (mode) {
            case FLOWING -> {
                //return sourceSupplier == null || flowingSupplier == null;
            }
            case NON_FLOWING -> {
                //return sourceSupplier == null;
            }
            default -> {
                return true;
            }
        }

        return propertiesSupplier == null;

        // if mode is undefined
        //return true;
    }

    public void checkForInsufficientParams() {
        if (insufficientParams()) throw new IllegalStateException("Attempted to build a fluid with insufficient parameters");
    }

    private void queueBucketDatagen(Supplier<Item> supplier) {
        datagenInstance.generateBucket(supplier);
    }

    private void queueBottleDatagen(Supplier<Item> supplier) {
        datagenInstance.generateBottle(supplier);
    }

    public enum Mode {
        FLOWING,
        NON_FLOWING;

        public boolean flowing() {
            return this == FLOWING;
        }
        public boolean nonFlowing() {
            return !flowing();
        }
    }
}