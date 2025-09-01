package com.robotemployee.reu.registry;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RobotEmployeeUtils.MODID);

    // BlockRegistryEntry has both a block and an item

    public static class Patterns {
        //public static final BlockPattern ALTAR
    }
}
