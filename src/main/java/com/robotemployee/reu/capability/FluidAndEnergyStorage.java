package com.robotemployee.reu.capability;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;

public class FluidAndEnergyStorage implements ICapabilitySerializable<CompoundTag> {

    private final Logger LOGGER = LogUtils.getLogger();
    private final LazyOptional<IFluidHandlerItem> fluidHandler;
    private final LazyOptional<IEnergyStorage> energyHandler;
    public FluidAndEnergyStorage(ItemStack itemStack, int fluidCapacity, int energyCapacity) {
        this.fluidHandler = LazyOptional.of(() -> new CustomFluidHandlerItemStack(itemStack, fluidCapacity));
        this.energyHandler = LazyOptional.of(() -> new EnergyHandlerItemStack(itemStack, energyCapacity) {
            @Override
            public boolean canExtract() { return true; }
        });
    }

    @Override
    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
            return fluidHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Fluid", fluidHandler.resolve().map(h -> ((FluidHandlerItemStack)h).getFluid().writeToNBT(new CompoundTag())).orElse(new CompoundTag()));
        energyHandler.resolve().map(IEnergyStorage::getEnergyStored).ifPresent(i -> tag.putInt("Energy", i));
        //("Serializing NBT: " + tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        //LOGGER.info("Deserializing NBT: " + nbt);
        //LOGGER.info("Current energy: " + energyHandler.map(IEnergyStorage::getEnergyStored).orElse(0));
        //LOGGER.info("Current fluid: " + fluidHandler.map(handler -> ((FluidHandlerItemStack)handler).getFluid().getAmount()).orElse(0));
        if (nbt.contains("Energy")) {
            energyHandler.resolve().ifPresent(handler -> {
                ((EnergyHandlerItemStack)handler).setEnergy(nbt.getInt("Energy"));
            });
        }
        if (nbt.contains("Fluid")) {
            fluidHandler.resolve().ifPresent(handler -> {
                //LOGGER.info("old " + ((FluidHandlerItemStack)handler).getFluid().getAmount());
                ((CustomFluidHandlerItemStack)handler).setFluid(FluidStack.loadFluidStackFromNBT(nbt.getCompound("Fluid")));
                //LOGGER.info("new " + ((FluidHandlerItemStack)handler).getFluid().getAmount());
            });
        }
    }

    public static class EnergyHandlerItemStack extends EnergyStorage implements ICapabilityProvider {
        // the purpose of this is to save the energy to the item's nbt. why? i forgot lmao
        @NotNull
        protected ItemStack container;
        public EnergyHandlerItemStack(@NotNull ItemStack container, int capacity) {
            super(capacity);
            this.container = container;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate)
        {
            if (!canReceive())
                return 0;

            int energyReceived = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));
            if (!simulate) {
                energy += energyReceived;
                updateTag();
            }
            return energyReceived;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!canExtract())
                return 0;

            int energyExtracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));
            if (!simulate) {
                energy -= energyExtracted;
                updateTag();
            }
            return energyExtracted;
        }

        public void setEnergy(int newEnergy) {
            energy = newEnergy;
        }

        public void updateTag() {
            CompoundTag tag = container.getOrCreateTag();
            tag.putInt("Energy", energy);
            container.setTag(tag);
        }

        private final LazyOptional<IEnergyStorage> holder = LazyOptional.of(() -> this);
        @Override
        @NotNull
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
            return ForgeCapabilities.ENERGY.orEmpty(capability, holder);
        }
    }

    public static class CustomFluidHandlerItemStack extends FluidHandlerItemStack {
        public CustomFluidHandlerItemStack(@NotNull ItemStack container, int capacity) {
            super(container, capacity);
        }

        @Override
        public boolean canDrainFluidType(FluidStack fluid) {
            return false;
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            if (!fluid.hasTag()) return false;
            CompoundTag tag = fluid.getTag();
            String bottle = tag.getString("Bottle");
            String potion = tag.getString("Potion");
            return (bottle.equals("REGULAR") && potion.equals("minecraft:healing"));
        }

        @Override
        public void setFluid(FluidStack fluid) {
            super.setFluid(fluid);
        }

        @Override
        public void setContainerToEmpty()
        {
            super.setContainerToEmpty();
        }
    }
}