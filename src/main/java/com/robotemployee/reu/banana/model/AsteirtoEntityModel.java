package com.robotemployee.reu.banana.model;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.banana.entity.AsteirtoEntity;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

@OnlyIn(Dist.CLIENT)
public class AsteirtoEntityModel extends GeoModel<AsteirtoEntity> {

    static final Logger LOGGER = LogUtils.getLogger();

    // todo
    @Override
    public ResourceLocation getModelResource(AsteirtoEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "geo/asteirto.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AsteirtoEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "textures/entity/asteirto/rosepike.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AsteirtoEntity animatable) {
        return new ResourceLocation(RobotEmployeeUtils.MODID, "animations/asteirto.animation.json");
    }

    @Override
    public void setCustomAnimations(AsteirtoEntity animatable, long instanceId, AnimationState<AsteirtoEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        roseFaceCamera(animatable, instanceId, animationState);
    }

    protected void roseFaceCamera(AsteirtoEntity animatable, long instanceId, AnimationState<AsteirtoEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        CoreGeoBone bone = getAnimationProcessor().getBone("rose");

        if (bone == null) return;

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();



        Vec3 globalBonePos = new Vec3(bone.getPosX(), bone.getPosY(), bone.getPosZ())
                .add(0, 21.5, 0)
                .scale(1/16f)
                .add(animatable.getPosition(animationState.getPartialTick()));

        Vec3 vecToCam = globalBonePos
                .vectorTo(cameraPos)
                .normalize();

        float horizontalDistance = (float)vecToCam.horizontalDistance();

        float yaw = (float) Math.atan2(vecToCam.z(), vecToCam.x());
        float pitch = (float) Math.atan2(vecToCam.y(), horizontalDistance);

        float counterYaw = (float)Math.toRadians(animatable.getYRot());

        bone.setRotY((float)Math.PI / 2 - yaw + counterYaw);
        bone.setRotX(pitch);
    }


}
