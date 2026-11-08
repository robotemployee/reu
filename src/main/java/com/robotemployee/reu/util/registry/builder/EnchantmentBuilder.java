package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.util.datagen.DatagenInstance;
import com.robotemployee.reu.util.registry.entry.EnchantmentRegistryEntry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class EnchantmentBuilder {

    public final Manager MANAGER;
    public Supplier<Enchantment.Builder> enchantment;
    public String name;

    protected EnchantmentBuilder(Manager manager) {
        this.MANAGER = manager;
    }

    public static class Manager implements IManager {
        protected final ModInformation modInformation;
        public Manager(ModInformation modInformation) {
            this.modInformation = modInformation;
        }

        public DatagenInstance getDatagenInstance() {
            return modInformation.getDatagenInstance();
        }

        public EnchantmentBuilder createBuilder() {
            return new EnchantmentBuilder(this);
        }

        @Override
        public void register(IEventBus modEventBus) {

        }
    }

    public EnchantmentBuilder withEnchantment(Supplier<Enchantment.Builder> enchantment) {
        this.enchantment = enchantment;
        return this;
    }

    public EnchantmentBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public EnchantmentRegistryEntry build() {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(MANAGER.getDatagenInstance().MODID, name);

        return new EnchantmentRegistryEntry(MANAGER.getDatagenInstance().modEnchantmentProviderManager.justPutDownTheSillyLittleThing(loc, () -> enchantment.get().build(loc)));
    }
}
