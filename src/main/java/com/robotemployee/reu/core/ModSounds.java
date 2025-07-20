package com.robotemployee.reu.core;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

public class ModSounds {

    static Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RobotEmployeeUtils.MODID);

    public static final RegistryObject<SoundEvent> BIRDBRAIN_DISC = registerDiscSound("birdbrain");

    public static final RegistryObject<SoundEvent> HATRED_JACKULATION_DISC = registerDiscSound("hatred_jackulation");

    public static final RegistryObject<SoundEvent> CALIFORNIA_GIRLS_DISC = registerDiscSound("california_girls");

    public static final RegistryObject<SoundEvent> TRIPLE_BAKA_DISC = registerDiscSound("triple_baka");

    public static final RegistryObject<SoundEvent> CLAIRO_DISC = registerDiscSound("clairo");

    public static final RegistryObject<SoundEvent> GIANT_ROBOTS_DISC = registerDiscSound("giant_robots");

    public static final RegistryObject<SoundEvent> MEMORIES_DISC = registerDiscSound("armored_core_20th");

    public static final RegistryObject<SoundEvent> SO_BE_IT_DISC = registerDiscSound("so_be_it");

    public static final RegistryObject<SoundEvent> HEART_OF_GLASS_DISC = registerDiscSound("heart_of_glass");

    public static RegistryObject<SoundEvent> registerDiscSound(String location) {

        String finalLocation = "music_disc." + location;

        LOGGER.info(String.format("Registering sound %s", location));
        return SOUNDS.register(
                location,
                () -> SoundEvent.createVariableRangeEvent(
                        new ResourceLocation(RobotEmployeeUtils.MODID, finalLocation)
                )
        );
    }
}
