package com.robotemployee.reu.registry;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.datagen.DatagenInstance;
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

    private static ResourceLocation lastDiscLoc = null;

    public static ResourceLocation createDiscAdvancement(DatagenInstance datagenInstance, String id, Supplier<Item> supplier, Component desc) {
        //LOGGER.info("Creating disc advancement with ID " + id);
        ResourceLocation newborn = datagenInstance.modAdvancementProviderManager.simpleItemObtainedAdvancement(new ResourceLocation(RobotEmployeeUtils.MODID, id), supplier, desc, lastDiscLoc);
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
