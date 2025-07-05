package com.robotemployee.reu.capability;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Objects;

public class FilteredFluidStorage extends FluidHandlerItemStack {
    /**
     * @param container The container itemStack, data is stored on it directly as NBT.
     * @param capacity  The maximum capacity of this fluid tank.
     */

    Logger LOGGER = LogUtils.getLogger();

    public final FluidStack acceptedFluid;

    public final FilteredSimpleFluidStorage.Leniency leniency;

    public FilteredFluidStorage(FluidStack acceptedFluid, @NotNull ItemStack container, int capacity) {
        this(acceptedFluid, container, capacity, FilteredSimpleFluidStorage.Leniency.ID);

    }
    public FilteredFluidStorage(FluidStack acceptedFluid, @NotNull ItemStack container, int capacity, FilteredSimpleFluidStorage.Leniency leniency) {
        super(container, capacity);
        this.acceptedFluid = acceptedFluid;
        //if (acceptedFluid.getAmount() > 0) this.setFluid(acceptedFluid);
        this.leniency = leniency;
    }


    @Override
    public boolean canFillFluidType(FluidStack stack) {
        return isFluidGood(stack);
    }

    public boolean isFluidGood(FluidStack stack) {

        if (leniency.isIDStrict() && !stack.getFluid().isSame(acceptedFluid.getFluid())) {
            //LOGGER.info("Fluids did not match ID");
            return false;
        }
        if (leniency.isTagStrict() && !Objects.equals(stack.getTag(), acceptedFluid.getTag())) {
            //LOGGER.info("Fluids did not match tag");
            return false;
        }

        return true;
    }
}
