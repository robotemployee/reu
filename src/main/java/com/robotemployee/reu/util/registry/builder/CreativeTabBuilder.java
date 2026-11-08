package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.util.registry.entry.CreativeTabMutableRegistryEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class CreativeTabBuilder {

    private String name;
    private Supplier<ItemStack> iconSupplier;
    private Component title;

    public static class Manager implements IManager {
        protected final DeferredRegister<CreativeModeTab> register;
        protected final ModInformation modInformation;

        public Manager(ModInformation modInformation) {
            this.register = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, modInformation.getNamespace());
            this.modInformation = modInformation;
        }

        public CreativeTabBuilder createBuilder() {
            return new CreativeTabBuilder(register, modInformation.getNamespace());
        }

        @Override
        public void register(IEventBus modEventBus) {
            register.register(modEventBus);
        }
    }

    public final DeferredRegister<CreativeModeTab> register;
    public final String modid;
    private CreativeTabBuilder(DeferredRegister<CreativeModeTab> register, String modid) {
        this.register = register;
        this.modid = modid;
    }

    public CreativeTabBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public CreativeTabBuilder withIcon(Supplier<ItemStack> iconSupplier) {
        this.iconSupplier = iconSupplier;
        return this;
    }

    public CreativeTabBuilder withTitle(Component title) {
        this.title = title;
        return this;
    }

    public CreativeTabMutableRegistryEntry build() {
        return new CreativeTabMutableRegistryEntry(name, iconSupplier, title, register);
    }
}
