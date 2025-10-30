package com.robotemployee.reu.foliant.entity;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.foliant.FoliantRaid;
import com.robotemployee.reu.foliant.entity.sound.EntitySoundInstanceThatTicks;
import com.robotemployee.reu.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class AsteirtoEntity extends FlyingFoliantRaidMob implements GeoEntity {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final double ATTACK_RANGE = 1;

    public AsteirtoEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new AsteirtoMoveControl(this); //new FlyingMoveControl(this, 20, true);
    }

    // todo
    // phases through blocks
    //

    @OnlyIn(Dist.CLIENT)
    protected EntitySoundInstanceThatTicks<AsteirtoEntity> ambient_sound = null;

    @Override
    public boolean isColliding(BlockPos p_20040_, BlockState p_20041_) {
        return false;
    }

    @Override
    public FoliantRaid.EnemyType getEnemyType() {
        return FoliantRaid.EnemyType.ASTEIRTO;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericFlyController(this));
    }


    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0)
                .add(Attributes.ATTACK_KNOCKBACK, 8)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.FLYING_SPEED, 0.008D)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }



    @Override
    protected void registerGoals() {
        super.registerGoals();
        int goalIndex = 0;
        this.goalSelector.addGoal(goalIndex++, new AsteirtoApproachAttackGoal(this));

        int targetIndex = 0;
        this.targetSelector.addGoal(targetIndex++, new AsteirtoTargetPlayerGoal(this));
    }

    @Override
    public void knockback(double strength, double x, double y) {
        super.knockback(strength * 1.6, x, y);
    }

    @Override
    public void push(@NotNull Entity entity) {
        //super.push(entity);
    }

    @Override
    public boolean canDevilGrantKnockbackResistance() {
        return false;
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        this.noPhysics = false;
        this.setNoGravity(true);

        if (canStartAmbientSound()) {
            startNewAmbientSound();
        }

        managePoison();
    }

    public void managePoison() {
        if (level().getGameTime() % 20 > 8) return;
        if (!hasEffect(MobEffects.POISON)) return;

        MobEffectInstance poison = getEffect(MobEffects.POISON);
        addEffect(new MobEffectInstance(MobEffects.POISON, poison.getDuration(), poison.getAmplifier() + 1));
    }

    public boolean canStartAmbientSound() {
        return level().isClientSide() && (ambient_sound == null || ambient_sound.isStopped());
    }

    public void startNewAmbientSound() {
        if (ambient_sound != null) ambient_sound.stopPlaying();

        ambient_sound = EntitySoundInstanceThatTicks.playAndFollow(this, ModSounds.ASTEIRTO_HUM.get(), SoundSource.HOSTILE);
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }


    public static class AsteirtoMoveControl extends MoveControl {

        // 99.9% copied from VexMoveControl bc im lazy as balls
        public AsteirtoMoveControl(Mob mob) {
            super(mob);
        }

        public void tick() {
            if (this.operation != MoveControl.Operation.MOVE_TO) return;

            Vec3 offset = new Vec3(this.wantedX - mob.getX(), this.wantedY - mob.getY(), this.wantedZ - mob.getZ());
            double distance = offset.length();

            if (distance < 1) {
                operation = Operation.WAIT;
                return;
            }

            mob.setDeltaMovement(mob.getDeltaMovement().add(offset.scale(this.speedModifier * mob.getAttributeValue(Attributes.FLYING_SPEED) / distance)));
            mob.setYRot(0);
        }
        public void justHit() {
            operation = Operation.WAIT;
        }
    }

    public static class AsteirtoApproachAttackGoal extends Goal {

        public final AsteirtoEntity asteirto;

        public AsteirtoApproachAttackGoal(AsteirtoEntity asteirto) {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
            this.asteirto = asteirto;
        }

        @Override
        public boolean canUse() {
            LivingEntity livingentity = asteirto.getTarget();
            if (livingentity != null && livingentity.isAlive() && asteirto.random.nextInt(reducedTickDelay(7)) == 0) {
                return asteirto.distanceToSqr(livingentity) > 4.0D;
            } else {
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return asteirto.getTarget() != null && asteirto.getTarget().isAlive();
        }

        @Override
        public void start() {
            LivingEntity livingentity = asteirto.getTarget();
            if (livingentity != null) {
                Vec3 targetPos = livingentity.position();
                asteirto.moveControl.setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 1.0D);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = asteirto.getTarget();
            if (target != null) {
                // ignore this shit code
                double distSqr = asteirto.distanceToSqr(target);
                Vec3 targetPos = target.getEyePosition().add(0, -asteirto.getBbHeight() * 0.5, 0);

                if (distSqr < Math.pow(AsteirtoEntity.ATTACK_RANGE, 2)) {
                    double xOffset = asteirto.getTarget().getX() - asteirto.getX();
                    double zOffset = asteirto.getTarget().getZ() - asteirto.getZ();
                    asteirto.setYRot(-((float)Mth.atan2(xOffset, zOffset)) * (180F / (float)Math.PI));
                    asteirto.doHurtTarget(target);
                    asteirto.setYRot(0);

                    ((AsteirtoMoveControl)asteirto.getMoveControl()).justHit();
                    // asteirto recoils from hitting you
                    asteirto.addDeltaMovement(targetPos.vectorTo(asteirto.getPosition(0)).normalize().scale(0.7));
                }
                asteirto.moveControl.setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 1.35);
            }
        }

    }

    public static class AsteirtoTargetPlayerGoal extends TargetGoal {

        // heavily copied from NearestAttackableTargetGoal
        public final TargetingConditions targetConditions;

        final int randomInterval = reducedTickDelay(10);

        public final AsteirtoEntity asteirto;
        public LivingEntity target;

        public AsteirtoTargetPlayerGoal(AsteirtoEntity asteirto) {
            super(asteirto, false);
            this.asteirto = asteirto;
            targetConditions = TargetingConditions.forCombat().range(getFollowDistance()).ignoreLineOfSight();
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (randomInterval > 0 && asteirto.getRandom().nextInt(randomInterval) != 0) {
                return false;
            } else {
                this.findTarget();
                return target != null;
            }
        }

        @Override
        public void start() {
            this.mob.setTarget(this.target);
            super.start();
        }

        protected void findTarget() {
            target = asteirto.level().getNearestPlayer(targetConditions, asteirto.getX(), asteirto.getEyeY(), asteirto.getZ());
        }

        protected double getFollowDistance() {
            return asteirto.getAttributeValue(Attributes.FOLLOW_RANGE);
        }

        protected AABB getTargetSearchArea(double range) {
            return asteirto.getBoundingBox().inflate(range, 4.0D, range);
        }

    }
}
