package com.robotemployee.reu.util.registry.builder;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.util.datagen.DatagenInstance;
import com.robotemployee.reu.util.registry.entry.BlockRegistryEntry;
import com.robotemployee.reu.util.registry.entry.ItemRegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class BlockBuilder {

    Logger LOGGER = LogUtils.getLogger();
    private boolean hasItem = true;
    private boolean hasCreativeTab = true;
    private String name;
    private Supplier<? extends Block> supplier;
    private boolean doDatagen = true;
    private BiConsumer<DatagenInstance, Supplier<Block>> datagenConsumer = (datagen, blockSupplier) -> {
        datagen.modBlockModelProviderManager.cubeAll(blockSupplier);
    };

    private ItemBuilder itemBuilder;

    public static class Manager implements IManager {
        protected final DeferredRegister<Block> blockRegister;
        protected final ItemBuilder.Manager itemManager;
        protected final ModInformation modInformation;

        public Manager(ModInformation modInformation, ItemBuilder.Manager itemManager) {
            this.modInformation = modInformation;
            this.blockRegister = DeferredRegister.create(Registries.BLOCK, modInformation.getNamespace());
            this.itemManager = itemManager;
        }

        public BlockBuilder createBuilder() {
            return new BlockBuilder(this);
        }

        public DeferredRegister<Block> getRegister() {
            return blockRegister;
        }

        public ItemBuilder.Manager getItemManager() {
            return itemManager;
        }

        public ModInformation getModInformation() {
            return modInformation;
        }

        @Override
        public void register(IEventBus modEventBus) {
            blockRegister.register(modEventBus);
        }
    }

    private final Manager MANAGER;
    private BlockBuilder(Manager manager) {
        this.MANAGER = manager;
    }

    public BlockBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public BlockBuilder withSupplier(Supplier<? extends Block> supplier) {
        this.supplier = supplier;
        return this;
    }

    public BlockBuilder noDatagen() {
        this.doDatagen = false;
        return this;
    }

    public BlockBuilder withCustomDatagen(BiConsumer<DatagenInstance, Supplier<Block>> datagenConsumer) {
        if (!doDatagen) throw new IllegalStateException("Specified that datagen shouldn't be done, but afterwards attempted to designate custom datagen");
        this.datagenConsumer = datagenConsumer;
        return this;
    }

    // Build

    public BlockRegistryEntry build() {

        checkForInsufficientParams();
        Supplier<Block> block = MANAGER.getRegister().register(getName(), getSupplier());

        if (hasItem()) {
            itemBuilder = MANAGER.getItemManager().createBuilder();
            //RegistryObject<Item> item = ITEMS.register(getName(), () -> new BlockItem(block.get(), new Item.Properties()));
            if (!itemBuilder.hasName()) itemBuilder.withName(getName());
            if (!itemBuilder.hasSupplier()) itemBuilder.withSupplier(() -> new BlockItem(block.get(), new Item.Properties()));

            if (!hasCreativeTab) itemBuilder.noCreativeTab();

            ItemRegistryEntry item = itemBuilder.build();
            return new BlockRegistryEntry(block, item);
        }

        if (doDatagen) {
            datagenConsumer.accept(MANAGER.getModInformation().getDatagenInstance(), block);
        }

        return new BlockRegistryEntry(block);
    }

    // Optional things

    public BlockBuilder noCreativeTab() {
        hasCreativeTab = false;
        return this;
    }

    public BlockBuilder noItem() {
        hasItem = false;
        return this;
    }

    public BlockBuilder itemBuilder(Function<ItemBuilder, ItemBuilder> function) {
        this.itemBuilder = function.apply(itemBuilder);
        return this;
    }

    public void checkForInsufficientParams() {
        if (insufficientParams()) throw new IllegalStateException();
    }

    // Getters

    public boolean hasItem() {
        return hasItem;
    }

    public String getName() {
        return name;
    }

    public Supplier<? extends Block> getSupplier() {
        return supplier;
    }

    // Other

    public boolean insufficientParams() {
        return getName() == null || getSupplier() == null;
    }
}
