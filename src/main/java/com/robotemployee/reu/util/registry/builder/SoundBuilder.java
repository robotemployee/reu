package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.registry.ModSounds;
import com.robotemployee.reu.util.datagen.Datagen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

public class SoundBuilder {

    private String name;
    private ResourceLocation location;
    private SoundDefinition definition;
    private Consumer<SoundDefinition.Sound> soundModifier;

    private Mode mode = Mode.NORMAL;
    private boolean isFixedRange = false;
    private float range = 0;

    public SoundBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public SoundBuilder soundLocation(ResourceLocation location) {
        this.location = location;
        return this;
    }

    public SoundBuilder soundLocation(String location) {
        return soundLocation(new ResourceLocation(RobotEmployeeUtils.MODID, location));
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

    public RegistryObject<SoundEvent> build() {
        checkForInsufficientParams();
        RegistryObject<SoundEvent> newborn;

        if (isFixedRange) {
            newborn = ModSounds.SOUNDS.register(
                    name,
                    () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(RobotEmployeeUtils.MODID, name), range)
            );
        } else {
            newborn = ModSounds.SOUNDS.register(
                    name,
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RobotEmployeeUtils.MODID, name))
            );
        }

        runDatagen(newborn);

        return newborn;
    }


    private void runDatagen(RegistryObject<SoundEvent> newborn) {
        if (definition == null) {
            SoundDefinition.Sound sound = SoundDefinition.Sound.sound(location, SoundDefinition.SoundType.SOUND);
            if (soundModifier != null) soundModifier.accept(sound);
            definition = SoundDefinition.definition().with(sound);
        }
        Datagen.ModSoundProvider.register(newborn, definition);
    }

    private void checkForInsufficientParams() {
        if (name == null) throw new IllegalStateException("Sound name was not provided");
        if (location == null) throw new IllegalStateException("Sound resource location was not provided");
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
