package com.robotemployee.reu.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(PathNavigation.class)
public interface PathNavigationAccessor {

    @Accessor("pathFinder")
    PathFinder getPathFinder();

    @Accessor("speedModifier")
    double getSpeedModifier();

    @Invoker("createPathFinder")
    PathFinder createPathFinder(int idk);

    @Invoker("getTempMobPos")
    Vec3 getTempMobPos();

    @Invoker("canUpdatePath")
    boolean canUpdatePath();

    @Invoker("createPath")
    Path createPath(Set<BlockPos> p_148223_, int p_148224_, boolean p_148225_, int p_148226_, float p_148227_);
}
