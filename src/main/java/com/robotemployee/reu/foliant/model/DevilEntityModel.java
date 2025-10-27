package com.robotemployee.reu.foliant.model;

import com.robotemployee.reu.foliant.entity.DevilEntity;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.model.GeoModel;

@OnlyIn(Dist.CLIENT)
public class DevilEntityModel extends GeoModel<DevilEntity> {

    @Override
    public ResourceLocation getModelResource(DevilEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "geo/devil.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DevilEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "textures/entity/devil/devil.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DevilEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "animations/devil.animation.json");
    }
}
