package com.robotemployee.reu.capability;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashSet;

public class FlowerCounterCapability implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    static Logger LOGGER = LogUtils.getLogger();

    public FlowerCounter flowerHandler;

    private final LazyOptional<FlowerCounter> CAP = LazyOptional.of(this::getFlowerHandler);

    public FlowerCounter getFlowerHandler() {
        if (flowerHandler == null) flowerHandler = new FlowerCounter();
        return flowerHandler;
    }

    @Override
    public CompoundTag serializeNBT() {
        return getFlowerHandler().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        getFlowerHandler().deserializeNBT(tag);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return (cap == CAPABILITY) ? CAP.cast() : LazyOptional.empty();
    }

    public static final Capability<FlowerCounter> CAPABILITY = CapabilityManager.get(new CapabilityToken<FlowerCounter>() {});

    public static class FlowerCounter implements INBTSerializable<CompoundTag> {
        public static final String CONSUMED_PATH = "ConsumedFlowers";

        Logger LOGGER = LogUtils.getLogger();

        protected HashSet<ResourceLocation> consumedFlowers = new HashSet<>();

        // returns whether or not it was successful
        public boolean addFlower(ItemStack flower) {
            if (!flower.is(ItemTags.FLOWERS)) return false;
            ResourceLocation loc = ForgeRegistries.ITEMS.getKey(flower.getItem());
            if (consumedFlowers.contains(loc)) return false;
            consumedFlowers.add(loc);
            return true;
        }

        public int getCount() {
            return consumedFlowers.size();
        }

        public boolean hasEaten(ItemStack flower) {
            if (!flower.is(ItemTags.FLOWERS)) return false;
            ResourceLocation loc = ForgeRegistries.ITEMS.getKey(flower.getItem());
            return consumedFlowers.contains(loc);
        }

        public void reset() {
            consumedFlowers.clear();
        }

        public boolean isFlower(ItemStack flower) {
            if (flower.is(ItemTags.FLOWERS)) return true;
            //if (flower.getItem() instanceof BlockItem blockItem && blockItem.getBlock().is(BlockTags.FLOWERS)) return true;
            return false;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (ResourceLocation loc : consumedFlowers) list.add(StringTag.valueOf(loc.toString()));
            tag.put(CONSUMED_PATH, list);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            reset();
            ListTag list = tag.getList(CONSUMED_PATH, Tag.TAG_STRING);
            for (Tag entry : list) {
                if (!(entry instanceof StringTag)) LOGGER.error("FlowerCounter attempting to deserialize NBT data where the list of consumed flowers does not have all StringTag. This is likely due to corrupt NBT");
                consumedFlowers.add(new ResourceLocation(entry.getAsString()));
            }
        }
    }

}
