package com.robotemployee.reu.foliant;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.HashMap;

@Mod.EventBusSubscriber(modid = RobotEmployeeUtils.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FoliantRaidServerManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final HashMap<ResourceKey<Level>, FoliantRaidLevelManager> MANAGERS = new HashMap<>();

    public static FoliantRaidLevelManager getLevelManager(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level.dimension(), k -> FoliantRaidLevelManager.onLevelLoaded(level));
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;
        getLevelManager(serverLevel).tick(serverLevel);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MANAGERS.clear();
    }

    public static void removeManager(ServerLevel level) {
        // this doesn't handle saving to the level because SavedData and SavedData.setDirty() does that already
        // all it does is remove the instantiated BananaRaidLevelManager from memory
        MANAGERS.remove(level.dimension());
    }

    public static void stopAll() {
        for (FoliantRaidLevelManager levelManager : MANAGERS.values()) {
            levelManager.stopAll();
        }
    }
}
