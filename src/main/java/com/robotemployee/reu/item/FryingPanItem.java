package com.robotemployee.reu.item;

import com.robotemployee.reu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FryingPanItem extends Item {

    public static final String HELD_PATH = "HeldItem";

    public FryingPanItem(Properties properties) {
        super(properties);
    }

    public static ItemStack craftFrom(ItemStack held) {
        return saveTo(new ItemStack(ModItems.FRYING_PAN.get()), held);
    }

    public static ItemStack saveTo(ItemStack pan, ItemStack held) {
        CompoundTag savedTag = held.save(new CompoundTag());

        CompoundTag newTag = pan.getOrCreateTag();
        newTag.put(HELD_PATH, savedTag);
        pan.setTag(newTag);

        return pan;
    }

    @Override
    @NotNull
    public UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return 40;
    }



    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override @NotNull
    public ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(HELD_PATH)) {
            if (level.isClientSide()) entity.sendSystemMessage(Component.literal("Error due to the frying pan having no tag"));
            return ItemStack.EMPTY;
        }

        entity.playSound(SoundEvents.FIRE_EXTINGUISH);

        return ItemStack.of(tag.getCompound(HELD_PATH));
    }
}
