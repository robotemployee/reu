package com.robotemployee.reu.registry;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.registry.help.builder.SoundBuilder;
import com.robotemployee.reu.registry.help.datagen.Datagen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.SoundDefinition;
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
    public static final RegistryObject<SoundEvent> STEEL_HAZE_DISC = registerDiscSound("steel_haze");
    public static final RegistryObject<SoundEvent> SO_BE_IT_DISC = registerDiscSound("so_be_it");
    public static final RegistryObject<SoundEvent> HEART_OF_GLASS_DISC = registerDiscSound("heart_of_glass");
    public static final RegistryObject<SoundEvent> KOKOROTOLUNANOFUKAKAI_DISC = registerDiscSound("kokorotolunanofukakai");
    public static final RegistryObject<SoundEvent> ORANGE_BLOSSOMS_DISC = registerDiscSound("orange_blossoms");
    public static final RegistryObject<SoundEvent> PROVIDENCE_DISC = registerDiscSound("providence");
    public static final RegistryObject<SoundEvent> I_WISH_DISC = registerDiscSound("i_wish");

    public static final RegistryObject<SoundEvent> MEH_CHEERING = registerNormalSound("player.meh_cheering", "player/meh_cheering");
    public static final RegistryObject<SoundEvent> GOOD_CHEERING = registerNormalSound("player.good_cheering", "player/good_cheering");
    public static final RegistryObject<SoundEvent> EPIC_CHEERING = registerNormalSound("player.epic_cheering", "player/epic_cheering");


    public static final RegistryObject<SoundEvent> GREG_FLYING = registerNormalSound("entity.greg.fly", "banana/greg_flying");

    public static final RegistryObject<SoundEvent> ASTEIRTO_HUM = new SoundBuilder()
            .withName("entity.asteirto.idle")
            .soundLocation("banana/asteirto_hum")
            .soundModifier(sound -> sound.attenuationDistance(32))
            .build();

    public static RegistryObject<SoundEvent> registerDiscSound(String name) {
        return new SoundBuilder()
                .withName("music_disc." + name)
                .soundLocation("music_disc/" + name)
                .soundModifier(SoundDefinition.Sound::stream)
                .build();
    }

    public static RegistryObject<SoundEvent> registerNormalSound(String name, String location) {
        return new SoundBuilder()
                .soundLocation(location)
                .withName(name)
                .build();
    }

}
