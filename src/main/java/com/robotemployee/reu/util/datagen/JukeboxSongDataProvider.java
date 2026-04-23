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
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

// based on advancement provider im just speedrunning a port
public class JukeboxSongDataProvider implements DataProvider {
    private final PackOutput.PathProvider pathProvider;
    private final List<JukeboxSongSubProvider> subProviders;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public JukeboxSongDataProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, List<JukeboxSongSubProvider> subProviders) {
        this.pathProvider = output.createRegistryElementsPathProvider(Registries.JUKEBOX_SONG);
        this.subProviders = subProviders;
        this.registries = registries;
    }

    @Override
    @NotNull
    public CompletableFuture<?> run(@NotNull CachedOutput output) {
        return this.registries.thenCompose((p_323115_) -> {
            Set<ResourceKey<JukeboxSong>> set = new HashSet<>();
            List<CompletableFuture<?>> list = new ArrayList<>();
            Consumer<Holder<JukeboxSong>> consumer = (songHolder) -> {
                if (!set.add(songHolder.getKey())) {
                    throw new IllegalStateException("Duplicate jukebox song " + songHolder.getKey());
                } else {
                    ResourceKey<JukeboxSong> resourceKey = songHolder.getKey();
                    if (resourceKey == null) throw new IllegalStateException("Jukebox song holder has no ResourceKey: " + songHolder);
                    Path path = this.pathProvider.json(resourceKey.location());
                    list.add(DataProvider.saveStable(output, p_323115_, JukeboxSong.CODEC, songHolder, path));
                }
            };

            for(JukeboxSongSubProvider jukeboxSongSubProvider : this.subProviders) {
                jukeboxSongSubProvider.generate(p_323115_, consumer);
            }

            return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    @NotNull
    public String getName() {
        return RobotEmployeeUtils.MODID + "_jukebox_song";
    }

    public static interface JukeboxSongSubProvider {
        void generate(HolderLookup.Provider provider, Consumer<Holder<JukeboxSong>> consumer);

        static AdvancementHolder createPlaceholder(String location) {
            return Advancement.Builder.advancement().build(ResourceLocation.parse(location));
        }
    }
}
