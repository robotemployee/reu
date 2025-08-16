package com.robotemployee.reu.banana;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class BananaRaidEvents {
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        BananaRaidSavedData bananaData = BananaRaidSavedData.onLevelLoaded(serverLevel);
    }
}
