package com.robotemployee.reu.banana.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.robotemployee.reu.banana.entity.DevilEntity;
import com.robotemployee.reu.banana.model.DevilEntityModel;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3f;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@OnlyIn(Dist.CLIENT)
public class DevilRenderer extends GeoEntityRenderer<DevilEntity> {

    public static final ResourceLocation WINGS_TEXTURE = new ResourceLocation(RobotEmployeeUtils.MODID, "textures/entity/devil/devilwings.png");
    public DevilRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DevilEntityModel());
    }

    @Override
    public void render(@NotNull DevilEntity entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        // render the devil body
        poseStack.pushPose();
        poseStack.translate(0, entity.getBoundingBox().getYsize() * 0.5, 0);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        final float scale = 2f;

        // now for the wings

        poseStack.pushPose();

        Vector3f vecToCam = entity.getPosition(partialTick).vectorTo(entityRenderDispatcher.camera.getPosition()).normalize().toVector3f();
        Vector3f vecToCamHorizontal = new Vector3f(vecToCam).mul(1, 0, 1);
        Vector3f normalizedVecToCamHorizontal = new Vector3f(vecToCamHorizontal).normalize();
        float horizontalDistance = vecToCamHorizontal.length();
        float yaw = (float) Math.atan2(normalizedVecToCamHorizontal.x, normalizedVecToCamHorizontal.z);
        Vector2f what = new Vector2f(horizontalDistance, vecToCam.y).normalize();
        float pitch = (float) Math.atan2(what.y, what.x);

        poseStack.mulPose(Axis.YP.rotation(yaw));
        poseStack.mulPose(Axis.XP.rotation(-pitch));
        poseStack.scale(scale, scale, scale);

        VertexConsumer vertexes = bufferSource.getBuffer(RenderType.entityCutoutNoCull(WINGS_TEXTURE));

        float size = 0.5f;

        vertexes.vertex(poseStack.last().pose(), -size, -size, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();

        vertexes.vertex(poseStack.last().pose(), -size, size, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();

        vertexes.vertex(poseStack.last().pose(), size, size, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();

        vertexes.vertex(poseStack.last().pose(), size, -size, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(poseStack.last().normal(), 0, 1, 0)
                .endVertex();

        poseStack.popPose();
        poseStack.popPose();
    }
}
