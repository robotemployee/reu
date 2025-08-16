package com.robotemployee.reu.banana.entity.ai;

import com.robotemployee.reu.banana.entity.BananaRaidMob;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class DevilProtectGoal extends FollowSpecificMobGoal {
    public DevilProtectGoal(Mob follower, double speedModifier, double maxDistance) {
        super(follower, BananaRaidMob.class, speedModifier, maxDistance, 40);
    }

    static final float DODGE_FORCE = 0.3f;
    static final int MIN_TICKS_TILL_DODGE = 10;
    static final int MAX_TICKS_TILL_DODGE = 30;
    private int ticksTillDodge = 0;

    static final int BUFFS_INTERVAL = 100;
    private int ticksTillBuff = 0;

    @Override
    public void tick() {
        RandomSource random = follower.getRandom();
        if (--ticksTillDodge <= 0) {
            ticksTillDodge = random.nextInt(MIN_TICKS_TILL_DODGE, MAX_TICKS_TILL_DODGE);
            dodge();
        }

        if (--ticksTillBuff <= 0) {
            ticksTillDodge = BUFFS_INTERVAL;
            applyBuffs();
        }
        super.tick();
    }

    protected void dodge() {
        RandomSource random = follower.getRandom();
        Vec3 addedMovement = new Vec3((random.nextFloat() - 0.5), (random.nextFloat() - 0.5) * 0.25, (random.nextFloat() - 0.5))
                .scale(2 * random.nextFloat() * DODGE_FORCE);
        follower.addDeltaMovement(addedMovement);
    }

    protected void applyBuffs() {
        // TODO: implement
    }


    protected double getWeightForEntity(LivingEntity entity) {
        double distanceWeight = 2 * ((maxDistance - follower.distanceTo(entity)) / maxDistance);
        double healthWeight = 2 * (1 - (entity.getHealth() / entity.getMaxHealth()));
        double multiplier = ((BananaRaidMob)entity).getDevilProtectionWeight();
        return (distanceWeight + healthWeight) * multiplier;
    }

    @Override
    public boolean isValidTarget(LivingEntity entity) {
        if (!((BananaRaidMob)entity).canDevilProtect()) return false;
        return super.isValidTarget(entity);
    }
}
