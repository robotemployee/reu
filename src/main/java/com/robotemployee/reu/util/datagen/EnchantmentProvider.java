package com.robotemployee.reu.util.datagen;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EnchantmentProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;
    private final List<EnchantmentProvider.EnchantmentSubProvider> subProviders;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public EnchantmentProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, List<EnchantmentSubProvider> subProviders) {
        this.pathProvider = output.createRegistryElementsPathProvider(Registries.ENCHANTMENT);
        this.subProviders = subProviders;
        this.registries = registries;
    }

    @Override
    @NotNull
    public CompletableFuture<?> run(@NotNull CachedOutput output) {
        return this.registries.thenCompose((p_323115_) -> {
            Set<ResourceKey<Enchantment>> set = new HashSet<>();
            List<CompletableFuture<?>> list = new ArrayList<>();
            Consumer<Holder<Enchantment>> consumer = (enchantmentHolder) -> {
                if (!set.add(enchantmentHolder.getKey())) {
                    throw new IllegalStateException("Duplicate enchantment " + enchantmentHolder.getKey());
                } else {
                    ResourceKey<Enchantment> resourceKey = enchantmentHolder.getKey();
                    if (resourceKey == null) throw new IllegalStateException("Enchantment holder has no ResourceKey: " + enchantmentHolder);
                    Path path = this.pathProvider.json(resourceKey.location());
                    list.add(DataProvider.saveStable(output, p_323115_, Enchantment.CODEC, enchantmentHolder, path));
                }
            };

            for(EnchantmentSubProvider enchantmentSubProvider : this.subProviders) {
                enchantmentSubProvider.generate(p_323115_, consumer);
            }

            return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    @NotNull
    public String getName() {
        return RobotEmployeeUtils.MODID + "_enchantment";
    }

    public static interface EnchantmentSubProvider {
        void generate(HolderLookup.Provider provider, Consumer<Holder<Enchantment>> consumer);

        static AdvancementHolder createPlaceholder(String location) {
            return Advancement.Builder.advancement().build(ResourceLocation.parse(location));
        }
    }
}
