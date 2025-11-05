package com.robotemployee.reu.foliant.entity;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.foliant.FoliantRaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AmelieEntity extends FlyingFoliantRaidMob implements GeoEntity {

    public static final String BEHAVIOR_STATE_KEY = "BehaviorState";
    public static final SerializableDataTicket<String> BEHAVIOR_STATE = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofString(new ResourceLocation(RobotEmployeeUtils.MODID, "behavior_state")));
    public static final SerializableDataTicket<Integer> IS_DODGING_LEFT = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofInt(new ResourceLocation(RobotEmployeeUtils.MODID, "dodge_direction")));

    // this is just a bunch of mutually exclusive behaviors, to make it easier to synchronize stuff logically.
    protected BehaviorState behaviorState = BehaviorState.IDLE;

    // dodging increments consecutiveDodges.
    // ticksWithoutDodge goes up every tick if consecutiveDodges is above 0. if it's 0, it does nothing because it doesn't matter.
    // once it reaches the consecutive dodge timeout, it resets consecutiveDodges to 0.
    protected int consecutiveDodges = 0;
    protected int ticksWithoutDodge = 0;
    public static final int MAX_CONSECUTIVE_DODGES = 2;
    public static final int CONSECUTIVE_DODGE_TIMEOUT = 60;



    public AmelieEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public FoliantRaid.EnemyType getEnemyType() {
        return FoliantRaid.EnemyType.AMELIE;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        int goalIndex = 0;
        int targetIndex = 0;

        goalSelector.addGoal(goalIndex++, new AmelieStrafingRunGoal(this));

        targetSelector.addGoal(targetIndex++, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.FLYING_SPEED, 4)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(BEHAVIOR_STATE_KEY, getBehaviorState().toString());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBehaviorState(BehaviorState.valueOf(tag.getString(BEHAVIOR_STATE_KEY)));
    }

    // i did say i encapsulate a lot. it makes me happy
    public boolean shouldBeakSpin() {
        return !isDodging();
    }

    public boolean isIdle() {
        return getBehaviorState() == BehaviorState.IDLE;
    }

    public boolean isTaunting() {
        return getBehaviorState() == BehaviorState.TAUNTING;
    }

    public boolean isFiringWeapon() {
        return getBehaviorState() == BehaviorState.FIRING;
    }

    public boolean isSpoolingWeapon() {
        return getBehaviorState() == BehaviorState.SPOOLING_UP;
    }

    public boolean isDodging() {
        return getBehaviorState() == BehaviorState.DODGING;
    }

    public boolean shouldBeakOpen() {
        return isFiringWeapon() || isSpoolingWeapon();
    }

    public boolean canDodge() {
        boolean goodState = isSpoolingWeapon() || isIdle();
        boolean notTooManyInARow = consecutiveDodges < MAX_CONSECUTIVE_DODGES;
        return goodState && notTooManyInARow;
    }

    // note that this only plays the animation. must be called on the server
    public void dodge(boolean isDodgingLeft) {
        consecutiveDodges++;
        ticksWithoutDodge = 0;

        Vector3f addedMovement = new Vector3f(1, 0, 0).rotateY((float)Math.toRadians(getYRot() + (isDodgingLeft ? -90 : 90)));
        addDeltaMovement(new Vec3(addedMovement));
        setBehaviorState(BehaviorState.DODGING);
        setAnimData(IS_DODGING_LEFT, isDodgingLeft ? -1 : 1);
        triggerAnim("Dodge", "Dodge");
    }

    public BehaviorState getBehaviorState() {

        if (level().isClientSide()) {
            String state = getAnimData(BEHAVIOR_STATE);
            if (state == null) behaviorState = BehaviorState.IDLE;
            else behaviorState = BehaviorState.valueOf(state);
        }

        return behaviorState;
    }

    protected long timestampOfLastBehaviorChange = 0;
    public long getTimestampOfLastBehaviorChange() {
        return timestampOfLastBehaviorChange;
    }
    public void setBehaviorState(BehaviorState state) {
        setAnimData(BEHAVIOR_STATE, state.name());
        behaviorState = state;
        timestampOfLastBehaviorChange = level().getGameTime();
    }

    public boolean canSmoothlyChangeBehaviorState() {
        return getBehaviorState().canSmoothlyChange(this);
    }

    public static final RawAnimation BEAK_SPIN = RawAnimation.begin().thenLoop("misc.beak_spin");
    public static final RawAnimation OPEN_BEAK = RawAnimation.begin().thenPlayAndHold("misc.open_wide");
    public static final RawAnimation DODGE = RawAnimation.begin().thenPlay("misc.dodge");
    public static final RawAnimation TAUNT = RawAnimation.begin().thenPlay("misc.taunt");
    public static final RawAnimation FACE_BILLBOARD = RawAnimation.begin().thenPlay("misc.face_billboard");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "BeakSpin", 10, state -> {
            AmelieEntity animatable = state.getAnimatable();
            if (!animatable.shouldBeakSpin()) return PlayState.STOP;

            return state.setAndContinue(BEAK_SPIN);
        }));

        // doing it this way instead of using my RenderUtils billboard thing, because im really lazy :)
        controllers.add(new AnimationController<>(this, "FaceBillboard", 10, state -> {
            return state.setAndContinue(FACE_BILLBOARD);
        }));

        controllers.add(new AnimationController<>(this, "BeakOpen", 10, state -> {
            AmelieEntity animatable = state.getAnimatable();
            if (!animatable.shouldBeakOpen()) return PlayState.STOP;

            return state.setAndContinue(OPEN_BEAK);
        }));


        controllers.add(new AnimationController<>(this, "Dodge", 10, state -> {
            return PlayState.CONTINUE;
        }).triggerableAnim("dodge", DODGE));
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public enum BehaviorState {
        IDLE(-1),
        SPOOLING_UP(60),
        FIRING(15),
        DODGING(15),
        TAUNTING(40);

        final int ticks;
        BehaviorState(int ticks) {
            this.ticks = ticks;
        }

        public boolean canSmoothlyChange(AmelieEntity amelie) {
            long timestamp = amelie.getTimestampOfLastBehaviorChange();
            long currentTime = amelie.level().getGameTime();
            return ticks < 0 || currentTime - timestamp > ticks;
        }

        public int getTicks() {
            return ticks;
        }
    }

    public static class AmelieStrafingRunGoal extends Goal {

        // only activates when amelie has a target

        public final AmelieEntity amelie;

        public AmelieStrafingRunGoal(AmelieEntity amelie) {
            this.amelie = amelie;
        }

        @Override
        public boolean canUse() {
            return false;
        }
    }
}
