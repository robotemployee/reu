package com.robotemployee.reu.compat;

import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Objects;

public class FriendsAndFoesCompat {

    public static final String GLARE = "friendsandfoes:glare";
    public static final String OTHER_GLARE = "caverns_and_chasms:glare";

    @SubscribeEvent
    public static void onSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (Objects.equals(event.getEntity().getEncodeId(), GLARE)) event.setCanceled(true);
    }
}
