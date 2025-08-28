package com.robotemployee.reu.banana.entity;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.banana.BananaRaid;
import com.robotemployee.reu.banana.entity.ai.MultiGoal;
import com.robotemployee.reu.banana.entity.ai.MultiMoveControl;
import com.robotemployee.reu.banana.entity.ai.MultiPathNavigation;
import com.robotemployee.reu.banana.entity.ai.StrictGroundPathNavigation;
import com.robotemployee.reu.banana.entity.sound.GregFlyingSoundInstance;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.LevelUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;

public class GregEntity extends BananaRaidMob implements GeoEntity {
    static final Logger LOGGER = LogUtils.getLogger();

    public static final EntityDataAccessor<Boolean> IS_FLYING = SynchedEntityData.defineId(GregEntity.class, EntityDataSerializers.BOOLEAN);
    public static final SerializableDataTicket<String> VISUAL_STATE = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofString(new ResourceLocation(RobotEmployeeUtils.MODID, "visual_state")));
    public static final String IS_FLYING_PATH = "IsFlying";

    private VisualState visualState = VisualState.GROUNDED;
    private BehaviorMode behaviorMode = BehaviorMode.BRAVE;

    public static final int MAX_PATH_LENGTH = 64;
    protected int ticksWantedPosWithoutMoving = 0;
    public static final int ticksUntilJumpWhenStuck = 20;
    public static final int tickDurationOfTakingOff = 40;
    public static final int tickDurationOfLanding = 40;
    long timestampOfAnimationStarted;

    public GregFlyingSoundInstance flyingSoundInstance;

    public GregEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        entityData.define(IS_FLYING ,false);

        HashMap<MoveControlMode, MoveControl> movements = new HashMap<>();
        movements.put(MoveControlMode.FLYING, new FlyingMoveControl(this, 20, true));
        movements.put(MoveControlMode.GROUNDED, new MoveControl(this));
        moveControl = new MultiMoveControl<>(this, MoveControlMode.GROUNDED, movements);

        HashMap<MoveControlMode, PathNavigation> navigations = new HashMap<>();
        navigations.put(MoveControlMode.FLYING, new FlyingPathNavigation(this, level));
        navigations.put(MoveControlMode.GROUNDED, new StrictGroundPathNavigation(this, level));
        navigation = new MultiPathNavigation<>(this, level, MoveControlMode.GROUNDED, navigations);
    }

    @Override
    public void onAddedToWorld() {
        if (isFlying()) startFlying(true);
        super.onAddedToWorld();
    }

    // courtesy of FlyingMob or whatever that class is called
    @Override
    protected void checkFallDamage(double p_20809_, boolean p_20810_, BlockState p_20811_, BlockPos p_20812_) {
    }

    @Override
    public void baseTick() {
        super.baseTick();

        if (level().isClientSide()) return;

        /*
        LOGGER.info(String.format("speedInfo (%s): " +
                        "Flying: %s Flyspeed... Base: %.1f Mod: %.1f, " +
                        "Gndspeed... Base: %.1f Mod: %.1f ... " +
                        "Speedmod: %.1f Speed: %.1f Flyspeed: %.1f " +
                        "isFallFlying: %s, onGround: %s, shouldDiscardFriction: %s, isNoGravity: %s, isJumping: %s" +
                        "Fly... hasWanted: %s isPathing: %s Gnd... hasWanted: %s isPathing: %s " +
                        "Desired movement.. Fwd: %s Strafe: %s Vert:%s",
                level().isClientSide() ? "CLIENT" : "LEVEL",

                getMultiMoveControl().getKey() == MoveControlMode.FLYING,
                getAttributeBaseValue(Attributes.FLYING_SPEED),
                getAttributeValue(Attributes.FLYING_SPEED) - getAttributeBaseValue(Attributes.FLYING_SPEED),

                getAttributeBaseValue(Attributes.MOVEMENT_SPEED),
                getAttributeValue(Attributes.MOVEMENT_SPEED) - getAttributeBaseValue(Attributes.MOVEMENT_SPEED),

                getMoveControl().getSpeedModifier(),
                getSpeed(),
                getFlyingSpeed(),

                isFallFlying(),
                onGround(),
                shouldDiscardFriction(),
                isNoGravity(),
                jumping,

                getMultiMoveControl().getMovement(MoveControlMode.FLYING).hasWanted(),
                getMultiPathNavigation().getNavigation(MoveControlMode.FLYING).isInProgress(),
                getMultiMoveControl().getMovement(MoveControlMode.GROUNDED).hasWanted(),
                getMultiPathNavigation().getNavigation(MoveControlMode.GROUNDED).isInProgress(),

                zza,
                xxa,
                yya
        ));
         */

        boolean flying = isFlying();
        if (!flying && onGround() && getMoveControl().hasWanted() && getDeltaMovement().lengthSqr() < 0.05) {
            if (ticksWantedPosWithoutMoving++ >= ticksUntilJumpWhenStuck) {
                //LOGGER.info("Greg jumping because stuck");
                this.jumpFromGround();
            }
        } else {
            ticksWantedPosWithoutMoving = 0;
        }

        if (flying) {
            // add a little bit of up / down oscillation
            final int tickPeriod = 140;
            final double amplitude = 0.001;
            double time = ((level().getGameTime() % tickPeriod) / (float)tickPeriod) * Math.PI * 2;
            double addedYVel = amplitude * Math.sin(time);
            //LOGGER.info(String.format("Bubbling... T: %s R: %s", time, addedYVel));
            addDeltaMovement(new Vec3(0, addedYVel, 0));
        }

        VisualState visualState = getVisualState();
        switch (visualState) {
            case FLYING:
            case GROUNDED:
                break;
            default:
                boolean landing = visualState == VisualState.LANDING;
                long duration = landing ? tickDurationOfLanding : tickDurationOfTakingOff;
                //LOGGER.info((landing ? "landing" : "taking off") + " time until done: " + (duration - (level().getGameTime() - timestampOfAnimationStarted)) + "timestamp: " + timestampOfAnimationStarted);
                //LOGGER.info(String.format("gametime: %s, timestamp: %s, duration: %s", level().getGameTime(), timestampOfAnimationStarted, duration));
                if (level().getGameTime() - timestampOfAnimationStarted > duration) {
                    //LOGGER.info("Animation finished, running logic");
                    // todo inspect
                    if (landing) stopFlying(true);
                    else startFlying(true);
                }
                break;
        }
    }

    public void setBehaviorMode(BehaviorMode mode) {
        behaviorMode = mode;
    }

    public BehaviorMode getBehaviorMode() {
        return behaviorMode;
    }

    public static final Class<? extends LivingEntity> TARGET_CLASS = Pig.class;
    @Override
    protected void registerGoals() {
        int goalIndex = 0;
        int targetIndex = 0;
        //goalSelector.addGoal(goalIndex++, new GregComeToGroundAndFinishLandingGoal(this));
        goalSelector.addGoal(goalIndex++, new FloatGoal(this));
        goalSelector.addGoal(goalIndex++, new MeleeAttackGoal(this, 1.3, true));
        // too much extra stuff...
        // i got the "swaps between fearful and not fearful" thing to work perfectly
        // but gameplay-wise it feels a little weird and frustrating for what is meant to be fodder - something reliable
        // i do still have interest in making it fearful, but i think it would be best if it were as a
        // self-preservation response after getting to low health
        //goalSelector.addGoal(goalIndex++, new GregAlternateAttackAndRunAwayGoal(this));

        // Flight management
        goalSelector.addGoal(goalIndex++, new GregTakeOffIfUnreachableGoal(this));
        // todo GregTakeOffToAirliftGoal
        //
        goalSelector.addGoal(goalIndex++, new GregLandIfIdleGoal(this));
        goalSelector.addGoal(goalIndex++, new GregLandIfCouldReachTargetAnywayGoal(this));

        goalSelector.addGoal(goalIndex++, new RandomLookAroundGoal(this));
        goalSelector.addGoal(goalIndex++, new WaterAvoidingRandomStrollGoal(this, 0.6));

        targetSelector.addGoal(targetIndex++, new NearestAttackableTargetGoal<>(this, TARGET_CLASS, false));
        targetSelector.addGoal(targetIndex++, new HurtByTargetGoal(this, BananaRaidMob.class).setAlertOthers(IBananaNonAttacking.class));

        /*
        * alternate between moving to attack and running away, on a global timer - attacks have 5% chance to cause blindness for 4 seconds
        * airlift allies
        * start flying if can't reach
        * */
    }

    protected static final double MOVEMENT_SPEED = 0.3;
    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.FLYING_SPEED, 4)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }

    // takeOff() -> startFlying() -> startLanding() -> startLandingAnim() -> stopFlying()
    // takeOff() and startLanding() serve as the transitional functions;
    // if you want greg to change its flying state, use one of those
    // whereas start and stopFlying() are for the exact moments where it programmatically changes mode;
    // the point where the movement functions change and everything
    // those functions are called in baseTick() once the animation is finished
    // tldr the AI goals tell the animations to do their magic and the actual behavior of greg changes once those animations are done

    // this function is called in the ai goals that tell greg to start flying
    public void takeOff() {
        //LOGGER.info("Taking off. Current time: " + level().getGameTime());
        timestampOfAnimationStarted = level().getGameTime();
        setVisualState(VisualState.TAKING_OFF);
        addDeltaMovement(new Vec3(0, 0.08, 0));

        //entityData.set(IS_FLYING, true);
        // isFlying() would be more useful if it only returned true when greg is using the flight controls and etc
    }

    // this function is called in the animation controller once greg has finished taking off
    public void startFlying(boolean quietly) {
        // thank god for how capable minecraft's sound system is
        // sure you can't speed up / slow down irrespective of pitch but yknow that's complicated
        if (flyingSoundInstance == null || flyingSoundInstance.isStopped()) {
            //LOGGER.info("Playing new flying sound");
            flyingSoundInstance = GregFlyingSoundInstance.startFrom(this);
        }

        setNoGravity(true);
        //LOGGER.info("Starting to fly, setting visual state");
        setVisualState(VisualState.FLYING);
        //LOGGER.info("New visual state is " + getVisualState() + " for " + (level().isClientSide() ? "client" : "server"));
        getMultiMoveControl().setMovement(MoveControlMode.FLYING, false);

        if (quietly) getMultiPathNavigation().setNavigationQuietly(MoveControlMode.FLYING);
        else getMultiPathNavigation().setNavigationLoudly(MoveControlMode.FLYING);

        //LOGGER.info("New move mode is " + getMultiMoveControl().getKey() + ", new path mode is " + getMultiPathNavigation().getNavKey());
        // in case you did not call takeOff() first
        entityData.set(IS_FLYING, true);
    }

    // this function is called in the ai goals that tell greg to come to the land so it could start the landing process
    public static final int DISTANCE_TO_LAND = 2;
    public boolean landIfCloseToGround(@NotNull BlockPos groundPos) {
        BlockPos pos = blockPosition();
        int distance = pos.getY() - groundPos.getY();
        if (distance <= DISTANCE_TO_LAND + 1) {
            startLanding();
            return true;
        }
        return false;
    }
    public void startLanding() {
        getNavigation().stop();
        startLandingAnim();
    }
    // this function is called in another ai goal when greg is close enough to the ground to actually land
    protected void startLandingAnim() {
        timestampOfAnimationStarted = level().getGameTime();
        setVisualState(VisualState.LANDING);
    }
    // this function is called in the ai goal that tells greg to actually change its flying state when it comes to the ground
    public void stopFlying(boolean quietPathNav) {
        setVisualState(VisualState.GROUNDED);
        // fixme inspect keepWantedLocation
        getMultiMoveControl().setMovement(MoveControlMode.GROUNDED, true);

        if (quietPathNav) getMultiPathNavigation().setNavigationQuietly(MoveControlMode.GROUNDED);
        else getMultiPathNavigation().setNavigationLoudly(MoveControlMode.GROUNDED);

        /*
        AttributeInstance movespeed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (movespeed != null) movespeed.setBaseValue(MOVEMENT_SPEED);
         */
        //setSpeed(0);

        setNoGravity(false);
        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x, 0, motion.z);
        entityData.set(IS_FLYING, false);
    }

    public MultiMoveControl<MoveControlMode> getMultiMoveControl() {
        return (MultiMoveControl<MoveControlMode>) moveControl;
    }

    public MultiPathNavigation<MoveControlMode> getMultiPathNavigation() {
        return (MultiPathNavigation<MoveControlMode>) navigation;
    }

    public boolean canChangeFlying() {
        //LOGGER.info("Asking if can change flying. Visual state is " + getVisualState());
        VisualState state = getVisualState();
        return state != VisualState.TAKING_OFF && state != VisualState.LANDING;
    }

    public VisualState getVisualState() {
        if (level().isClientSide()) {
            String state = getAnimData(VISUAL_STATE);
            if (state == null) visualState = VisualState.GROUNDED;
            else visualState = VisualState.valueOf(state);
        }
        return visualState;
    }

    public void setVisualState(VisualState state) {
        setAnimData(VISUAL_STATE, state.name());
        visualState = state;
    }

    public MoveControlMode getMoveControlMode() {
        return getMultiMoveControl().getKey();
    }

    public boolean isFlying() {
        return getEntityData().get(IS_FLYING);
    }

    public boolean isInGroundMode() {
        return !isFlying();
    }

    @Override
    public float getAirliftWeight() {
        return 0;
    }

    @Override
    public float getImportance() {
        return 1;
    }

    @Override
    public BananaRaid.EnemyTypes getBananaType() {
        return BananaRaid.EnemyTypes.GREG;
    }

    public static final RawAnimation TAKE_OFF_ANIM = RawAnimation.begin().thenPlay("misc.take_off");
    public static final RawAnimation FLYING_ANIM = RawAnimation.begin().thenLoop("move.fly");
    public static final RawAnimation LANDING_ANIM = RawAnimation.begin().thenPlay("misc.land");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // walk animation
        // animation to start flying
        // then keep playing the flying animation
        // play the stop flying animation when landing
        // occasionally make it look at things with the look at animation
        //controllers.add(new AnimationController(this, "Walking", 5, this::walkAnimController));
        //controllers.add(DefaultAnimations.genericWalkController(this));
        controllers.add(new AnimationController<>(this, "Flying", 0, this::animController));
    }

    /*
    protected <E extends GregEntity> PlayState walkAnimController(final AnimationState<E> event) {
        //if (!event.isMoving() || !event.getAnimatable().onGround()) return PlayState.CONTINUE;
        E greg = event.getAnimatable();
        boolean navigating = greg.getNavigation().isInProgress();
        boolean moving = event.isMoving();
        boolean ordering = greg.getMoveControl().hasWanted();
        //LOGGER.info(String.format("What the animation sees ... nav: %s, mov: %s, ord: %s", navigating, moving, ordering));
        return event.setAndContinue(WALK_ANIM);
        //return PlayState.STOP;
    }
     */

    protected <E extends GregEntity> PlayState animController(final AnimationState<E> event) {
        //LOGGER.info("Flycont is in " + getVisualState());
        switch (getVisualState()) {
            case GROUNDED -> {
                return event.isMoving() ? event.setAndContinue(DefaultAnimations.WALK) : PlayState.STOP;
            }
            case TAKING_OFF -> {
                if (event.getController().hasAnimationFinished()) {
                    //LOGGER.info("Taking off animation has finished, starting flying");
                    setVisualState(VisualState.FLYING);
                    //LOGGER.info("Animation finished");
                    startFlying(false);
                    return event.setAndContinue(FLYING_ANIM);
                }
                //LOGGER.info("Still taking off");
                return event.setAndContinue(TAKE_OFF_ANIM);
            }
            case FLYING -> {
                //LOGGER.info("Currently flying");
                return event.setAndContinue(FLYING_ANIM);
            }
            case LANDING -> {
                if (event.getController().hasAnimationFinished()) {
                    //LOGGER.info("Animation finished");
                    setVisualState(VisualState.GROUNDED);
                    return PlayState.CONTINUE;
                }
                //LOGGER.info("Still landing");
                return event.setAndContinue(LANDING_ANIM);
            }
            default -> {
                //LOGGER.info("Shouldn't happen");
                return PlayState.CONTINUE;
            }
        }
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // spend a little while attacking, spend a little while running away
    public static class GregAlternateAttackAndRunAwayGoal extends MultiGoal<BehaviorMode> {

        static final Logger LOGGER = LogUtils.getLogger();
        public final GregEntity greg;
        final int offset;
        public GregAlternateAttackAndRunAwayGoal(GregEntity greg) {
            super(BehaviorMode.BRAVE, new HashMap<>());
            this.greg = greg;
            goals.put(BehaviorMode.BRAVE, new MeleeAttackGoal(greg, 1.3, true));
            goals.put(BehaviorMode.FEARFUL, new AvoidEntityGoal<>(greg, GregEntity.TARGET_CLASS, 32, 1, 1.1));
            long time = greg.level().getGameTime();
            // fixme
            this.offset = -(int)(time % INTERVAL) + greg.getRandom().nextInt(-MAX_SWAP_TICKS_OFFSET, MAX_SWAP_TICKS_OFFSET);
            setTicksUntilSwap();
        }

        static final int INTERVAL = 400;
        static final int MAX_SWAP_TICKS_OFFSET = 10;
        int ticksUntilSwap;
        boolean swapped = false;

        @Override
        public boolean canUse() {
            if (shouldSwap()) {
                swap();
                return false;
            }

            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            if (swapped) {
                swapped = false;
                return false;
            }

            return super.canContinueToUse();
        }

        @Override
        public void tick() {
            if (shouldSwap()) {
                swap();
                return;
            }
            super.tick();
        }

        @Override
        public void start() {
            //LOGGER.info("Starting " + (goalKey == BehaviorMode.BRAVE ? "brave" : "afraid"));
            super.start();
        }

        boolean shouldSwap() {
            boolean result = ticksUntilSwap-- <= 0;
            // this is copium due to avoid entity goal having a reduced ticking rate
            if (goalKey == BehaviorMode.FEARFUL) ticksUntilSwap--;
            //LOGGER.info(String.format("%s ticks until next swap, swap=%s", ticksUntilSwap, result));
            if (result) setTicksUntilSwap();
            return result;
        }

        void swap() {
            swapped = true;
            if (goalKey == BehaviorMode.BRAVE) {
                setGoal(BehaviorMode.FEARFUL);
            } else {
                setGoal(BehaviorMode.BRAVE);
            }
            setTicksUntilSwap();
        }

        void setTicksUntilSwap() {
            ticksUntilSwap = INTERVAL + offset;
        }

        @Override
        public void setGoal(BehaviorMode key) {
            greg.setBehaviorMode(key);
            super.setGoal(key);
        }
    }

    public static final int VERTICAL_DISTANCE_UNTIL_FLY_TO_REACH = 5;
    public static class GregTakeOffIfUnreachableGoal extends Goal {
        public final GregEntity greg;
        public GregTakeOffIfUnreachableGoal(GregEntity greg) {
            this.greg = greg;
        }
        static final int PATHING_CHECK_INTERVAL = 40;
        int ticksUntilNextPathingCheck = PATHING_CHECK_INTERVAL;

        @Override
        public boolean canUse() {
            MoveControlMode moveMode = greg.getMoveControlMode();

            boolean canCheckPath = ticksUntilNextPathingCheck-- <= 0;
            if (canCheckPath) ticksUntilNextPathingCheck = PATHING_CHECK_INTERVAL;


            LivingEntity target = greg.getTarget();

            boolean isInGroundMode = greg.isInGroundMode();
            boolean canChangeFlying = greg.canChangeFlying();
            boolean isNotAlreadyTakingOff = greg.getVisualState() != VisualState.TAKING_OFF;

            boolean pathSucks = canCheckPath && canWeNotPath();
            boolean targetTooVerticallyFar = (target != null) && (target.blockPosition().getY() - greg.blockPosition().getY() > VERTICAL_DISTANCE_UNTIL_FLY_TO_REACH);

            boolean canTakeOff = isInGroundMode && canChangeFlying && isNotAlreadyTakingOff;
            boolean shouldTakeOff = targetTooVerticallyFar || pathSucks;

            //LOGGER.info(String.format("GREG takeoffcheck. " + (canTakeOff && shouldTakeOff ? "ACTIVATING" : "") + " can: %s %s%s%s%s, should: %s %s%s", canTakeOff, isInGroundMode, canChangeFlying, isNotLanding, isNotAlreadyTakingOff, shouldTakeOff, targetTooVerticallyFar, pathSucks));
            return canTakeOff && shouldTakeOff;
        }

        public boolean canWeNotPath() {
            MultiPathNavigation<MoveControlMode> navigation = greg.getMultiPathNavigation();
            LivingEntity targetEntity = greg.getTarget();
            BlockPos navTarget = navigation.getTargetPos();
            BlockPos gregTarget = targetEntity != null ? targetEntity.blockPosition() : null;
            BlockPos generalTarget = navTarget == null ? gregTarget : navTarget;
            boolean hasTarget = generalTarget != null;
            if (!hasTarget) return false;
            Path path = navigation.createPath(generalTarget, GregEntity.MAX_PATH_LENGTH);
            return (path == null || !path.canReach());
        }

        @Override
        public void start() {
            //MultiPathNavigation<MoveControlMode> navigation = greg.getMultiPathNavigation();
            greg.takeOff();
            //LivingEntity targetEntity = greg.getTarget();
            //if (targetEntity != null) navigation.moveTo(greg.getTarget(), 2);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

    public static class GregLandIfCouldReachTargetAnywayGoal extends Goal {

        // Does not ask Greg to navigate to the ground because if their target is reachable from the ground they'll get there anyway eventually

        public final GregEntity greg;
        public GregLandIfCouldReachTargetAnywayGoal(GregEntity greg) {
            this.greg = greg;
            lastCheckedGroundPosTimestamp = greg.level().getGameTime() - greg.getRandom().nextInt(10);
        }

        //long lastTickWhereMovingToTarget;

        static final int CREATE_PATH_COOLDOWN = 40;
        int createPathCooldownTicks = CREATE_PATH_COOLDOWN;

        boolean groundPathGood = false;
        @Override
        public boolean canUse() {
            MoveControlMode moveMode = greg.getMoveControlMode();
            MultiPathNavigation<MoveControlMode> navigation = greg.getMultiPathNavigation();

            BlockPos targetPos = navigation.getTargetPos();
            boolean hasTargetPos = targetPos != null;
            LivingEntity targetEntity = greg.getTarget();
            boolean hasTargetEntity = targetEntity != null;

            // checks for if we even can
            boolean canChangeFlying = greg.canChangeFlying();
            boolean isFlying = greg.isFlying();

            // checks for if we should based on whether we could reach the target from the ground
            boolean canCreatePath = createPathCooldownTicks-- <= 0;
            boolean isPathing = navigation.isInProgress();
            Path groundPath;
            if (canCreatePath && hasTargetPos && isPathing) {
                createPathCooldownTicks = CREATE_PATH_COOLDOWN;
                groundPath = navigation.getNavigation(MoveControlMode.GROUNDED).createPath(targetPos, 32);
            } else groundPath = null;
            boolean groundCloseToTarget = getTargetDistanceToGround() < VERTICAL_DISTANCE_UNTIL_FLY_TO_REACH;
            // update groundPathGood
            if (canCreatePath) groundPathGood = groundPath != null && groundPath.canReach() && groundCloseToTarget;

            // checks for if we have gone too long without having a targeted entity
            // we also need to either be pathing somewhere and have a good ground path
            // or not be pathing at all

            boolean rejectingSwap = false;
            boolean canSwap = (canChangeFlying && isFlying) && !rejectingSwap;
            boolean shouldSwap = groundPathGood;

            //LOGGER.info(String.format("GREG landcheck. " + (canSwap && shouldSwap ? "ACTIVATING" : "") + " can: %s %s%s%s, should: %s pathGood: %s idling: %s idleTime: %s", canSwap, canChangeFlying, isFlying, !rejectingSwap, shouldSwap, groundPathGood, idling, greg.level().getGameTime() - lastTickWhereMovingToTarget));
            return canSwap && shouldSwap;
        }

        @Nullable
        BlockPos groundPos = null;

        long lastCheckedGroundPosTimestamp;
        public static final int GROUND_POS_CHECK_INTERVAL = 30;

        public boolean canCheckGroundPos() {
            return greg.level().getGameTime() - lastCheckedGroundPosTimestamp > GROUND_POS_CHECK_INTERVAL;
        }
        public BlockPos getOrFindGroundPos() {
            boolean alreadyFound = groundPos != null;
            boolean innacurateGroundPos = alreadyFound && (groundPos.getX() != greg.getX() || groundPos.getZ() != greg.getZ() || groundPos.getY() > greg.getY());
            boolean shouldRedoGroundPos = innacurateGroundPos && canCheckGroundPos();
            if ((!alreadyFound) || shouldRedoGroundPos) {
                lastCheckedGroundPosTimestamp = greg.level().getGameTime();
                groundPos = LevelUtils.findSolidGroundBelow(greg);
            }
            return groundPos;
        }

        public int getTargetDistanceToGround() {
            LivingEntity target = greg.getTarget();
            int referenceY = (target == null) ? greg.getBlockY() : target.getBlockY();
            getOrFindGroundPos();
            if (groundPos == null) return 128;
            // comparing the target's vertical position to the ground position below greg
            return referenceY - groundPos.getY();
        }

        boolean successfullyLanded = false;
        @Override
        public void start() {
            successfullyLanded = successfullyLanded || greg.landIfCloseToGround(getOrFindGroundPos());
        }

        @Override
        public boolean canContinueToUse() {
            return !successfullyLanded;
        }

        @Override
        public void tick() {
            successfullyLanded = successfullyLanded || greg.landIfCloseToGround(getOrFindGroundPos());
        }

        @Override
        public void stop() {
            successfullyLanded = false;
        }
    }
    public static class GregLandIfIdleGoal extends Goal {
        // Involves navigating to the ground

        public final GregEntity greg;
        long lastTickWhereMovingToTarget;
        long lastTickWhereRecalculatedGroundPos;
        static final int TICKS_RECALCULATE_GROUND_COOLDOWN = 40;
        boolean idling = false;
        boolean landed = false;
        BlockPos groundPos = null;
        BlockPos targetPos = null;

        static final int TICKS_UNTIL_IDLE = 100;
        public GregLandIfIdleGoal(GregEntity greg) {
            this.greg = greg;
            lastTickWhereMovingToTarget = greg.level().getGameTime();
            lastTickWhereRecalculatedGroundPos = greg.level().getGameTime();
        }

        @Override
        public boolean canUse() {
            boolean isMoving = greg.getMoveControl().hasWanted();
            trackIdling();

            boolean can = greg.canChangeFlying() && greg.isFlying();
            boolean should = idling;
            return can && should;
        }

        public void trackIdling() {
            boolean hasTargetEntity = greg.getTarget() != null;
            // Only tracks airborne idle ticks
            if (hasTargetEntity || greg.isInGroundMode()) lastTickWhereMovingToTarget = greg.level().getGameTime();
            idling = greg.level().getGameTime() - lastTickWhereMovingToTarget > TICKS_UNTIL_IDLE;
            //LOGGER.info(String.format("GREG tracking idle time; idling: %s idleTime: %s", idling, greg.level().getGameTime() - lastTickWhereMovingToTarget));
        }

        @Override
        public void start() {
            //LOGGER.info("GREG idle goal starting");
            getOrFindGroundPos();
            greg.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 0.7);
            landed = greg.landIfCloseToGround(groundPos);
        }

        @Override
        public boolean canContinueToUse() {
            trackIdling();
            return idling && !landed;
        }

        public BlockPos getOrFindGroundPos() {
            if (canRecalculateGroundPos()) {
                groundPos = LevelUtils.findSolidGroundBelow(greg);
                lastTickWhereRecalculatedGroundPos = greg.level().getGameTime();
            }
            targetPos = groundPos != null ? groundPos.above() : null;
            //LOGGER.info("GREG found ground pos at " + groundPos);
            return groundPos;
        }

        public boolean canRecalculateGroundPos() {
            BlockPos current = greg.blockPosition();
            if (groundPos == null) return true;
            if (current.getX() == groundPos.getX() && current.getZ() == groundPos.getZ()) return false;
            return greg.level().getGameTime() - lastTickWhereRecalculatedGroundPos > TICKS_RECALCULATE_GROUND_COOLDOWN;
        }

        @Override
        public void tick() {
            BlockPos currentTarget = greg.getNavigation().getTargetPos();
            if (currentTarget != groundPos.above()) greg.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 0.7);
            landed = landed || greg.landIfCloseToGround(groundPos);
        }

        @Override
        public void stop() {
            landed = false;
            groundPos = null;
            targetPos = null;
        }
    }

    /*
    public static class GregComeToGroundAndFinishLandingGoal extends Goal {

        static final Logger LOGGER = LogUtils.getLogger();
        public final GregEntity greg;
        public static final int DISTANCE_TO_GROUND_UNTIL_LAND = 2;
        BlockPos groundPos;
        public GregComeToGroundAndFinishLandingGoal(GregEntity greg) {
            this.greg = greg;
        }

        @Override
        public boolean canUse() {
            boolean attemptingToLand = greg.isComingToLand();

            boolean can = attemptingToLand;
            boolean should = attemptingToLand;

            return can && should;
        }

        @Override
        public void start() {
            findGround();
        }

        public BlockPos findGround() {
            groundPos = LevelUtils.findSolidGroundBelow(greg);
            return groundPos;
        }

        public BlockPos getGroundPos() {
            return groundPos != null ? groundPos : findGround();
        }

        public boolean isCloseToLand() {
            // the +1 is to get the air block above it; you want to return a distance of 0 when they are right above the ground
            return getGroundPos().distSqr(greg.blockPosition()) + 1 < Math.pow(DISTANCE_TO_GROUND_UNTIL_LAND, 2);
        }

        @Override
        public boolean canContinueToUse() {
            return greg.isComingToLand();
        }

        @Override
        public boolean isInterruptable() {
            return false;
        }

        @Override
        public void tick() {
            // ixme logger
            LOGGER.info("Greg coming to the ground. Detected ground pos: " + groundPos);
            //greg.addDeltaMovement(new Vec3(0, -0.03, 0));
            if (greg.getNavigation().isInProgress()) greg.getNavigation().stop();
            //greg.addDeltaMovement(new Vec3(0, -0.025, 0));
            if (greg.getMoveControl().getWantedY() != groundPos.getY()) greg.getMoveControl().setWantedPosition(groundPos.getX(), groundPos.getY(), groundPos.getZ(), 0.7);
            if (groundPos.getX() != greg.getX() || groundPos.getZ() != greg.getZ()) findGround();
            if (!isCloseToLand()) return;
            if (greg.getVisualState() == VisualState.FLYING) greg.startLandingAnim();
        }
    }

     */

    @Override
    protected SoundEvent getHurtSound(DamageSource p_33034_) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SWEET_BERRY_BUSH_BREAK;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean(IS_FLYING_PATH, isFlying());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.getBoolean(IS_FLYING_PATH)) startFlying(true);
    }

    /*
    @Override
    public void travel(Vec3 vec) {
        if (isFlying()) flyTravel(vec);
        else groundTravel(vec);
    }


    public void groundTravel(@NotNull Vec3 vec) {
        super.travel(vec);
    }

    public void flyTravel(@NotNull Vec3 vec) {
        if (this.isControlledByLocalInstance()) {
            if (this.isInWater()) {
                this.moveRelative(0.02F, vec);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale((double)0.8F));
            } else if (this.isInLava()) {
                this.moveRelative(0.02F, vec);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
            } else {
                BlockPos ground = getBlockPosBelowThatAffectsMyMovement();
                float f = 0.91F;
                if (this.onGround()) {
                    f = this.level().getBlockState(ground).getFriction(this.level(), ground, this) * 0.91F;
                }

                float f1 = 0.16277137F / (f * f * f);
                f = 0.91F;
                if (this.onGround()) {
                    f = this.level().getBlockState(ground).getFriction(this.level(), ground, this) * 0.91F;
                }

                this.moveRelative(this.onGround() ? 0.1F * f1 : 0.02F, vec);
                this.move(MoverType.SELF, this.getDeltaMovement());
                this.setDeltaMovement(this.getDeltaMovement().scale((double)f));
            }
        }

        this.calculateEntityAnimation(false);
    }

     */

    public enum MoveControlMode {
        FLYING,
        GROUNDED
    }

    public enum VisualState {
        GROUNDED,
        TAKING_OFF,
        FLYING,
        LANDING
    }

    public enum BehaviorMode {
        BRAVE,
        FEARFUL
    }


/*
    private static final AttributeModifier SLOW_FALLING = new AttributeModifier(UUID.fromString("A5B6CF2A-2F7C-31EF-9022-7C3E7D5E6ABA"), "Slow falling acceleration reduction", -0.07, AttributeModifier.Operation.ADDITION); // Add -0.07 to 0.08 so we get the vanilla default of 0.01
    @Override
    public void travel(Vec3 p_21280_) {
        if (this.isControlledByLocalInstance()) {
            double d0 = 0.08D;
            AttributeInstance gravity = this.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
            boolean flag = this.getDeltaMovement().y <= 0.0D;
            if (flag && this.hasEffect(MobEffects.SLOW_FALLING)) {
                if (!gravity.hasModifier(SLOW_FALLING)) gravity.addTransientModifier(SLOW_FALLING);
            } else if (gravity.hasModifier(SLOW_FALLING)) {
                gravity.removeModifier(SLOW_FALLING);
            }
            d0 = gravity.getValue();

            FluidState fluidstate = this.level().getFluidState(this.blockPosition());
            if ((this.isInWater() || (this.isInFluidType(fluidstate) && fluidstate.getFluidType() != ForgeMod.LAVA_TYPE.get())) && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate)) {
                LOGGER.info("In water or non-lava fluid");
                if (this.isInWater() || (this.isInFluidType(fluidstate) && !this.moveInFluid(fluidstate, p_21280_, d0))) {
                    LOGGER.info("Passed second water + non-lava check");
                    double d9 = this.getY();
                    float f4 = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
                    float f5 = 0.02F;
                    float f6 = (float) EnchantmentHelper.getDepthStrider(this);
                    if (f6 > 3.0F) {
                        f6 = 3.0F;
                    }

                    if (!this.onGround()) {
                        f6 *= 0.5F;
                    }

                    if (f6 > 0.0F) {
                        f4 += (0.54600006F - f4) * f6 / 3.0F;
                        f5 += (this.getSpeed() - f5) * f6 / 3.0F;
                    }

                    if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
                        f4 = 0.96F;
                    }

                    f5 *= (float)this.getAttribute(ForgeMod.SWIM_SPEED.get()).getValue();
                    this.moveRelative(f5, p_21280_);
                    this.move(MoverType.SELF, this.getDeltaMovement());
                    Vec3 vec36 = this.getDeltaMovement();
                    if (this.horizontalCollision && this.onClimbable()) {
                        vec36 = new Vec3(vec36.x, 0.2D, vec36.z);
                    }

                    this.setDeltaMovement(vec36.multiply((double)f4, (double)0.8F, (double)f4));
                    Vec3 vec32 = this.getFluidFallingAdjustedMovement(d0, flag, this.getDeltaMovement());
                    this.setDeltaMovement(vec32);
                    if (this.horizontalCollision && this.isFree(vec32.x, vec32.y + (double)0.6F - this.getY() + d9, vec32.z)) {
                        this.setDeltaMovement(vec32.x, (double)0.3F, vec32.z);
                    }
                }
            } else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate)) {
                LOGGER.info("In lava and affected by fluids");
                double d8 = this.getY();
                this.moveRelative(0.02F, p_21280_);
                this.move(MoverType.SELF, this.getDeltaMovement());
                if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
                    LOGGER.info("Coping and seething in lava");
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, (double)0.8F, 0.5D));
                    Vec3 vec33 = this.getFluidFallingAdjustedMovement(d0, flag, this.getDeltaMovement());
                    this.setDeltaMovement(vec33);
                } else {
                    LOGGER.info("Not coping and seething in lava");
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
                }

                if (!this.isNoGravity()) {
                    LOGGER.info("Has gravity in lava. wow");
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -d0 / 4.0D, 0.0D));
                } else LOGGER.info("Has no gravity in lava. wow");

                Vec3 vec34 = this.getDeltaMovement();
                if (this.horizontalCollision && this.isFree(vec34.x, vec34.y + (double)0.6F - this.getY() + d8, vec34.z)) {
                    LOGGER.info("lava weird horizontal collision check, prolly being pushed");
                    this.setDeltaMovement(vec34.x, (double)0.3F, vec34.z);
                }
            } else if (this.isFallFlying()) {
                LOGGER.info("Fall flying");
                this.checkSlowFallDistance();
                Vec3 vec3 = this.getDeltaMovement();
                Vec3 vec31 = this.getLookAngle();
                float f = this.getXRot() * ((float)Math.PI / 180F);
                double d1 = Math.sqrt(vec31.x * vec31.x + vec31.z * vec31.z);
                double d3 = vec3.horizontalDistance();
                double d4 = vec31.length();
                double d5 = Math.cos((double)f);
                d5 = d5 * d5 * Math.min(1.0D, d4 / 0.4D);
                vec3 = this.getDeltaMovement().add(0.0D, d0 * (-1.0D + d5 * 0.75D), 0.0D);
                if (vec3.y < 0.0D && d1 > 0.0D) {
                    LOGGER.info("First bullshit vec check passed");
                    double d6 = vec3.y * -0.1D * d5;
                    vec3 = vec3.add(vec31.x * d6 / d1, d6, vec31.z * d6 / d1);
                } else LOGGER.info("First bullshit vec check failed");

                if (f < 0.0F && d1 > 0.0D) {
                    LOGGER.info("Second bullshit vec check passed");
                    double d10 = d3 * (double)(-Mth.sin(f)) * 0.04D;
                    vec3 = vec3.add(-vec31.x * d10 / d1, d10 * 3.2D, -vec31.z * d10 / d1);
                } LOGGER.info("Second bullshit vec check failed");

                if (d1 > 0.0D) {
                    LOGGER.info("Third bullshit vec check passed");
                    vec3 = vec3.add((vec31.x / d1 * d3 - vec3.x) * 0.1D, 0.0D, (vec31.z / d1 * d3 - vec3.z) * 0.1D);
                } else LOGGER.info("Third bullshit vec check failed");

                this.setDeltaMovement(vec3.multiply((double)0.99F, (double)0.98F, (double)0.99F));
                this.move(MoverType.SELF, this.getDeltaMovement());
                if (this.horizontalCollision && !this.level().isClientSide) {
                    LOGGER.info("Fourth vec check passed");
                    double d11 = this.getDeltaMovement().horizontalDistance();
                    double d7 = d3 - d11;
                    float f1 = (float)(d7 * 10.0D - 3.0D);
                    if (f1 > 0.0F) {
                        LOGGER.info("Embedded fourth vec check passed");
                        this.playSound(ModSounds.MEH_CHEERING.get(), 1.0F, 1.0F);
                        this.hurt(this.damageSources().flyIntoWall(), f1);
                    } else LOGGER.info("Embedded fourth vec check failed");
                } else LOGGER.info("Forth vec check failed");

                if (this.onGround() && !this.level().isClientSide) {
                    LOGGER.info("Weird flag being set");
                    this.setSharedFlag(7, false);
                } LOGGER.info("Weird flag not being set");
            } else {
                LOGGER.info("The everything else section");
                LOGGER.info(String.format("Pre-friction state: deltaMovement=%s, onGround=%s, noGravity=%s", getDeltaMovement(), onGround(), isNoGravity()));
                BlockPos blockpos = this.getBlockPosBelowThatAffectsMyMovement();
                float f2 = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getFriction(level(), this.getBlockPosBelowThatAffectsMyMovement(), this);
                float f3 = this.onGround() ? f2 * 0.91F : 0.91F;
                Vec3 vec35 = this.handleRelativeFrictionAndCalculateMovement(p_21280_, f2);
                double d2 = vec35.y;
                if (this.hasEffect(MobEffects.LEVITATION)) {
                    LOGGER.info("Has levitation");
                    d2 += (0.05D * (double)(this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - vec35.y) * 0.2D;
                } else if (this.level().isClientSide && !this.level().hasChunkAt(blockpos)) {
                    LOGGER.info("Whatever this clientside chunk check thing is");
                    if (this.getY() > (double)this.level().getMinBuildHeight()) {
                        d2 = -0.1D;
                    } else {
                        d2 = 0.0D;
                    }
                } else if (!this.isNoGravity()) {
                    LOGGER.info("Has gravity");
                    d2 -= d0;
                } else LOGGER.info("None of the previous checks passed");

                if (this.shouldDiscardFriction()) {
                    LOGGER.info("Discarding friction");
                    this.setDeltaMovement(vec35.x, d2, vec35.z);
                } else {
                    LOGGER.info(String.format("Not discarding friction, vec: %s f: %.1f", vec35, f3));
                    this.setDeltaMovement(vec35.x * (double)f3, d2 * (double)0.98F, vec35.z * (double)f3);
                }
            }
        }

        this.calculateEntityAnimation(this instanceof FlyingAnimal);
    }

    @Override
    public void moveRelative(float speed, Vec3 vec) {
        float yRot = getYRot();
        Vec3 inputVec = getInputVector(vec, speed, yRot);
        Vec3 deltaBefore = getDeltaMovement();
        Vec3 deltaAfter = deltaBefore.add(inputVec);

        LOGGER.info(String.format("Moving relative... speed: %.3f, vec: %s, yRot: %s, Flying: %s, Attrs... Base: %.3f, Total: %.3f, " +
                        "inputVec: %s, deltaBefore: %s, deltaAfter: %s",
                speed,
                vec,
                yRot,
                isFlying(),
                getAttributeBaseValue(Attributes.MOVEMENT_SPEED),
                getAttributeValue(Attributes.MOVEMENT_SPEED),

                inputVec,
                deltaBefore,
                deltaAfter
        ));
        this.setDeltaMovement(deltaAfter);
    }

    // mixin dodging. maybe it works? lol
    public Vec3 getInputVector(Vec3 inputVec, float speed, float yRot) {
        double inputLength = inputVec.lengthSqr();
        if (inputLength < 1.0E-7D) {
            LOGGER.info("Input length is so tiny that we are stopping");
            return Vec3.ZERO;
        }

        Vec3 normalizedOrLess = inputLength > 1 ? inputVec.normalize() : inputVec;
        Vec3 speedScaled = normalizedOrLess.scale(speed);

        float radians = (float)Math.toRadians(yRot);
        float sin = (float)Math.sin(radians);
        float cos = (float)Math.cos(radians);

        Vec3 result = new Vec3(speedScaled.x * cos - speedScaled.z * sin, speedScaled.y, speedScaled.z * cos + speedScaled.x * sin);

        LOGGER.info(String.format("Getting input vector. Args... inputVec: %s, speed: %s, yRot: %s Results... normalizedOrLess: %s, speedScaled: %s, result: %s",
                inputVec, speed, yRot, normalizedOrLess, speedScaled, result));

        return result;
    }

 */
}
