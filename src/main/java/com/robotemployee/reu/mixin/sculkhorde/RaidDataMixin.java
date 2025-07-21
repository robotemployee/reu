package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.core.ModBlocks;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.systems.raid_system.RaidData;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.BlockSearcher;
import com.robotemployee.reu.extra.SculkHordeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.function.Predicate;

@Mixin(RaidData.class)
public class RaidDataMixin {

    @Shadow(remap = SculkHordeCompat.remapNormalSculkHorde)
    protected static int MAXIMUM_RAID_RADIUS;

    // changed things are marked with CHANGED
    // i am altering the fields that hold the target predicates for the block searcher
    // so they believe that out of bounds blocks are a no-go
    @Inject(method = "<init>()V", at = @At("TAIL"), remap = SculkHordeCompat.remapNormalSculkHorde)
    void RaidData(CallbackInfo ci) {
        RaidData self = (RaidData)(Object)this;
        try {
            Field isTargetPredicate = RaidData.class.getDeclaredField("isTargetInvestigateLocationState");
            isTargetPredicate.setAccessible(true);
            Predicate<BlockPos> target = (blockPos) -> {
                BlockSearcher blockSearcher = self.getBlockSearcher().get();
                boolean isTarget = self.getDimension().getBlockState(blockPos).is(ModBlocks.BlockTags.SCULK_RAID_TARGET_HIGH_PRIORITY) || self.getDimension().getBlockState(blockPos).is(ModBlocks.BlockTags.SCULK_RAID_TARGET_LOW_PRIORITY) || self.getDimension().getBlockState(blockPos).is(ModBlocks.BlockTags.SCULK_RAID_TARGET_MEDIUM_PRIORITY);
                // CHANGED
                isTarget = isTarget && !SculkHordeCompat.isOutOfBounds(blockPos, self.getDimension());
                if (isTarget && BlockAlgorithms.getBlockDistance(self.getAreaOfInterestEntry().getPosition(), blockPos) > (float)self.getCurrentRaidRadius()) {
                    self.setCurrentRaidRadius((int)BlockAlgorithms.getBlockDistance(self.getRaidLocation(), blockPos));
                    SculkHorde.LOGGER.debug("Raid Radius is now " + self.getCurrentRaidRadius() + " blocks.");
                }
                return (!isTarget || blockSearcher.foundTargets.isEmpty() || !blockSearcher.isAnyTargetCloserThan(blockPos, 5)) && isTarget;
            };
            isTargetPredicate.set(this, target);

            Field isObstructedPredicate = RaidData.class.getDeclaredField("isObstructedInvestigateLocationState");
            isObstructedPredicate.setAccessible(true);
            Predicate<BlockPos> obstructed = (blockPos) -> {
                BlockSearcher blockSearcher = self.getBlockSearcher().get();
                if (blockSearcher.foundTargets.isEmpty() && BlockAlgorithms.getBlockDistance(self.getAreaOfInterestEntry().getPosition(), blockPos) > (float)MAXIMUM_RAID_RADIUS) {
                    return true;
                    // CHANGED
                } else if (SculkHordeCompat.isOutOfBounds(blockPos, self.getDimension())) {
                    return true;
                } else if (self.getDimension().getBlockState(blockPos).is(Blocks.AIR)) {
                    return true;
                } else if (!blockSearcher.foundTargets.isEmpty() && !blockSearcher.isAnyTargetCloserThan(blockPos, 25)) {
                    return true;
                } else {
                    return !BlockAlgorithms.isExposedToAir(self.getDimension(), blockPos);
                }
            };
            isObstructedPredicate.set(this, obstructed);

            Field isSpawnTarget = RaidData.class.getDeclaredField("isSpawnTarget");
            isSpawnTarget.setAccessible(true);

            Predicate<BlockPos> spawnTarget = (blockPos) -> {
                // CHANGED
                return !SculkHordeCompat.isOutOfBounds(blockPos, self.getDimension()) && (double)BlockAlgorithms.getBlockDistance(blockPos, self.getRaidLocation()) > (double)self.getCurrentRaidRadius() * 0.75 && BlockAlgorithms.isAreaFlat(self.getDimension(), blockPos, 2);
            };
            isSpawnTarget.set(this, spawnTarget);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
