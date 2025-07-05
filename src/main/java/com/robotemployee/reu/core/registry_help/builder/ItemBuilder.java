package com.robotemployee.reu.core.registry_help.builder;

import com.robotemployee.reu.core.ModCreativeModeTabs;
import com.robotemployee.reu.core.ModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ItemBuilder {

    private String name;
    private Supplier<Item> supplier;
    private boolean inCreativeTab = true;

    public ItemBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ItemBuilder withSupplier(Supplier<Item> supplier) {
        this.supplier = supplier;
        return this;
    }

    public RegistryObject<Item> build() {
        checkForInsufficientParams();
        RegistryObject<Item> newborn = ModItems.ITEMS.register(name, supplier);
        if (inCreativeTab) ModCreativeModeTabs.addItem(newborn);
        return newborn;
    }

    public boolean hasName() {
        return name != null;
    }

    public boolean hasSupplier() {
        return supplier != null;
    }

    public ItemBuilder noCreativeTab() {
        this.inCreativeTab = false;
        return this;
    }

    public boolean insufficientParams() {
        return name == null || supplier == null;
    }

    public void checkForInsufficientParams() {
        if (insufficientParams()) throw new IllegalStateException("Attempted to build a fluid with insufficient parameters");
    }
}
