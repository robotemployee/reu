package com.robotemployee.reu.capability;

import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * energy storage that, instead of storing its energy directly as a field, treats a supplier and consumer as a getter and setter.
 * use this with whatever data saving method you want to have like, easy persistence
 */
public class BasedEnergyStorage implements IEnergyStorage {
    protected int capacity;
    protected int maxRecieve;
    protected int maxExtract;

    protected final Supplier<Integer> capacitySupplier;
    protected final Consumer<Integer> capacityConsumer;

    public BasedEnergyStorage(int capacity, Supplier<Integer> capacitySupplier, Consumer<Integer> capacityConsumer) {
        this(capacity, capacity, capacity, capacitySupplier, capacityConsumer);
    }

    public BasedEnergyStorage(int capacity, int maxRecieve, int maxExtract, Supplier<Integer> capacitySupplier, Consumer<Integer> capacityConsumer) {
        this.capacity = capacity;
        this.maxRecieve = maxRecieve;
        this.maxExtract = maxExtract;

        this.capacitySupplier = capacitySupplier;
        this.capacityConsumer = capacityConsumer;
    }


    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        if (amount < 0) return 0;
        int energy = getEnergyStored();
        int amountCanDo = Math.min(Math.min(amount, maxRecieve), capacity - energy);
        if (!simulate) setEnergyStored(energy + amountCanDo);
        return amountCanDo;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        if (amount < 0) return 0;
        int energy = getEnergyStored();
        int amountCanDo = Math.min(Math.min(amount, maxExtract), energy);
        if (!simulate) setEnergyStored(energy - amountCanDo);
        return amountCanDo;
    }

    @Override
    public int getEnergyStored() {
        return capacitySupplier.get();
    }

    public void setEnergyStored(int energy) {
        capacityConsumer.accept(energy);
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
