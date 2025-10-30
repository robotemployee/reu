package com.robotemployee.reu.foliant.entity;

import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.mojang.logging.LogUtils;
import com.robotemployee.reu.foliant.FoliantRaid;
import com.robotemployee.reu.foliant.entity.ai.FlyingWanderGoal;
import com.robotemployee.reu.foliant.entity.ai.FollowMobTypeGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.OptionalInt;

public class DevilEntity extends FlyingFoliantRaidMob implements GeoEntity {

    static final Logger LOGGER = LogUtils.getLogger();

    protected static final EntityDataAccessor<OptionalInt> TRACKED_TARGET_ID = SynchedEntityData.defineId(DevilEntity.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT);

    public DevilEntity(EntityType<? extends DevilEntity> entityType, Level level) {
        super(entityType, level);
        this.entityData.define(TRACKED_TARGET_ID, OptionalInt.empty());
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.navigation = new FlyingPathNavigation(this, level);
    }

    // if you expect something to run on the clientside, make sure to use getProtectionTarget()
    private LivingEntity protectionTarget = null;

    @Nullable
    public LivingEntity getProtectionTarget() {
        if (level().isClientSide()) {
            OptionalInt optionalInt = getEntityData().get(TRACKED_TARGET_ID);
            if (optionalInt.isEmpty()) return null;
            return (LivingEntity)level().getEntity(optionalInt.getAsInt());
        }
        return protectionTarget;
    }

    public void setProtectionTarget(@Nullable LivingEntity newTarget) {
        getEntityData().set(TRACKED_TARGET_ID, newTarget == null ? OptionalInt.empty() : OptionalInt.of(newTarget.getId()));
        if (protectionTarget instanceof FoliantRaidMob foliant) {
            if (newTarget == null) foliant.stopProtectionFrom(this);
            else foliant.startProtectionFrom(this);
        }
        protectionTarget = newTarget;
    }

    public boolean isProtecting() {
        return getProtectionTarget() != null;
    }

    @Override
    public boolean canDevilProtect() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public FoliantRaid.EnemyType getEnemyType() {
        return FoliantRaid.EnemyType.DEVIL;
    }

    @Override
    protected void registerGoals() {
        // TODO: make it move a little livelier

        int goalIndex = 0;
        //this.goalSelector.addGoal(goalIndex++, new AvoidEntityGoal<>(this, Player.class, 16, 1, 1.5));
        this.goalSelector.addGoal(goalIndex++, new FloatGoal(this));
        this.goalSelector.addGoal(goalIndex++, new ProtectGoal(this, 2, 8, 16, 64));
        this.goalSelector.addGoal(goalIndex++, new FlyingWanderGoal(this, 0.1f, 5, 6));
        //this.goalSelector.addGoal(goalIndex++, new WaterAvoidingRandomFlyingGoal(this, 1));
        //this.goalSelector.addGoal(goalIndex++, new LookAtPlayerGoal(this, Player.class, 8));
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2)
                .add(Attributes.MOVEMENT_SPEED, 10)
                .add(Attributes.FLYING_SPEED, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    public static final RawAnimation GLOW_ANIM = RawAnimation.begin().thenLoop("animation.devil.glow");
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

    // todo: set to BananaRaidMob.class
    public static final Class<? extends LivingEntity> targetClass = FoliantRaidMob.class;
    public static class ProtectGoal extends FollowMobTypeGoal {

        static final Logger LOGGER = LogUtils.getLogger();

        public ProtectGoal(Mob follower, double speedModifier, double minDistance, double maxDistance, double scanRange) {
            super(follower, DevilEntity.targetClass, speedModifier, minDistance, maxDistance, scanRange, 40);
        }

        static final float DODGE_FORCE = 0.3f;
        static final int MIN_TICKS_TILL_DODGE = 10;
        static final int MAX_TICKS_TILL_DODGE = 30;
        private int ticksTillDodge = 0;

        public static final int MIN_HORIZONTAL_DISTANCE = 15;
        public static final int MIN_HEIGHT_OFFSET = 8;
        public static final int SPREAD = 5;

        static final int BUFFS_INTERVAL = 100;
        private int ticksTillBuff = 0;

        @Override
        public void tick() {
            //LOGGER.info("boop");
            RandomSource random = follower.getRandom();
            if (ticksTillDodge-- <= 0) {
                ticksTillDodge = random.nextInt(MIN_TICKS_TILL_DODGE, MAX_TICKS_TILL_DODGE);
                dodge();
            }

            if (ticksTillBuff-- <= 0) {
                ticksTillDodge = BUFFS_INTERVAL;
                applyBuffs();
            }
            super.tick();
        }

        protected void dodge() {
            RandomSource random = follower.getRandom();
            Vec3 addedMovement = new Vec3((random.nextFloat() - 0.5), (random.nextFloat() - 0.5) * 0.15, (random.nextFloat() - 0.5))
                    .scale(2 * random.nextFloat() * DODGE_FORCE);
            follower.addDeltaMovement(addedMovement);
        }

        protected void applyBuffs() {
            //getTarget().addEffect(new MobEffectInstance(MobEffects.REGENERATION, BUFFS_INTERVAL + 40));
            if (follower.level().getGameTime() % 20 == 0) getTarget().heal(1);
            getTarget().addEffect(new MobEffectInstance(MobEffects.REGENERATION, BUFFS_INTERVAL + 40));
            if (!(getTarget() instanceof FoliantRaidMob foliant) || foliant.canDevilGrantKnockbackResistance()) {
                getTarget().addEffect(new MobEffectInstance(AMEffectRegistry.KNOCKBACK_RESISTANCE.get(), BUFFS_INTERVAL + 40, 1));
            }
            getTarget().addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, BUFFS_INTERVAL + 40));
            getTarget().addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BUFFS_INTERVAL + 40, 1));
            //LOGGER.info("Applying buffs to target at" + getTarget().blockPosition());
        }

        /*
        @Override
        protected Path pathFromPositionOfTarget(BlockPos targetPos) {
            RandomSource random = follower.getRandom();
            Vec3 diff = follower.position().vectorTo(targetPos.getCenter());
            Vec2 hDiff = new Vec2((float)diff.x, (float)diff.z);

            // the random spread is done at the end
            Vec2 desiredHorizontalOffset = hDiff.normalized().scale(MIN_HORIZONTAL_DISTANCE);

            Path result = null;
            for (int i = 0; i < 5; i++) {
                float spread = SPREAD * (1 + i / 2f);
                Vec3 testPosition = new Vec3(
                        desiredHorizontalOffset.x + random.nextFloat() * spread,
                        MIN_HEIGHT_OFFSET + random.nextFloat() * spread,
                        desiredHorizontalOffset.y + random.nextFloat() * spread
                        ).add(getTarget().position());

                result = super.pathFromPositionOfTarget(BlockPos.containing(testPosition));
                if (result != null && result.canReach()) break;
            }

            return result;
        }
         */

        @Override
        protected BlockPos getMoveToPos() {
            double angle = ((follower.level().getGameTime() % 80) / 80d) * 2 * Math.PI;
            BlockPos target = getTarget().blockPosition();
            double xOffset = Math.cos(angle) * SPREAD;
            double yOffset = 6;
            double zOffset = Math.sin(angle) * SPREAD;
            return target.offset((int)Math.ceil(xOffset), (int)Math.floor(yOffset), (int)Math.ceil(zOffset));
        }

        @Override
        protected double getWeightForEntity(LivingEntity entity) {
            double distanceWeight = 2 * ((scanRange - follower.distanceTo(entity)) / scanRange);
            double healthWeight = 2 * (1 - (entity.getHealth() / entity.getMaxHealth()));
            // todo: uncomment
            double multiplier = 1;//((BananaRaidMob)entity).getDevilProtectionWeight();
            return (distanceWeight + healthWeight) * multiplier;
        }

        @Override
        public boolean isValidTarget(@NotNull LivingEntity entity) {

            if (!((FoliantRaidMob)entity).canDevilProtect()) return false;
            return super.isValidTarget(entity);
        }

        @Override
        public void stop() {
            // fixme logger
            LOGGER.info("No longer following");
            super.stop();
        }

        @Override
        public void setTarget(LivingEntity target) {
            // fixme logger
            LOGGER.info("SETTING TARGET TO " + target);
            ((DevilEntity)follower).setProtectionTarget(target);
        }

        @Override
        public LivingEntity getTarget() {
            return ((DevilEntity)follower).getProtectionTarget();
        }
    }
}
