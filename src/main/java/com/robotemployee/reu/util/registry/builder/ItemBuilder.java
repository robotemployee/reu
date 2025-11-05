package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.registry.ModCreativeModeTabs;
import com.robotemployee.reu.registry.ModItems;
import com.robotemployee.reu.util.datagen.DatagenInstance;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ItemBuilder {

    private String name;
    private Supplier<Item> supplier;
    private final ArrayList<Supplier<TagKey<Item>>> tags = new ArrayList<>();
    private BiConsumer<DatagenInstance, RegistryObject<Item>> datagenConsumer;
    private final DatagenInstance datagenInstance;
    private final DeferredRegister<Item> register;
    private boolean inCreativeTab = true;

    private boolean doDatagen = true;

    protected ItemBuilder(DatagenInstance datagenInstance, DeferredRegister<Item> register) {
        datagenConsumer = DatagenInstance::basicItem;
        this.datagenInstance = datagenInstance;
        this.register = register;
    }

    public static class Manager {

        public final DatagenInstance datagenInstance;
        public final DeferredRegister<Item> register;
        public Manager(@NotNull DatagenInstance datagenInstance, DeferredRegister<Item> register) {
            this.datagenInstance = datagenInstance;
            this.register = register;
        }

        public ItemBuilder createBuilder() {
            return new ItemBuilder(datagenInstance, register);
        }
    }

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
        if (doDatagen) datagenConsumer.accept(datagenInstance, newborn);
        for (Supplier<TagKey<Item>> tag : tags) {
            datagenInstance.addTagToItem(newborn, tag);
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

    /** see easy consumers for this at the bottom of {@link DatagenInstance.ModItemModelProviderManager}*/
    public ItemBuilder customDatagen(BiConsumer<DatagenInstance, RegistryObject<Item>> datagen) {
        if (!doDatagen) throw new IllegalStateException("Added custom datagen after specifying to not do datagen");
        this.datagenConsumer = datagen;
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
