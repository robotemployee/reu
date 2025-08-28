package com.robotemployee.reu.banana.model;

import com.robotemployee.reu.banana.entity.GregEntity;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.model.GeoModel;

// todo make greg entity
@OnlyIn(Dist.CLIENT)
public class GregEntityModel extends GeoModel<GregEntity> {

    @Override
    public ResourceLocation getModelResource(GregEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "geo/greg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GregEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "textures/entity/greg/greg.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GregEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "animations/greg.animation.json");
    }
}
