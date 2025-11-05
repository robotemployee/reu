package com.robotemployee.reu.registry;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.registry.builder.SoundBuilder;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

public class ModSounds {

    static Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RobotEmployeeUtils.MODID);

    public static RegistryObject<SoundEvent> registerDiscSound(String name, SoundBuilder.Manager manager) {
        return manager.createBuilder()
                .withName("music_disc." + name)
                .soundLocation("music_disc/" + name)
                .soundModifier(SoundDefinition.Sound::stream)
                .build();
    }

    public static RegistryObject<SoundEvent> registerNormalSound(String name, String location, SoundBuilder.Manager manager) {
        return manager.createBuilder()
                .soundLocation(location)
                .withName(name)
                .build();
    }

}
