package com.robotemployee.reu.extra;

import com.teamabnormals.upgrade_aquatic.core.registry.UAEntityTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AlexsCavesCompat {

    @SubscribeEvent
    public static void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        EntityType<?> type = event.getEntityType();
        ServerLevelAccessor levelAccessor = event.getLevel();
        ResourceKey<Biome> biomeKey = levelAccessor.getBiome(event.getPos()).unwrapKey().orElse(null);
        assert biomeKey != null;
        String biomeMod = biomeKey.location().getNamespace();

        if ((type == UAEntityTypes.THRASHER.get() || type == UAEntityTypes.GREAT_THRASHER.get()) && biomeMod.equals("alexscaves")) {
            event.setResult(Event.Result.DENY);
        }
    }
}
