package com.robotemployee.reu.util.registry.entry;

import net.minecraft.core.Holder;
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
}
