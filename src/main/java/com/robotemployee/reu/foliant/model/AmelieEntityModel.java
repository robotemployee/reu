package com.robotemployee.reu.foliant.model;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.foliant.entity.AmelieEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AmelieEntityModel extends GeoModel<AmelieEntity> {
    @Override
    public ResourceLocation getModelResource(AmelieEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "geo/amelie.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AmelieEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "textures/entity/amelie/amelie.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AmelieEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "animations/amelie.animation.json");
    }
}
