package com.robotemployee.reu.banana.entity;

import com.robotemployee.reu.banana.BananaRaid;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

public class AsteirtoEntity extends FlyingBananaRaidMob implements GeoEntity {
    public AsteirtoEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.navigation = new FlyingPathNavigation(this, level);
    }

    // todo
    // create the renderer and insure that it works with the custom UV params
    // but fix greg and the devil first

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

    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.FLYING_SPEED, 2)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return null;
    }
}
