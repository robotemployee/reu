package com.robotemployee.reu.capability;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;


/**
 Only designed to be drained.
 this is essentially  the uhhhhhh yknow fluid storage item thing swap empty whatever except it doesn't need a DataComponent
 because i didn't think it should need one when it's all or nothing
 */
public class AllOrNothingFluidStorageItem implements IFluidHandlerItem {
    public final FluidStack fluidStack;
    public final Function<ItemStack, ItemStack> newStackWhenDrained;
    public ItemStack stack;

    public AllOrNothingFluidStorageItem(FluidStack fluidStack, ItemStack stack, Function<ItemStack, ItemStack> newStackWhenDrained) {
        this.fluidStack = fluidStack;
        this.newStackWhenDrained = newStackWhenDrained;
        this.stack = stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int i) {
        return null;
    }

    @Override
    public int getTankCapacity(int i) {
        return fluidStack.getAmount();
    }

    @Override
    public boolean isFluidValid(int i, FluidStack fluidStack) {
        return false;
    }

    @Override
    public int fill(FluidStack fluidStack, FluidAction fluidAction) {
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, FluidAction fluidAction) {
        return fluidStack.is(this.fluidStack.getFluidType()) ? drain(fluidStack.getAmount(), fluidAction) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int i, FluidAction fluidAction) {
        // drains completely even if the amount requested is less
        // this is definitely a problem but counterpoint i don't care
        // and besides whatever's draining from it would proabably be able to handle the amount it drained being bigger than what it asked for
        // this is PURE. COPIUM because i don't want to register a whole DataComponent it feels scary for this

        if (newStackWhenDrained != null) {
            stack = newStackWhenDrained.apply(stack);
        }
        return fluidStack;
    }

    @Override
    public ItemStack getContainer() {
        return null;
    }
}
