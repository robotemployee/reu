package com.robotemployee.reu.banana.entity.extra;

import com.robotemployee.reu.mixin.base.PathNavigationAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class MultiPathNavigation<T> extends PathNavigation {

    protected final Map<T, PathNavigation> navigators;
    protected T navKey;
    public MultiPathNavigation(Mob mob, Level level, Map<T, PathNavigation> navigators) {
        super(mob, level);
        this.navigators = navigators;
    }

    public T getNavKey() {
        return navKey;
    }

    /**<p>This switches the navigation without moving over the target position or etc</p><p>Intended for cases where you want to repopulate that information manually</p> */
    public void setNavigationQuietly(T key) {
        getCurrentNavigation().stop();
        navKey = key;
    }

    /** <p>This switches the navigation and asks the navigation you are switching to to move to the previous target position.</p>*/
    public void setNavigationLoudly(T key) {
        PathNavigation current = getCurrentNavigation();
        PathNavigation newcomer = getNavigation(key);

        current.stop();

        if (current.isInProgress()) {
            BlockPos targetPosition = current.getTargetPos();
            double speedMod = ((PathNavigationAccessor) current).getSpeedModifier();

            newcomer.moveTo(targetPosition.getX(), targetPosition.getY(), targetPosition.getZ(), speedMod);
        }
        navKey = key;
    }

    public PathNavigation getCurrentNavigation() {
        return navigators.get(getNavKey());
    }

    public PathNavigation getNavigation(T key) {
        return navigators.get(key);
    }

    @Override
    public void resetMaxVisitedNodesMultiplier() {
        getCurrentNavigation().resetMaxVisitedNodesMultiplier();
    }

    @Override
    public void setMaxVisitedNodesMultiplier(float mult) {
        getCurrentNavigation().setMaxVisitedNodesMultiplier(mult);
    }

    @Nullable
    @Override
    public BlockPos getTargetPos() {
        return getCurrentNavigation().getTargetPos();
    }

    @Override
    public void setSpeedModifier(double speedMod) {
        getCurrentNavigation().setSpeedModifier(speedMod);
    }

    @Override
    public void recomputePath() {
        getCurrentNavigation().recomputePath();
    }

    // public is public ; all public logic must be deferred, all of it

    @Nullable
    @Override
    public Path createPath(Stream<BlockPos> blockStream, int range) {
        return getCurrentNavigation().createPath(blockStream, range);
    }

    @Nullable
    @Override
    public Path createPath(Set<BlockPos> blockSet, int range) {
        return getCurrentNavigation().createPath(blockSet, range);
    }

    @Nullable
    @Override
    public Path createPath(BlockPos pos, int range) {
        return getCurrentNavigation().createPath(pos, range);
    }

    @Nullable
    @Override
    public Path createPath(BlockPos pos, int idk1, int idk2) {
        return getCurrentNavigation().createPath(pos, idk1, idk2);
    }

    @Nullable
    @Override
    public Path createPath(Entity entity, int range) {
        return getCurrentNavigation().createPath(entity, range);
    }

    // public is public

    @Override
    public boolean moveTo(Entity entity, double speedMod) {
        return getCurrentNavigation().moveTo(entity, speedMod);
    }

    @Override
    public boolean moveTo(@Nullable Path path, double speedMod) {
        return getCurrentNavigation().moveTo(path, speedMod);
    }

    @Override
    public boolean moveTo(double x, double y, double z, double speedMod) {
        return getCurrentNavigation().moveTo(x, y, z, speedMod);
    }

    @Nullable
    @Override
    public Path getPath() {
        return getCurrentNavigation().getPath();
    }

    @Override
    public void tick() {
        getCurrentNavigation().tick();
    }

    @Override
    public boolean isDone() {
        return getCurrentNavigation().isDone();
    }

    // public is public, as simple as the logic for this is i don't wanna take chances
    @Override
    public boolean isInProgress() {
        return getCurrentNavigation().isInProgress();
    }

    @Override
    public void stop() {
        getCurrentNavigation().stop();
    }

    @Override
    public boolean canCutCorner(BlockPathTypes blockPathTypes) {
        return getCurrentNavigation().canCutCorner(blockPathTypes);
    }

    @Override
    public boolean isStableDestination(BlockPos pos) {
        return getCurrentNavigation().isStableDestination(pos);
    }

    @Override
    public NodeEvaluator getNodeEvaluator() {
        return getCurrentNavigation().getNodeEvaluator();
    }

    @Override
    public void setCanFloat(boolean canFloat) {
        getCurrentNavigation().setCanFloat(canFloat);
    }

    @Override
    public boolean shouldRecomputePath(BlockPos pos) {
        return getCurrentNavigation().shouldRecomputePath(pos);
    }

    @Override
    public float getMaxDistanceToWaypoint() {
        return getCurrentNavigation().getMaxDistanceToWaypoint();
    }

    @Override
    public boolean isStuck() {
        return getCurrentNavigation().isStuck();
    }

    @Override
    public boolean canFloat() {
        return getCurrentNavigation().canFloat();
    }

    // evil things and copium down here
    @Override
    protected PathFinder createPathFinder(int idk) {
        return null;//((PathNavigationAccessor)getCurrentNavigation()).createPathFinder(idk);
    }

    @Override
    protected Vec3 getTempMobPos() {
        return null;//((PathNavigationAccessor)getCurrentNavigation()).getTempMobPos();
    }

    @Override
    protected boolean canUpdatePath() {
        //throw new IllegalCallerException("canUpdatePath() cannot be called in MultiPathNavigation.");
        return true;
    }


}
