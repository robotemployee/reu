package com.robotemployee.reu.banana.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robotemployee.reu.banana.entity.AsteirtoEntity;
import com.robotemployee.reu.banana.model.AsteirtoEntityModel;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.RenderTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector2f;
import oshi.util.tuples.Pair;
import software.bernie.geckolib.model.GeoModel;

public class AsteirtoRenderer extends BananaRenderer<AsteirtoEntity> {


    public static final ResourceLocation TEXTURE = new ResourceLocation(RobotEmployeeUtils.MODID, "textures/entity/asteirto/rosepike.png");
    public static final Pair<Vector2f, Vector2f> ROSE_UVS = new Pair<>(new Vector2f(0.296875f, 0), new Vector2f(1, 0.703125f));

    public AsteirtoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AsteirtoEntityModel());
    }

    @Override
    public void render(AsteirtoEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

    }

    public void renderRose(AsteirtoEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        RenderTools.renderCameraFacing2DTexture(
                entity.getPosition(partialTick).toVector3f(),
                entityRenderDispatcher.camera.getPosition().toVector3f(),
                RenderType.entityCutoutNoCull(TEXTURE),
                poseStack,
                bufferSource,
                1,
                ROSE_UVS.getA(),
                ROSE_UVS.getB()
        );
    }
}
