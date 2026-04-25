package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.util.datagen.DatagenInstance;
import com.robotemployee.reu.util.registry.entry.SoundRegistryEntry;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SoundBuilder {

    private String name;
    private ResourceLocation location;
    private SoundDefinition definition;
    private Consumer<SoundDefinition.Sound> soundModifier;

    private boolean isFixedRange = false;
    private float range = 0;

    private BiFunction<Holder<SoundEvent>, ResourceLocation, JukeboxSong> jukeboxSongCreator;
    private String jukeboxSongName;

    public static class Manager {
        public final DatagenInstance datagenInstance;
        public final DeferredRegister<SoundEvent> register;
        public final String modid;
        public Manager(DatagenInstance datagenInstance, DeferredRegister<SoundEvent> register, String modid)  {
            this.datagenInstance = datagenInstance;
            this.register = register;
            this.modid = modid;
        }

        public SoundBuilder createBuilder() {
            return new SoundBuilder(this);
        }

        public DeferredRegister<SoundEvent> getSoundRegister() {
            return register;
        }

        public DatagenInstance getDatagenInstance() {
            return datagenInstance;
        }

        public String getModid() {
            return modid;
        }
    }

    private final Manager MANAGER;
    protected SoundBuilder(Manager manager) {
        this.MANAGER = manager;
    }

    public SoundBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public SoundBuilder soundLocation(String path) {
        this.location = ResourceLocation.fromNamespaceAndPath(MANAGER.getModid(), path);
        return this;
    }

    public SoundBuilder soundLocation(ResourceLocation location) {
        this.location = location;
        return this;
    }

    public SoundBuilder withDefinition(SoundDefinition definition) {
        this.definition = definition;
        return this;
    }

    public SoundBuilder soundModifier(Consumer<SoundDefinition.Sound> soundModifier) {
        if (this.definition != null) throw new IllegalStateException("Cannot specify both a SoundDefinition and sound modifiers. The modifiers are intended to be embedded into the default definition and will do nothing when the default definition is overridden.");
        this.soundModifier = soundModifier;
        return this;
    }

    public SoundBuilder fixedRange(float range) {
        this.isFixedRange = true;
        this.range = range;
        return this;
    }

    public SoundBuilder withJukeboxSong(String jukeboxSongName, int ticks) {
        this.jukeboxSongCreator = (newborn, loc) -> new JukeboxSong(
                newborn,
                Component.translatable("item." + loc.getNamespace() + "." + loc.getPath() + ".desc"),
                ticks / 60f,
                5
        );
        this.jukeboxSongName = jukeboxSongName;
        return this;
    }

    /**
     * @param jukeboxSongCreator gives you the newborn holder for the sound event and the resource location it's under, you provide a JukeboxSong from that
     */
    public SoundBuilder withJukeboxSong(String jukeboxSongName, BiFunction<Holder<SoundEvent>, ResourceLocation, JukeboxSong> jukeboxSongCreator) {
        this.jukeboxSongCreator = jukeboxSongCreator;
        this.jukeboxSongName = jukeboxSongName;
        return this;
    }

    public SoundRegistryEntry build() {
        checkForInsufficientParams();
        DeferredHolder<SoundEvent, SoundEvent> newborn;

        ResourceLocation newbornResourceLocation = ResourceLocation.fromNamespaceAndPath(MANAGER.getModid(), name);
        ResourceLocation jukeboxSongResourceLocation = newbornResourceLocation;
        if (isFixedRange) {
            newborn = MANAGER.getSoundRegister().register(
                    name,
                    () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(MANAGER.getModid(), name), range)
            );
        } else {
            newborn = MANAGER.getSoundRegister().register(
                    name,
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MANAGER.getModid(), name))
            );
        }

        ResourceKey<JukeboxSong> jukeboxSongKey;
        if (jukeboxSongCreator != null) {
            JukeboxSong song = jukeboxSongCreator.apply(newborn, jukeboxSongResourceLocation);
            jukeboxSongKey = MANAGER.getDatagenInstance().modJukeboxSongProviderManager.registerJukeboxSong(jukeboxSongResourceLocation, song);
        } else jukeboxSongKey = null;

        runDatagen(newborn);
        return new SoundRegistryEntry(newborn, jukeboxSongKey);
    }


    private void runDatagen(Supplier<SoundEvent> newborn) {
        if (definition == null) {
            SoundDefinition.Sound sound = SoundDefinition.Sound.sound(location, SoundDefinition.SoundType.SOUND);
            if (soundModifier != null) soundModifier.accept(sound);
            definition = SoundDefinition.definition().with(sound);
        }
        MANAGER.getDatagenInstance().modSoundProviderManager.register(newborn, definition);
    }

    private void checkForInsufficientParams() {
        if (name == null) throw new IllegalStateException("Sound name was not provided");
        if (location == null && definition == null) throw new IllegalStateException("Sound resource location was not provided. Needed if you aren't going to specify a sound definition, since it has to generate one for you");
    }

}
