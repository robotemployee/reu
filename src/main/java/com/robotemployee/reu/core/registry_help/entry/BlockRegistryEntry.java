package com.robotemployee.reu.core.registry_help.entry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

public class BlockRegistryEntry {
    @Nullable
    public final RegistryObject<Item> ITEM;
    public final RegistryObject<Block> BLOCK;
    public BlockRegistryEntry(RegistryObject<Block> BLOCK, @Nullable RegistryObject<Item> ITEM) {
        this.BLOCK = BLOCK;
        this.ITEM = ITEM;
    }

    public BlockRegistryEntry(RegistryObject<Block> BLOCK) {
        this.BLOCK = BLOCK;
        this.ITEM = null;
    }

    public Block get() {
        return BLOCK.get();
    }

    public Item getItem() {
        if (!hasItem()) throw new IllegalStateException();
        return ITEM.get();
    }

    public boolean hasItem() {
        return ITEM != null;
    }
}
