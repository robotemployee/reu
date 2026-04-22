package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.datagen.DatagenInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SoundBuilder {

    private String name;
    private ResourceLocation location;
    private SoundDefinition definition;
    private Consumer<SoundDefinition.Sound> soundModifier;

    private Mode mode = Mode.NORMAL;
    private boolean isFixedRange = false;
    private float range = 0;

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
            return new SoundBuilder(datagenInstance, register, modid);
        }
    }

    private final DatagenInstance datagenInstance;
    private final DeferredRegister<SoundEvent> register;
    private final String modid;
    protected SoundBuilder(DatagenInstance datagenInstance, DeferredRegister<SoundEvent> register, String modid) {
        this.datagenInstance = datagenInstance;
        this.register = register;
        this.modid = modid;
    }

    public SoundBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public SoundBuilder soundLocation(String path) {
        this.location = ResourceLocation.fromNamespaceAndPath(modid, path);
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

    public SoundBuilder withMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    public Supplier<SoundEvent> build() {
        checkForInsufficientParams();
        Supplier<SoundEvent> newborn;

        if (isFixedRange) {
            newborn = register.register(
                    name,
                    () -> SoundEvent.createFixedRangeEvent(ResourceLocation.fromNamespaceAndPath(modid, name), range)
            );
        } else {
            newborn = register.register(
                    name,
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(modid, name))
            );
        }

        runDatagen(newborn);

        return newborn;
    }


    private void runDatagen(Supplier<SoundEvent> newborn) {
        if (definition == null) {
            SoundDefinition.Sound sound = SoundDefinition.Sound.sound(location, SoundDefinition.SoundType.SOUND);
            if (soundModifier != null) soundModifier.accept(sound);
            definition = SoundDefinition.definition().with(sound);
        }
        datagenInstance.modSoundProviderManager.register(newborn, definition);
    }

    private void checkForInsufficientParams() {
        if (name == null) throw new IllegalStateException("Sound name was not provided");
        if (location == null && definition == null) throw new IllegalStateException("Sound resource location was not provided. Needed if you aren't going to specify a sound definition, since it has to generate one for you");
    }


    public enum Mode {
        NORMAL(false),
        DISC(true);

        final boolean stream;
        Mode(boolean stream) {

            this.stream = stream;
        }

    }
}
