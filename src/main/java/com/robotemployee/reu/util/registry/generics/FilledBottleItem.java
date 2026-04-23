package com.robotemployee.reu.util.registry.generics;

import com.robotemployee.reu.capability.AllOrNothingFluidStorageItem;
import com.robotemployee.reu.capability.IHasCapability;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class FilledBottleItem extends Item implements IHasCapability<Item> {


    private final FluidStack fluidStack;

    public FilledBottleItem(FluidStack fluidStack, Properties properties) {
        super(properties);
        this.fluidStack = fluidStack;
    }

    public FilledBottleItem(Supplier<Fluid> fluid, Properties properties) {
        this(new FluidStack(fluid.get(), 250), properties);
    }



    public FluidStack getOriginalFluidStack(ItemStack stack) {
        return fluidStack;
    }
    public FluidStack getFluidStack(ItemStack stack) {
        return getOriginalFluidStack(stack).copy();
    }

    @Override @NotNull
    public ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        super.finishUsingItem(stack, level, entity);
        if (entity instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
            ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(glassBottle)) {
                player.drop(glassBottle, false);
            }
        }

        return new ItemStack(Items.GLASS_BOTTLE);
    }

    public int getUseDuration(@NotNull ItemStack stack) {
        return 40;
    }

    @Override @NotNull
    public UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override @NotNull
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override @NotNull
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onRegisteringCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.FluidHandler.ITEM,
                (stack, context) -> new AllOrNothingFluidStorageItem(getFluidStack(stack), stack, (s) -> new ItemStack(Items.GLASS_BOTTLE)),
                this
        );
    }
}
