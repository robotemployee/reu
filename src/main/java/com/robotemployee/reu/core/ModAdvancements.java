package com.robotemployee.reu.core;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.registry_help.datagen.Datagen;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ModAdvancements {

    // DO NOT REMOVE. this is called in Datagen during GatherDataEvent and loads this class into the JVM so that all of these funny static things are handled properly!
    public static void register() {
        LOGGER.info("Registering advancements!");
    }

    static Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation OBTAINED_BIRDBRAIN_DISC = createDiscAdvancement("obtained_birdbrain_disc", ModItems.MUSIC_DISC_BIRDBRAIN, Component.literal("DO NOT EAT"));
    public static final ResourceLocation OBTAINED_HATRED_DISC = createDiscAdvancement("obtained_hatred_disc", ModItems.MUSIC_DISC_HATRED, Component.literal("If you're a real metalhead, then you'll love Custom Wood Burning"));
    public static final ResourceLocation OBTAINED_CALIFORNIA_DISC = createDiscAdvancement("obtained_california_disc", ModItems.MUSIC_DISC_CALIFORNIA, Component.literal("Let's channel our inner white girl together!!! :33 !!  HAAAAAAAAAAAH, HAAAAAAAAAAAAAAH, HAAAAAAAAAAAAAAAAAAAAA"));
    public static final ResourceLocation OBTAINED_TRIPLE_BAKA = createDiscAdvancement("obtained_triple_baka_disc", ModItems.MUSIC_DISC_TRIPLE_BAKA, Component.literal("DON'T STOP READING, DON'T STOP READING. please wake up please wake up please wake up please wake up please. please. i miss you please."));
    public static final ResourceLocation OBTAINED_CLAIRO_DISC = createDiscAdvancement("obtained_clairo_disc", ModItems.MUSIC_DISC_CLAIRO, Component.literal("har har harhar har har harhar harhar, har har harhar, har har harhar"));
    public static final ResourceLocation OBTAINED_GIANT_ROBOTS_DISC = createDiscAdvancement("obtained_giant_robots_disc", ModItems.MUSIC_DISC_GIANT_ROBOTS, Component.literal("I spent an hour and a half looking at a screenshot of Optimus Prime doing the default dance for this."));
    public static final ResourceLocation OBTAINED_MEMORIES_DISC = createDiscAdvancement("obtained_memories_disc", ModItems.MUSIC_DISC_MEMORIES, Component.literal("We need more depression robot games! I demand it!"));
    public static final ResourceLocation OBTAINED_SO_BE_IT_DISC = createDiscAdvancement("obtained_so_be_it_disc", ModItems.MUSIC_DISC_SO_BE_IT, Component.literal("I would love to talk about the cinematography of the music video but that's a lot of words!"));
    public static final ResourceLocation OBTAINED_HEART_OF_GLASS_DISC = createDiscAdvancement("obtained_heart_of_glass_disc", ModItems.MUSIC_DISC_HEART_OF_GLASS, Component.literal("Well, this is odd. Writing a message for my own disc. Hi. Ironically, the hope is that you have killed yourself in minecraft"));

    private static ResourceLocation lastDiscLoc = null;

    public static ResourceLocation createDiscAdvancement(String id, Supplier<Item> supplier, Component desc) {
        //LOGGER.info("Creating disc advancement with ID " + id);
        ResourceLocation newborn = (lastDiscLoc == null) ?
                Datagen.ModAdvancementProvider.simpleItemObtainedAdvancement(id, supplier, desc) :
                Datagen.ModAdvancementProvider.simpleItemObtainedAdvancement(id, supplier, desc, lastDiscLoc);
        lastDiscLoc = newborn;
        return newborn;
    }

    public static AdvancementProgress getAdvancementProgress(@NotNull ServerLevel level, @NotNull ServerPlayer player, ResourceLocation advancementLoc) {
        Advancement advancement = level.getServer().getAdvancements().getAdvancement(advancementLoc);
        assert advancement != null;
        return player.getAdvancements().getOrStartProgress(advancement);
    }


}
