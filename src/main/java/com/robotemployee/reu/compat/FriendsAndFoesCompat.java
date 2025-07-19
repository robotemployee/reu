package com.robotemployee.reu.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class FriendsAndFoesCompat {

    //public static final String GLARE = "friendsandfoes:glare";
    public static final String GLARE = "glare";
    //public static final String OTHER_GLARE = "caverns_and_chasms:glare";

    @SubscribeEvent
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        ResourceLocation loc = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntityType());
        assert loc != null;
        String name = loc.getPath();
        // death.
        if (name.equals(GLARE)) event.setResult(Event.Result.DENY);
    }
}
