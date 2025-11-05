package com.robotemployee.reu.util.registry.entry;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.function.Supplier;

public class CreativeTabMutableRegistryEntry {
    // note that this class NOT FINALIZED
    // you are meant to add items to it before it is registered, it differs from the other RegistryEntry-type stuff in that way
    protected RegistryObject<CreativeModeTab> tab;
    protected final ArrayList<Supplier<Item>> addedItems = new ArrayList<>();

    public final String name;
    public final Supplier<ItemStack> iconSupplier;
    public final Component title;

    public CreativeTabMutableRegistryEntry(String name, Supplier<ItemStack> iconSupplier, Component title, DeferredRegister<CreativeModeTab> register) {
        this.name = name;
        this.iconSupplier = iconSupplier;
        this.title = title;
        tab = register.register(name, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(iconSupplier.get().getItem()))
                .title(Component.translatable("creativetab." + name))
                .displayItems((parameters, output) -> {
                    for (Supplier<Item> item : addedItems) output.accept(item.get());
                })
                .build()
        );
    }

    public void addItem(Supplier<Item> addedItem) {
        addedItems.add(addedItem);
    }

    public RegistryObject<CreativeModeTab> getRegistryObject() {
        return tab;
    }

    public CreativeModeTab getTab() {
        return getRegistryObject().get();
    }
}
