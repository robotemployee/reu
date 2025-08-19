package com.robotemployee.reu.registry;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.registry.help.datagen.Datagen;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ModAdvancements {

    // DO NOT REMOVE. This is called to load this class into the JVM so that all of these funny static things are handled properly!
    public static void register() {
        LOGGER.info("Registering advancements!");
    }

    static Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation OBTAINED_BIRDBRAIN_DISC = createDiscAdvancement("obtained_birdbrain_disc", ModItems.MUSIC_DISC_BIRDBRAIN, Component.literal("DO NOT EAT"));
    //"If you're a real metalhead, then you'll love Custom Wood Burning" "Doodily ding dong tick tock \uD83D\uDD25☠\uD83D\uDD25. Doodily ding dong tick tock \uD83D\uDD25☠\uD83D\uDD25. Doodily ding dong tick tock \uD83D\uDD25☠\uD83D\uDD25. Doodily ding dong tick tock \uD83D\uDD25☠\uD83D\uDD25."
    public static final ResourceLocation OBTAINED_HATRED_DISC = createDiscAdvancement("obtained_hatred_disc", ModItems.MUSIC_DISC_HATRED, Component.literal("Doodily ding dong tick tock \uD83D\uDD25. Doodily ding dong tick tock \uD83D\uDD25☠\uD83D\uDD25. Doodily ding dong tick tock \uD83D\uDD25☠☠☠\uD83D\uDD25. Doodily ding dong tick tock \uD83D\uDD25☠☠☠☠☠\uD83D\uDD25."));
    public static final ResourceLocation OBTAINED_CALIFORNIA_DISC = createDiscAdvancement("obtained_california_disc", ModItems.MUSIC_DISC_CALIFORNIA, Component.literal("Let's channel our inner white girl together!!! :33 !!  HAAAAAAAAAAAH, HAAAAAAAAAAAAAAH, HAAAAAAAAAAAAAAAAAAAAA"));
    public static final ResourceLocation OBTAINED_TRIPLE_BAKA = createDiscAdvancement("obtained_triple_baka_disc", ModItems.MUSIC_DISC_TRIPLE_BAKA, Component.literal("DON'T STOP READING, DON'T STOP READING. please wake up please wake up please wake up please wake up please. please. i miss you please."));
    public static final ResourceLocation OBTAINED_CLAIRO_DISC = createDiscAdvancement("obtained_clairo_disc", ModItems.MUSIC_DISC_CLAIRO, Component.literal("har har harhar har har harhar harhar, har har harhar, har har harhar"));
    public static final ResourceLocation OBTAINED_GIANT_ROBOTS_DISC = createDiscAdvancement("obtained_giant_robots_disc", ModItems.MUSIC_DISC_GIANT_ROBOTS, Component.literal("I spent an hour and a half looking at a screenshot of Optimus Prime doing the default dance for this."));
    public static final ResourceLocation OBTAINED_STEEL_HAZE_DISC = createDiscAdvancement("obtained_mechanized_memories_disc", ModItems.MUSIC_DISC_STEEL_HAZE, Component.literal("We need more depression robot games! I demand it!"));
    public static final ResourceLocation OBTAINED_SO_BE_IT_DISC = createDiscAdvancement("obtained_so_be_it_disc", ModItems.MUSIC_DISC_SO_BE_IT, Component.literal("I would love to talk about the cinematography of the music video but that's a lot of words!"));
    public static final ResourceLocation OBTAINED_HEART_OF_GLASS_DISC = createDiscAdvancement("obtained_heart_of_glass_disc", ModItems.MUSIC_DISC_HEART_OF_GLASS, Component.literal("Well, this is odd. Writing a message for my own disc. Hi. Ironically, the hope is that you have killed yourself in minecraft"));
    public static final ResourceLocation OBTAINED_KOKOROTOLUNANOFUKAKAI_DISC = createDiscAdvancement("obtained_kokorotolunanofukakai_disc", ModItems.MUSIC_DISC_KOKOROTOLUNANOFUKAKAI, Component.literal("Chat, say it with me! Kokorotolunanofukakaikokorotolunanofukakaikokorotolunanofukakai ... Chat, am I muted? Alright, I'll say it again"));
    public static final ResourceLocation OBTAINED_ORANGE_BLOSSOMS_DISC = createDiscAdvancement("obtained_orange_blossoms_disc", ModItems.MUSIC_DISC_ORANGE_BLOSSOMS, Component.literal("WHY ARE ORANGE BLOSSOMS NOT ORANGE? Anyways, sorry for putting you through whatever draconian mechanic I'll make to obtain this disc"));
    public static final ResourceLocation OBTAINED_PROVIDENCE_DISC = createDiscAdvancement("obtained_providence_disc", ModItems.MUSIC_DISC_PROVIDENCE, Component.literal("Repent, then, and turn to God, so that your sins may be wiped out, that times of refreshing may come from the Lord. Or don't, I guess"));
    public static final ResourceLocation OBTAINED_I_WISH_DISC = createDiscAdvancement("obtained_i_wish_disc", ModItems.MUSIC_DISC_I_WISH, Component.literal("♫ I wish I was a little bit armored, I wish I wouldn't bother, I wish I had a world, that looked good, and a father; Wish I wasn't lagging out my ass with a shack and a shit poor logger"));

    // Granted via code when you get a rank of S or better in phillip's disc challenge
    public static final ResourceLocation VICTORY_ROYALE = Datagen.ModAdvancementProvider.simpleAdvancement("victory_royale", () -> Items.BOW, Component.literal("Fuck You in Particular"), Component.literal("Earn a rank of S from the challenge for Phillip's disc"), null);

    private static ResourceLocation lastDiscLoc = null;

    public static ResourceLocation createDiscAdvancement(String id, Supplier<Item> supplier, Component desc) {
        //LOGGER.info("Creating disc advancement with ID " + id);
        ResourceLocation newborn = Datagen.ModAdvancementProvider.simpleItemObtainedAdvancement(id, supplier, desc, lastDiscLoc);
        lastDiscLoc = newborn;
        return newborn;
    }

    @OnlyIn(Dist.CLIENT)
    public static Advancement getAdvancementClient(ResourceLocation loc) {
        ClientAdvancements manager = Minecraft.getInstance().getConnection().getAdvancements();
        Advancement output = manager.getAdvancements().get(loc);
        if (output == null) LOGGER.error("Attempted to get an advancement that doesn't exist: " + loc);

        return output;
    }

    public static Advancement getAdvancement(@NotNull ServerPlayer player, ResourceLocation loc) {
        ServerLevel level = (ServerLevel) player.level();
        MinecraftServer server = level.getServer();
        Advancement output = server.getAdvancements().getAdvancement(loc);
        if (output == null) LOGGER.error("Attempted to get an advancement that doesn't exist: " + loc);

        return output;
    }

    public static AdvancementProgress getAdvancementProgress(@NotNull ServerPlayer player, ResourceLocation loc) {
        Advancement advancement = getAdvancement(player, loc);
        return player.getAdvancements().getOrStartProgress(advancement);
    }

    public static void completeAdvancement(@NotNull ServerPlayer player, ResourceLocation loc) {
        //LOGGER.info("completing advancement " + loc + " for " + player.getName());
        Advancement advancement = getAdvancement(player, loc);
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);

        for (String criterion : progress.getRemainingCriteria()) {
            progress.grantProgress(criterion);
        }
    }

    public static boolean isAdvancementComplete(@NotNull ServerPlayer player, ResourceLocation loc) {
        //LOGGER.info("Querying whether " + loc.toString() + " is complete");
        return getAdvancementProgress(player, loc).isDone();
    }



}
