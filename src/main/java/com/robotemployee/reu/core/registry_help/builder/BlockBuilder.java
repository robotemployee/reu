package com.robotemployee.reu.core.registry_help.builder;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.ModBlocks;
import com.robotemployee.reu.core.ModItems;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.core.registry_help.entry.BlockRegistryEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.function.Function;
import java.util.function.Supplier;

public class BlockBuilder {

    Logger LOGGER = LogUtils.getLogger();
    private boolean hasItem = true;
    private boolean hasCreativeTab = true;
    private String name;
    private Supplier<? extends Block> supplier;

    private ItemBuilder itemBuilder = new ItemBuilder();

    public BlockBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public BlockBuilder withSupplier(Supplier<? extends Block> supplier) {
        this.supplier = supplier;
        return this;
    }

    // Build

    public BlockRegistryEntry build() {
        DeferredRegister<Block> BLOCKS = ModBlocks.BLOCKS;
        DeferredRegister<Item> ITEMS = ModItems.ITEMS;

        LOGGER.info("Registering block " + new ResourceLocation(RobotEmployeeUtils.MODID, name));
        checkForInsufficientParams();
        RegistryObject<Block> block = BLOCKS.register(getName(), getSupplier());
        //LOGGER.info("gdagdagaag" + block.getId());
        // do not uncomment
        //LOGGER.info("Blocky block block block" + block.get());

        if (hasItem()) {
            //RegistryObject<Item> item = ITEMS.register(getName(), () -> new BlockItem(block.get(), new Item.Properties()));
            if (!itemBuilder.hasName()) itemBuilder.withName(getName());
            if (!itemBuilder.hasSupplier()) itemBuilder.withSupplier(() -> new BlockItem(block.get(), new Item.Properties()));

            if (!hasCreativeTab) itemBuilder.noCreativeTab();

            RegistryObject<Item> item = itemBuilder.build();
            return new BlockRegistryEntry(block, item);
        }

        return new BlockRegistryEntry(block);
    }

    // Optional things

    public BlockBuilder noCreativeTab() {
        hasCreativeTab = false;
        return this;
    }

    public BlockBuilder noItem() {
        hasItem = false;
        return this;
    }

    public BlockBuilder itemBuilder(Function<ItemBuilder, ItemBuilder> function) {
        this.itemBuilder = function.apply(itemBuilder);
        return this;
    }

    public void checkForInsufficientParams() {
        if (insufficientParams()) throw new IllegalStateException();
    }

    // Getters

    public boolean hasItem() {
        return hasItem;
    }

    public String getName() {
        return name;
    }

    public Supplier<? extends Block> getSupplier() {
        return supplier;
    }

    // Other

    public boolean insufficientParams() {
        return getName() == null || getSupplier() == null;
    }
}
