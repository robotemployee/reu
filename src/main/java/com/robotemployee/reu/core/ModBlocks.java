package com.robotemployee.reu.core;

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
}
