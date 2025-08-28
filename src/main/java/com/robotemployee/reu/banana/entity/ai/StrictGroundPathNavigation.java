package com.robotemployee.reu.banana.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/**
 * <p>Fails when it would have to pass through air, lava, etc to reach; Must be able to walk continuously, on completely solid ground to reach destination</p>
 * <p>Intended use is to have a less tolerant PathNavigation for GregEntity's ground mode so it WILL fly when it can't reach.</p>
 * */
public class StrictGroundPathNavigation extends GroundPathNavigation {
    public StrictGroundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected boolean hasValidPathType(BlockPathTypes blockPathTypes) {
        return switch (blockPathTypes) {
            case TRAPDOOR,
                    FENCE,
                    POWDER_SNOW,
                    LAVA,
                    DANGER_POWDER_SNOW,
                    DANGER_FIRE,
                    DAMAGE_OTHER,
                    DAMAGE_CAUTIOUS
                    ->
                    false;
            default -> super.hasValidPathType(blockPathTypes);
        };
    }
}
