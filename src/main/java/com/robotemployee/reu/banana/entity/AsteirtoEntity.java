package com.robotemployee.reu.banana.entity;

import com.robotemployee.reu.banana.BananaRaid;
import com.robotemployee.reu.banana.entity.sound.EntitySoundInstanceThatTicks;
import com.robotemployee.reu.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AsteirtoEntity extends FlyingBananaRaidMob implements GeoEntity {

    public static final int MAX_OWNED_TEMFUR_TEMFURS = 16;
    public static final float RECYCLE_RANGE = 64;
    public AsteirtoEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.navigation = new FlyingPathNavigation(this, level);
    }

    // todo
    // obviously implement this
    // should probably start with temfur temfur
    // also, create a sounds.json entry for the hum
    // make sure it works
    // and replace greg's logic with the same stuff that is used for this one
    // good luck
    // - 4:25AM carlos wearing the uncomfortable white palm tree shirt

    @OnlyIn(Dist.CLIENT)
    protected EntitySoundInstanceThatTicks<AsteirtoEntity> ambient_sound = null;

    @Override
    public float getAirliftWeight() {
        return 0;
    }

    @Override
    public boolean canRecycle() {
        return false;
    }

    @Override
    public float getPresenceImportance() {
        return 8;
    }

    @Override
    public BananaRaid.EnemyType getBananaType() {
        return BananaRaid.EnemyType.ASTEIRTO;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericFlyController(this));
    }


    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.33)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.FLYING_SPEED, 2)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }


    @Override
    public void push(Entity entity) {
        //super.push(entity);
    }


    @Override
    public void baseTick() {
        super.baseTick();
        if (canStartAmbientSound()) {
            startNewAmbientSound();
        }
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

}
