package com.robotemployee.reu.core;

import com.robotemployee.reu.block.BlankEggBlock;
import com.robotemployee.reu.block.InfusedEggBlock;
import com.robotemployee.reu.core.registry_help.builder.BlockBuilder;

import com.robotemployee.reu.core.registry_help.entry.BlockRegistryEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RobotEmployeeUtils.MODID);
    public static final DeferredRegister<Item> ITEMS = ModItems.ITEMS;

    // BlockRegistryEntry has both a block and an item

    public static final BlockRegistryEntry INFUSED_EGG = new BlockBuilder()
            .withName("infused_egg")
            .withSupplier(() -> new InfusedEggBlock(BlockBehaviour.Properties.copy(Blocks.SNIFFER_EGG)))
            .build();

    public static final BlockRegistryEntry BLANK_EGG = new BlockBuilder()
            .withName("blank_egg")
            .withSupplier(() -> new BlankEggBlock(BlockBehaviour.Properties.copy(Blocks.SNIFFER_EGG)))
            .build();

}
