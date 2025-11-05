package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.registry.ModCreativeModeTabs;
import com.robotemployee.reu.registry.ModItems;
import com.robotemployee.reu.util.datagen.Datagen;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemBuilder {

    private String name;
    private Supplier<Item> supplier;
    private final ArrayList<Supplier<TagKey<Item>>> tags = new ArrayList<>();
    private Consumer<RegistryObject<Item>> datagen = Datagen::basicItem;
    private boolean inCreativeTab = true;

    private boolean doDatagen = true;

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
        if (doDatagen) datagen.accept(newborn);
        for (Supplier<TagKey<Item>> tag : tags) {
            Datagen.addTagToItem(newborn, tag);
        }
        return newborn;
    }

    public ItemBuilder addTag(Supplier<TagKey<Item>> tagSupplier) {
        this.tags.add(tagSupplier);
        return this;
    }

    public boolean hasName() {
        return name != null;
    }

    public boolean hasSupplier() {
        return supplier != null;
    }

    /** see easy consumers for this at the bottom of {@link Datagen.ModItemModelProvider}*/
    public ItemBuilder customDatagen(Consumer<RegistryObject<Item>> datagen) {
        if (!doDatagen) throw new IllegalStateException("Added custom datagen after specifying to not do datagen");
        this.datagen = datagen;
        return this;
    }

    public ItemBuilder noDatagen() {
        this.doDatagen = false;
        return this;
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
