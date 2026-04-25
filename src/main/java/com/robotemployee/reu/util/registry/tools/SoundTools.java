package com.robotemployee.reu.util.registry.tools;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.registry.builder.SoundBuilder;
import com.robotemployee.reu.util.registry.entry.SoundRegistryEntry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.common.data.SoundDefinition;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class SoundTools {

    static Logger LOGGER = LogUtils.getLogger();

    public static SoundRegistryEntry registerDiscSound(String name, SoundBuilder.Manager manager, int ticks) {
        return registerDiscSound(name, manager, ticks, name);
    }

    /**
     * see i can write good looking javadoc
     * @param name the resulting name of the sound in the registry. used for lang as well.
     * @param manager the {@link SoundBuilder.Manager} to use.
     * @param ticks the ticks the song will last for.
     * @param path the path to the sound file asset. the point of this being separate is so that you can have a sound which doesn't have the same name as its file. this is useful for secret sounds maybe whatever look i wanted to do it
     * @return the resulting sound
     * @implNote okay maybe i can't write good javadoc
     */
    public static SoundRegistryEntry registerDiscSound(String name, SoundBuilder.Manager manager, int ticks, String path) {
        return manager.createBuilder()
                .withName("music_disc." + name)
                .soundLocation("music_disc/" + path)
                .soundModifier(SoundDefinition.Sound::stream)
                .withJukeboxSong(name, ticks)
                .build();
    }

    public static SoundRegistryEntry registerNormalSound(String name, String location, SoundBuilder.Manager manager) {
        return manager.createBuilder()
                .soundLocation(location)
                .withName(name)
                .build();
    }

}
