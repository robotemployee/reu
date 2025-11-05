package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.util.registry.entry.CreativeTabMutableRegistryEntry;
import com.robotemployee.reu.util.datagen.DatagenInstance;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    @Nullable
    private CreativeTabMutableRegistryEntry creativeModeTab;

    private boolean doDatagen = true;

    protected ItemBuilder(DatagenInstance datagenInstance, DeferredRegister<Item> register, CreativeTabMutableRegistryEntry creativeModeTab) {
        datagenConsumer = DatagenInstance::basicItem;
        this.datagenInstance = datagenInstance;
        this.register = register;
        this.creativeModeTab = creativeModeTab;
    }

    public static class Manager {

        public final DatagenInstance datagenInstance;
        public final DeferredRegister<Item> register;
        @Nullable
        public CreativeTabMutableRegistryEntry defaultTab;
        public Manager(@NotNull DatagenInstance datagenInstance, DeferredRegister<Item> register) {
            this.datagenInstance = datagenInstance;
            this.register = register;
        }

        public Manager defaultCreativeTab(CreativeTabMutableRegistryEntry defaultTab) {
            this.defaultTab = defaultTab;
            return this;
        }

        public ItemBuilder createBuilder() {
            return new ItemBuilder(datagenInstance, register, defaultTab);
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
        RegistryObject<Item> newborn = register.register(name, supplier);
        if (creativeModeTab != null) creativeModeTab.addItem(newborn);
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

    public ItemBuilder overrideCreativeTab(CreativeTabMutableRegistryEntry creativeModeTab) {
        this.creativeModeTab = creativeModeTab;
        return this;
    }

    public ItemBuilder noCreativeTab() {
        this.creativeModeTab = null;
        return this;
    }

    public boolean insufficientParams() {
        return name == null || supplier == null;
    }

    public void checkForInsufficientParams() {
        if (insufficientParams()) throw new IllegalStateException("Attempted to build a fluid with insufficient parameters");
    }
}
