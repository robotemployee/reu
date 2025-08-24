package com.robotemployee.reu.banana.entity;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.banana.BananaRaid;
import com.robotemployee.reu.banana.entity.ai.MultiGoal;
import com.robotemployee.reu.banana.entity.extra.MultiMoveControl;
import com.robotemployee.reu.banana.entity.extra.MultiPathNavigation;
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
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.*;
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

    public GregEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        entityData.define(IS_FLYING ,false);

        HashMap<MoveControlMode, MoveControl> movements = new HashMap<>();
        movements.put(MoveControlMode.FLYING, new FlyingMoveControl(this, 20, true));
        movements.put(MoveControlMode.GROUNDED, new MoveControl(this));
        moveControl = new MultiMoveControl<>(this, MoveControlMode.GROUNDED, movements);

        HashMap<MoveControlMode, PathNavigation> navigations = new HashMap<>();
        navigations.put(MoveControlMode.FLYING, new FlyingPathNavigation(this, level));
        navigations.put(MoveControlMode.GROUNDED, navigation);
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


        //fixme logger
        LOGGER.info(String.format("speedInfo (%s): Flying: %s Flyspeed... Base: %.1f Mod: %.1f, Gndspeed... Base: %.1f Mod: %.1f ... Speedmod: %.1f Speed: %.1f Flyspeed: %.1f",
                level().isClientSide() ? "CLIENT" : "LEVEL",
                getMultiMoveControl().getKey() == MoveControlMode.FLYING,
                getAttributeBaseValue(Attributes.FLYING_SPEED),
                getAttribute(Attributes.FLYING_SPEED).getModifiers().stream().mapToDouble(AttributeModifier::getAmount).sum(),
                getAttributeBaseValue(Attributes.MOVEMENT_SPEED),
                getAttribute(Attributes.MOVEMENT_SPEED).getModifiers().stream().mapToDouble(AttributeModifier::getAmount).sum(),
                getMoveControl().getSpeedModifier(),
                getSpeed(),
                getFlyingSpeed()
        ));

        if (level().isClientSide()) return;
        boolean flying = isFlying();
        if (!flying && onGround() && getMoveControl().hasWanted() && getDeltaMovement().lengthSqr() < 0.05) {
            if (ticksWantedPosWithoutMoving++ >= ticksUntilJumpWhenStuck) {
                //fixme logger
                LOGGER.info("Greg jumping because stuck");
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
                    if (landing) stopFlying(false);
                    else startFlying(false);
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
        goalSelector.addGoal(goalIndex++, new GregComeToGroundAndFinishLandingGoal(this));
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
        // these are where you actually
        goalSelector.addGoal(goalIndex++, new GregStartLandingIfFlightNotNeededGoal(this));

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
        //fixme logger
        LOGGER.info("Taking off. Current time: " + level().getGameTime());
        timestampOfAnimationStarted = level().getGameTime();
        setVisualState(VisualState.TAKING_OFF);
        addDeltaMovement(new Vec3(0, 0.08, 0));

        //entityData.set(IS_FLYING, true);
        // isFlying() would be more useful if it only returned true when greg is using the flight controls and etc
    }

    // this function is called in the animation controller once greg has finished taking off
    public void startFlying(boolean quietly) {
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

    protected boolean isComingToLand;
    public boolean isComingToLand() {
        return isComingToLand;
    }
    // this function is called in the ai goals that tell greg to come to the land so it could start the landing process
    public void startLanding() {
        //BlockPos ground = LevelUtils.findSolidGroundBelowPosition(level(), blockPosition());
        //if (ground == null) ground = blockPosition().below(64);
        isComingToLand = true;
        getNavigation().stop();
    }
    // this function is called in another ai goal when greg is close enough to the ground to actually land
    public void startLandingAnim() {
        isComingToLand = false;
        timestampOfAnimationStarted = level().getGameTime();
        setVisualState(VisualState.LANDING);
    }
    // this function is called in the ai goal that tells greg to actually change its flying state when it comes to the ground
    public void stopFlying(boolean quietPathNav) {
        setSprinting(true);
        setNoGravity(false);
        setDeltaMovement(Vec3.ZERO);
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
        return !isComingToLand() && state != VisualState.TAKING_OFF && state != VisualState.LANDING;
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

    public boolean isGrounded() {
        return !getEntityData().get(IS_FLYING);
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

            boolean isInGroundMode = greg.isGrounded();
            boolean canChangeFlying = greg.canChangeFlying();
            boolean isNotAlreadyTakingOff = greg.getVisualState() != VisualState.TAKING_OFF;
            boolean isNotLanding = !greg.isComingToLand();

            boolean pathSucks = canCheckPath && canWeNotPath();
            boolean targetTooVerticallyFar = (target != null) && (target.blockPosition().getY() - greg.blockPosition().getY() > VERTICAL_DISTANCE_UNTIL_FLY_TO_REACH);

            boolean canTakeOff = isInGroundMode && canChangeFlying && isNotLanding && isNotAlreadyTakingOff;
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
            return hasTarget && navigation.createPath(generalTarget, GregEntity.MAX_PATH_LENGTH) == null;
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

    public static class GregStartLandingIfFlightNotNeededGoal extends Goal {

        public final GregEntity greg;
        public GregStartLandingIfFlightNotNeededGoal(GregEntity greg) {
            this.greg = greg;
            lastTickWhereMovingToTarget = greg.level().getGameTime();
        }

        long lastTickWhereMovingToTarget;
        static final int TICKS_UNTIL_IDLE = 100;
        static final int CREATE_PATH_COOLDOWN = 40;
        int createPathCooldownTicks = CREATE_PATH_COOLDOWN;
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
            boolean alreadyLanding = greg.isComingToLand();

            // checks for if we should based on whether we could reach the target from the ground
            boolean canCreatePath = createPathCooldownTicks-- <= 0;
            boolean isPathing = navigation.isInProgress();
            Path groundPath;
            if (canCreatePath && hasTargetPos && isPathing) {
                createPathCooldownTicks = CREATE_PATH_COOLDOWN;
                groundPath = navigation.getNavigation(MoveControlMode.GROUNDED).createPath(targetPos, 32);
            } else groundPath = null;
            boolean groundCloseToTarget = getTargetDistanceToGround() < VERTICAL_DISTANCE_UNTIL_FLY_TO_REACH;
            boolean groundPathGood = groundPath != null && groundPath.canReach() && groundCloseToTarget;

            // checks for if we have gone too long without having a targeted entity
            // we also need to either be pathing somewhere and have a good ground path
            // or not be pathing at all
            if (hasTargetEntity && (groundPathGood || !isPathing)) lastTickWhereMovingToTarget = greg.level().getGameTime();
            boolean idling = greg.level().getGameTime() - lastTickWhereMovingToTarget > TICKS_UNTIL_IDLE;

            boolean rejectingSwap = alreadyLanding;
            boolean canSwap = (canChangeFlying && isFlying) && !rejectingSwap;
            boolean shouldSwap = groundPathGood || idling;

            //LOGGER.info(String.format("GREG landcheck. " + (canSwap && shouldSwap ? "ACTIVATING" : "") + " can: %s %s%s%s, should: %s pathGood: %s idling: %s idleTime: %s", canSwap, canChangeFlying, isFlying, !rejectingSwap, shouldSwap, groundPathGood, idling, greg.level().getGameTime() - lastTickWhereMovingToTarget));
            return canSwap && shouldSwap;
        }

        public int getTargetDistanceToGround() {
            LivingEntity target = greg.getTarget();
            int referenceY = (target == null) ? greg.getBlockY() : target.getBlockY();
            BlockPos groundPos = LevelUtils.findSolidGroundBelow(greg);
            if (groundPos == null) return 128;
            // comparing the target's vertical position to the ground position below greg
            return referenceY - groundPos.getY();
        }

        @Override
        public void start() {
            /*
            MultiPathNavigation<MoveControlMode> navigation = greg.getMultiPathNavigation();
            LivingEntity gregTarget = greg.getTarget();
            BlockPos gregTargetPos = gregTarget == null ? null : gregTarget.blockPosition();
            BlockPos pathTargetPos = navigation.getTargetPos();
            BlockPos generalTargetPos = gregTargetPos == null ? pathTargetPos : gregTargetPos;
            if (generalTargetPos == null) return;
            Path path = navigation.getNavigation(MoveControlMode.GROUNDED).createPath(generalTargetPos, GregEntity.MAX_PATH_LENGTH);
            if (path == null || !path.canReach()) return;
             */
            greg.startLanding();
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }

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
            // fixme logger
            LOGGER.info("Greg coming to the ground. Detected ground pos: " + groundPos);
            //greg.addDeltaMovement(new Vec3(0, -0.03, 0));
            if (greg.getNavigation().isInProgress()) greg.getNavigation().stop();
            if (greg.getMoveControl().getWantedY() != groundPos.getY()) greg.getMoveControl().setWantedPosition(groundPos.getX(), groundPos.getY(), groundPos.getZ(), 0.7);
            if (groundPos.getX() != greg.getX() || groundPos.getY() != greg.getY()) findGround();
            if (!isCloseToLand()) return;
            if (greg.getVisualState() == VisualState.FLYING) greg.startLandingAnim();
        }
    }

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
}
