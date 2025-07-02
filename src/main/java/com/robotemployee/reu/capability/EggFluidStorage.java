package com.robotemployee.reu.capability;

import com.robotemployee.reu.core.ModFluids;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;

public class EggFluidStorage implements IFluidHandler {

    protected ArrayList<FluidTank> tanks = new ArrayList<FluidTank>();

    public EggFluidStorage(int capacity) {
        tanks.add(new FluidTank(capacity));
    }

    public EggFluidStorage(int capacity, int tanksAmount) {
        for (int i = 1; i < tanksAmount; i++) {
            tanks.add(new FluidTank(capacity));
        }
    }

    public EggFluidStorage(FluidTank... tanks) {
        Collections.addAll(this.tanks, tanks);
    }

    public boolean isMono() {
        return getTanks() == 1;
    }

    @Override
    public int getTanks() {
        return tanks.size();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return isMono() ? getFluid() : tanks.get(tank).getFluid();
    }

    public @NotNull FluidStack getFluid() {
        return tanks.get(0).getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tanks.get(tank).getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return isMono() ? isFluidValid(stack) : tanks.get(tank).isFluidValid(stack);
    }

    public boolean isFluidValid(@NotNull FluidStack stack) {
        return tanks.get(0).isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return 0;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (!canDrainFluidType(resource)) return FluidStack.EMPTY;
        int index = getMatchingTank(resource);
        return (index > -1) ? drain(index, resource, action) : FluidStack.EMPTY;
    }

    public @NotNull FluidStack drain(int tank, FluidStack resource, FluidAction action) {
        if (!canDrainFluidType(resource)) return FluidStack.EMPTY;
        return tanks.get(tank).drain(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        FluidTank tank = getAnyFilledTank();
        return tank != null ? tank.drain(maxDrain, action) : FluidStack.EMPTY;
    }

    public int getMatchingTank(FluidStack stack) {
        int i = 1;
        for (FluidTank tank : tanks) {
            if (tank.getFluid().isFluidEqual(stack)) return i;
            i++;
        }
        return -1;
    }

    public boolean hasRoomFor(FluidStack stack) {
        for (FluidTank tank : tanks) {
            FluidStack contained = tank.getFluid();
            if (tank.isEmpty() || (contained.isFluidEqual(stack) && tank.getCapacity() - contained.getAmount() > stack.getAmount())) {
                return true;
            }
        }
        return false;
    }

    public FluidTank getAnyFilledTank() {
        for (FluidTank tank : tanks) {
            if (tank != null) return tank;
        }
        return null;
    }

    public boolean canFillFluidType(FluidStack fluid) {
        return hasRoomFor(fluid) && fluid.getFluid().isSame(ModFluids.MOB_FLUID.getFlow()) || fluid.getFluid().isSame(ModFluids.MOB_FLUID.getSource());
    }

    public boolean canDrainFluidType(FluidStack fluid) {
        return getMatchingTank(fluid) > -1;
    }
}
