package com.robotemployee.reu.banana.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * <p>This item decays over time. In order to facilitate that,
 * it remembers the time it was created in the NBT tag
 * and references that in order to calculate what to set the damage to.</p>
 * <p>Specifically, it remembers the (truncated) seconds since epoch it was created.</p>
 * <p>It doesn't use ticks because I couldn't figure out how to get the Level from an ItemStack.
 * It seems like it is meant to be provided with it only at the exact time and scope that such is needed.</p>
 * */
public class SemisolidItem extends Item {

    public static final String DECAY_KEY = "DecayTime";
    public static final int DECAY_SECONDS = 120;

    public SemisolidItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public int getDamage(ItemStack stack) {
        long time = getTime(stack);
        return (int)time;
    }

    public long getTime(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(DECAY_KEY)) {
            tag.putLong(DECAY_KEY, System.currentTimeMillis() / 1000);
            return 0;
        }

        return (System.currentTimeMillis() / 1000) - tag.getLong(DECAY_KEY);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int ignored1, boolean bignored) {
        super.inventoryTick(stack, level, entity, ignored1, bignored);
        if (level.isClientSide()) return;

        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(DECAY_KEY)) {
            tag.putLong(DECAY_KEY, System.currentTimeMillis() / 1000);
            stack.setTag(tag);
            return;
        }
        vibeChecker(stack);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        if (vibeChecker(stack)) return false;
        return super.onDroppedByPlayer(stack, player);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (vibeChecker(stack)) {
            entity.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }



    public boolean vibeChecker(ItemStack stack) {
        if (getDamage(stack) > getMaxDamage(stack)) {
            stack.shrink(1);
            return true;
        }
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
