package com.robotemployee.reu.foliant.entity;

import com.robotemployee.reu.foliant.FoliantRaid;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class AmelieEntity extends FlyingFoliantRaidMob implements GeoEntity {
    public AmelieEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public FoliantRaid.EnemyType getEnemyType() {
        return FoliantRaid.EnemyType.AMELIE;
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.FLYING_SPEED, 4)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }
}
