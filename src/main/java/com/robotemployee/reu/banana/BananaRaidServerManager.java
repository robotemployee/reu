package com.robotemployee.reu.banana;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public class BananaRaidServerManager {

    private static final HashMap<ResourceKey<Level>, BananaRaidLevelManager> MANAGERS = new HashMap<>();

    public static BananaRaidLevelManager getManager(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level.dimension(), k -> BananaRaidLevelManager.onLevelLoaded(level));
    }

    public static void removeManager(ServerLevel level) {
        // this doesn't handle saving to the level because SavedData and SavedData.setDirty() does that already
        // all it does is remove the instantiated BananaRaidLevelManager from memory
        MANAGERS.remove(level.dimension());
    }
}
