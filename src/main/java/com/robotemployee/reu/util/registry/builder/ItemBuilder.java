package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.capability.IHasCapability;
import com.robotemployee.reu.util.registry.entry.CreativeTabMutableRegistryEntry;
import com.robotemployee.reu.util.datagen.DatagenInstance;
import com.robotemployee.reu.util.registry.entry.ItemRegistryEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ItemBuilder {

    private String name;
    private Supplier<Item> supplier;
    private final ArrayList<Supplier<TagKey<Item>>> tags = new ArrayList<>();
    private BiConsumer<DatagenInstance, Supplier<Item>> datagenConsumer;
    private final Manager MANAGER;
    private CreativeTabMutableRegistryEntry creativeTab;

    private boolean doDatagen = true;

    protected ItemBuilder(Manager manager) {
        datagenConsumer = DatagenInstance::basicItem;
        this.MANAGER = manager;
        this.creativeTab = manager.getDefaultTab();
    }

    @EventBusSubscriber
    public static class Manager implements IManager {
        private final ModInformation modInformation;
        private final DeferredRegister<Item> register;
        private final List<Supplier<Item>> registeredItems = new ArrayList<>();
        private SoundBuilder.Manager soundManager;

        @Nullable
        public CreativeTabMutableRegistryEntry defaultTab;
        public Manager(ModInformation modInformation) {
            this.modInformation = modInformation;
            this.register = DeferredRegister.create(Registries.ITEM, modInformation.getNamespace());
            Manager.MANAGERS.add(this);
        }

        public ModInformation getModInformation() {
            return modInformation;
        }

        public DeferredRegister<Item> getRegister() {
            return register;
        }

        public CreativeTabMutableRegistryEntry getDefaultTab() {
            return defaultTab;
        }

        public Manager defaultCreativeTab(CreativeTabMutableRegistryEntry defaultTab) {
            this.defaultTab = defaultTab;
            return this;
        }

        public Manager withSoundManager(SoundBuilder.Manager soundManager) {
            this.soundManager = soundManager;
            return this;
        }

        public SoundBuilder.Manager getSoundManager() {
            return soundManager;
        }

        public void noteRegisteredItem(Supplier<Item> supplier) {
            registeredItems.add(supplier);
        }

        /**
         * call this when registering capabilities
         * @param event the RegisterCapabilities event to use
         */
        public void registerMyCapabilities(RegisterCapabilitiesEvent event) {
            registeredItems.forEach(req -> {
                if (req.get() instanceof IHasCapability<?> capabilityHaver) {
                    capabilityHaver.onRegisteringCapabilities(event);
                }
            });
        }

        // i could do something similar to what i did in, i think EntityBuilder? where there's a queue in reu
        // buuuuutttttttt meh i can figure it out later. encapsulation and whatnot
        // probably a better solution
        // scratch that definitely a better solution
        // and actually i think that queue was intended to be in your thing

        public ItemBuilder createBuilder() {
            return new ItemBuilder(this);
        }

        public static final List<Manager> MANAGERS = new ArrayList<>();

        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            MANAGERS.forEach(manager -> manager.registerMyCapabilities(event));
        }

        @Override
        public void register(IEventBus modEventBus) {
            register.register(modEventBus);
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

    public ItemRegistryEntry build() {
        checkForInsufficientParams();
        DeferredHolder<Item, Item> newborn = MANAGER.getRegister().register(name, supplier);
        if (creativeTab != null) creativeTab.addItem(newborn::value);
        if (doDatagen) datagenConsumer.accept(MANAGER.getModInformation().getDatagenInstance(), newborn::value);
        for (Supplier<TagKey<Item>> tag : tags) {
            MANAGER.getModInformation().getDatagenInstance().addTagToItem(newborn::value, tag);
        }
        return new ItemRegistryEntry(newborn);
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
    public ItemBuilder customDatagen(BiConsumer<DatagenInstance, Supplier<Item>> datagen) {
        if (!doDatagen) throw new IllegalStateException("Added custom datagen after specifying to not do datagen");
        this.datagenConsumer = datagen;
        return this;
    }

    public ItemBuilder noDatagen() {
        this.doDatagen = false;
        return this;
    }

    public ItemBuilder overrideCreativeTab(CreativeTabMutableRegistryEntry creativeModeTab) {
        this.creativeTab = creativeModeTab;
        return this;
    }

    public ItemBuilder noCreativeTab() {
        this.creativeTab = null;
        return this;
    }

    public boolean insufficientParams() {
        return name == null || supplier == null;
    }

    public void checkForInsufficientParams() {
        if (insufficientParams()) throw new IllegalStateException("Attempted to build a fluid with insufficient parameters");
    }
}
