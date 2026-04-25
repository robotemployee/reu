package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.util.datagen.DatagenInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EnchantmentBuilder {

    public final Manager MANAGER;
    public Enchantment.EnchantmentDefinition definition;
    public String name;

    protected EnchantmentBuilder(Manager manager) {
        this.MANAGER = manager;
    }

    public static class Manager {
        protected final DatagenInstance datagenInstance;
        protected final DeferredRegister<Enchantment> register;
        public Manager(DatagenInstance datagenInstance, DeferredRegister<Enchantment> register) {
            this.datagenInstance = datagenInstance;
            this.register = register;
        }

        public DeferredRegister<Enchantment> getRegister() {
            return register;
        }

        public DatagenInstance getDatagenInstance() {
            return datagenInstance;
        }

        public EnchantmentBuilder createBuilder() {
            return new EnchantmentBuilder(this);
        }
    }

    public EnchantmentBuilder withDefinition(Enchantment.EnchantmentDefinition definition) {
        this.definition = definition;
        return this;
    }

    public EnchantmentBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public Supplier<Enchantment> build() {
        DeferredHolder<Enchantment, Enchantment> newborn = MANAGER.getRegister().register(name, () ->
                Enchantment
                        .enchantment(definition)
                        .build(ResourceLocation.fromNamespaceAndPath(MANAGER.getRegister().getNamespace(), name))
        );

        MANAGER.getDatagenInstance().modEnchantmentProviderManager.justPutDownTheSillyLittleThing(newborn);

        return newborn;
    }
}
