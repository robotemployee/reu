package com.robotemployee.reu.banana.entity;

import com.robotemployee.reu.banana.BananaRaid;
import com.robotemployee.reu.banana.entity.ai.DevilProtectGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class DevilEntity extends BananaRaidMob implements GeoAnimatable {

    public DevilEntity(EntityType<? extends DevilEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 5, false);
    }

    @Override
    public boolean causeFallDamage(float p_147187_, float p_147188_, DamageSource p_147189_) {
        return false;
    }

    @Override
    public float getAirliftWeight() {
        return 0;
    }

    @Override
    public float getRecycleWeight() {
        return 0;
    }

    @Override
    public float getDevilProtectionWeight() {
        return 1;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public BananaRaid.EnemyTypes getBananaType() {
        return BananaRaid.EnemyTypes.DEVIL;
    }

    public static final RawAnimation GLOW_ANIM = RawAnimation.begin().thenLoop("animation.devil.glow");

    @Override
    protected void registerGoals() {
        int goalIndex = 0;
        this.goalSelector.addGoal(goalIndex++, new AvoidEntityGoal<>(this, Player.class, 16, 1, 1.5));
        this.goalSelector.addGoal(goalIndex++, new DevilProtectGoal(this, 1, 3));
        this.goalSelector.addGoal(goalIndex++, new WaterAvoidingRandomFlyingGoal(this, 1));
        this.goalSelector.addGoal(goalIndex++, new FloatGoal(this));
        //this.goalSelector.addGoal(goalIndex++, new LookAtPlayerGoal(this, Player.class, 8));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FLYING_SPEED, 0.25)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "Flying", 5, this::glowAnimController));
    }

    protected <E extends DevilEntity> PlayState glowAnimController(final AnimationState<E> event) {
        //if (event.isMoving())
        return event.setAndContinue(GLOW_ANIM);

        //return PlayState.STOP;
    }


    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public double getTick(Object object) {
        return ((DevilEntity)object).getId();
    }
}
