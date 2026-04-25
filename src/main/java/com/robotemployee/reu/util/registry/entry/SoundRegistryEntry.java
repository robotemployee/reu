package com.robotemployee.reu.util.registry.entry;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;


public record SoundRegistryEntry(DeferredHolder<SoundEvent, SoundEvent> holder, @Nullable ResourceKey<JukeboxSong> jukeboxSong) implements Supplier<SoundEvent> {
    @Override
    public SoundEvent get() {
        return holder().get();
    }

    public boolean isJukeboxSong() {
        return jukeboxSong() != null;
    }

    @Nullable
    public JukeboxSong getJukeboxSongDirect(HolderLookup.Provider registries) {
        if (jukeboxSong() == null) return null;
        return registries.lookupOrThrow(Registries.JUKEBOX_SONG).getOrThrow(jukeboxSong()).value();
    }
}
