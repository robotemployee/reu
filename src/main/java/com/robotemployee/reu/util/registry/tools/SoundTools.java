package com.robotemployee.reu.util.registry.tools;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.registry.builder.SoundBuilder;
import com.robotemployee.reu.util.registry.entry.SoundRegistryEntry;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.common.data.SoundDefinition;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class SoundTools {

    static Logger LOGGER = LogUtils.getLogger();

    public static SoundRegistryEntry registerDiscSound(String name, SoundBuilder.Manager manager, int ticks) {
        return manager.createBuilder()
                .withName("music_disc." + name)
                .soundLocation("music_disc/" + name)
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
