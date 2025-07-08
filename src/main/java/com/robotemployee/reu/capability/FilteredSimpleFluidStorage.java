package com.robotemployee.reu.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStackSimple;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FilteredSimpleFluidStorage extends FluidHandlerItemStackSimple.SwapEmpty {
    /**
     * @param container The container itemStack, data is stored on it directly as NBT.
     * @param capacity  The maximum capacity of this fluid tank.
     */

    public final FluidStack acceptedFluid;

    public final Leniency leniency;

    public FilteredSimpleFluidStorage(FluidStack acceptedFluid, @NotNull ItemStack container, @NotNull ItemStack emptyContainer, int capacity) {
        this(acceptedFluid, container, emptyContainer, capacity, Leniency.ID);

    }
    public FilteredSimpleFluidStorage(FluidStack acceptedFluid, @NotNull ItemStack container, @NotNull ItemStack emptyContainer, int capacity, Leniency leniency) {
        super(container, emptyContainer, capacity);
        this.acceptedFluid = acceptedFluid;
        if (acceptedFluid.getAmount() > 0) this.setFluid(acceptedFluid);
        this.leniency = leniency;
    }

    @Override
    public boolean canFillFluidType(FluidStack stack) {
        return isFluidGood(stack);
    }

    public boolean isFluidGood(FluidStack stack) {
        if (leniency.isIDStrict() && !stack.getFluid().isSame(acceptedFluid.getFluid())) return false;
        if (leniency.isTagStrict() && !Objects.equals(stack.getTag(), acceptedFluid.getTag())) return false;
        if (!Objects.equals(stack.getTag(), acceptedFluid.getTag())) return false;
        return true;
    }

    @Override
    public void setFluid(FluidStack fluid) {
        super.setFluid(fluid);
    }

    public enum Leniency {
        // does not care about the tags, only wants the fluids themselves to match
        ID,
        // does not care about the fluids themselves, only cares about their tags (why would you want to use this??)
        TAG,
        // cares about both the tags and the id, must match
        ID_AND_TAGS;

        public boolean isTagStrict() {
            return this != ID;
        }

        public boolean isIDStrict() {
            return this != TAG;
        }
    }
}
