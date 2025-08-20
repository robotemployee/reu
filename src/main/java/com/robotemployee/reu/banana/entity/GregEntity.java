package com.robotemployee.reu.banana.entity;

import com.robotemployee.reu.banana.BananaRaid;
import com.robotemployee.reu.banana.entity.extra.MultiMoveControl;
import com.robotemployee.reu.banana.entity.extra.MultiPathNavigation;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
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

    public static final EntityDataAccessor<Boolean> IS_FLYING = SynchedEntityData.defineId(GregEntity.class, EntityDataSerializers.BOOLEAN);
    public static final SerializableDataTicket<String> VISUAL_STATE = SerializableDataTicket.ofString(new ResourceLocation(RobotEmployeeUtils.MODID, "visual_state"));

    private VisualState visualState = VisualState.GROUNDED;

    public GregEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        entityData.define(IS_FLYING ,false);

        // todo test these
        HashMap<MoveControlMode, MoveControl> movements = new HashMap<>();
        movements.put(MoveControlMode.FLYING, new FlyingMoveControl(this, 20, true));
        movements.put(MoveControlMode.GROUNDED, new MoveControl(this));
        moveControl = new MultiMoveControl<>(this, MoveControlMode.GROUNDED, movements);

        HashMap<MoveControlMode, PathNavigation> navigations = new HashMap<>();
        navigations.put(MoveControlMode.FLYING, new FlyingPathNavigation(this, level));
        navigations.put(MoveControlMode.GROUNDED, new GroundPathNavigation(this, level));
        navigation = new MultiPathNavigation<>(this, level, navigations);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        /*
        * move to attack but run and hide after
        * airlift allies
        * start flying if can't reach
        * */
    }

    public void startFlying() {
        setVisualState(VisualState.TAKING_OFF);
        getMultiMoveControl().setMovement(MoveControlMode.FLYING);
        getMultiPathNavigation().setNavigationLoudly(MoveControlMode.FLYING);
        entityData.set(IS_FLYING, true);
    }

    public void stopFlying() {
        setVisualState(VisualState.LANDING);
        getMultiMoveControl().setMovement(MoveControlMode.GROUNDED);
        getMultiPathNavigation().setNavigationLoudly(MoveControlMode.GROUNDED);
        entityData.set(IS_FLYING, false);
    }

    public MultiMoveControl<MoveControlMode> getMultiMoveControl() {
        return (MultiMoveControl<MoveControlMode>) moveControl;
    }

    public MultiPathNavigation<MoveControlMode> getMultiPathNavigation() {
        return (MultiPathNavigation<MoveControlMode>) navigation;
    }

    public boolean canChangeFlying() {
        VisualState state = getVisualState();
        return state != VisualState.TAKING_OFF && state != VisualState.LANDING;
    }

    public VisualState getVisualState() {
        if (level().isClientSide()) {
            visualState = VisualState.valueOf(getAnimData(VISUAL_STATE));
        }
        return visualState;
    }

    public void setVisualState(VisualState state) {
        setAnimData(VISUAL_STATE, state.name());
        visualState = state;
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

    public static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("animation.model.walk");
    public static final RawAnimation TAKE_OFF_ANIM = RawAnimation.begin().thenPlay("animation.model.start_flying");
    public static final RawAnimation FLYING_ANIM = RawAnimation.begin().thenLoop("animation.model.flying");
    public static final RawAnimation LANDING_ANIM = RawAnimation.begin().thenLoop("animation.model.stop_flying");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // walk animation
        // animation to start flying
        // then keep playing the flying animation
        // play the stop flying animation when landing
        // occasionally make it look at things with the look at animation
        controllers.add(new AnimationController<>(this, "Walking", 5, this::walkAnimController));
        controllers.add(new AnimationController<>(this, "Flying", 5, this::flyAnimController));
    }

    protected <E extends GregEntity> PlayState walkAnimController(final AnimationState<E> event) {
        if (!event.isMoving() || !event.getAnimatable().onGround()) return PlayState.STOP;
        return event.setAndContinue(WALK_ANIM);
        //return PlayState.STOP;
    }

    protected <E extends GregEntity> PlayState flyAnimController(final AnimationState<E> event) {
        switch (getVisualState()) {
            case GROUNDED -> {
                return PlayState.STOP;
            }
            case TAKING_OFF -> {
                if (event.getController().hasAnimationFinished()) {
                    setVisualState(VisualState.FLYING);
                    return event.setAndContinue(FLYING_ANIM);
                }
                return event.setAndContinue(TAKE_OFF_ANIM);
            }
            case FLYING -> {
                return event.setAndContinue(FLYING_ANIM);
            }
            case LANDING -> {
                if (event.getController().hasAnimationFinished()) {
                    setVisualState(VisualState.GROUNDED);
                    return PlayState.STOP;
                }
                return event.setAndContinue(LANDING_ANIM);
            }
            default -> {
                return PlayState.CONTINUE;
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.MOVEMENT_SPEED, 3);
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return 0;
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
}
