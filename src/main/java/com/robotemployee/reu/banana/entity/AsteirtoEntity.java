package com.robotemployee.reu.banana.entity;

import com.robotemployee.reu.banana.BananaRaid;
import com.robotemployee.reu.banana.entity.sound.TickingSoundInstance;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AsteirtoEntity extends FlyingBananaRaidMob implements GeoEntity {

    public static final int MAX_OWNED_TEMFUR_TEMFURS = 16;
    public AsteirtoEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.navigation = new FlyingPathNavigation(this, level);
    }

    // todo

    protected TickingSoundInstance<AsteirtoEntity> ambient_sound = null;

    @Override
    public float getAirliftWeight() {
        return 0;
    }

    @Override
    public float getImportance() {
        return 0;
    }

    @Override
    public BananaRaid.EnemyTypes getBananaType() {
        return null;
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


    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void push(Entity p_21294_) {
        //super.push(p_21294_);
    }

    @Override
    public void baseTick() {
        if (canStartAmbientSound()) {

        }
    }

    public boolean canStartAmbientSound() {
        return ambient_sound == null || ambient_sound.isStopped();
    }

    public void startNewAmbientSound() {
        if (ambient_sound != null) ambient_sound.stopPlaying();

        ambient_sound = TickingSoundInstance.playAndFollow(this, ModSounds.ASTEIRTO_HUM.get(), SoundSource.HOSTILE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
