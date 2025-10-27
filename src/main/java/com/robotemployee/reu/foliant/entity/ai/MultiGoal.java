package com.robotemployee.reu.foliant.entity.ai;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.Map;

/** <p>Tool to bundle multiple goals in one, so that you don't need to run connecting logic in {@link net.minecraft.world.entity.Entity}'s tick() method.</p>
 * <p>You must inherit from this and implement your own switching logic.</p>*/
public abstract class MultiGoal<T> extends Goal {
    protected final Map<T, Goal> goals;
    protected T goalKey;

    public MultiGoal(T initialGoalKey, Map<T, Goal> goals) {
        this.goals = goals;
        this.goalKey = initialGoalKey;
    }

    public void setGoal(T key) {
        getCurrentGoal().stop();
        goalKey = key;
    }

    public Goal getCurrentGoal() {
        return goals.get(goalKey);
    }

    public Goal getGoal(T key) {
        return goals.get(key);
    }

    @Override
    public boolean canUse() {
        return getCurrentGoal().canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return getCurrentGoal().canContinueToUse();
    }

    @Override
    public boolean isInterruptable() {
        return getCurrentGoal().isInterruptable();
    }

    @Override
    public void start() {
        getCurrentGoal().start();
    }

    @Override
    public void stop() {
        getCurrentGoal().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return getCurrentGoal().requiresUpdateEveryTick();
    }

    @Override
    public void tick() {
        getCurrentGoal().tick();
    }

    @Override
    public void setFlags(EnumSet<Flag> flags) {
        getCurrentGoal().setFlags(flags);
    }

    @Override
    public String toString() {
        return "(MultiGoal)" + getCurrentGoal().toString();
    }

    @Override
    public EnumSet<Flag> getFlags() {
        return getCurrentGoal().getFlags();
    }
}
