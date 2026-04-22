package com.robotemployee.reu.util.registry.entry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class BlockRegistryEntry {
    @Nullable
    public final Supplier<Item> ITEM;
    public final Supplier<Block> BLOCK;
    public BlockRegistryEntry(Supplier<Block> BLOCK, @Nullable Supplier<Item> ITEM) {
        this.BLOCK = BLOCK;
        this.ITEM = ITEM;
    }

    public BlockRegistryEntry(Supplier<Block> BLOCK) {
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
