package com.robotemployee.reu.banana.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.robotemployee.reu.banana.entity.DevilEntity;
import com.robotemployee.reu.banana.model.DevilEntityModel;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@OnlyIn(Dist.CLIENT)
public class DevilRenderer extends BananaRenderer<DevilEntity> {

    public static final ResourceLocation WINGS_TEXTURE = new ResourceLocation(RobotEmployeeUtils.MODID, "textures/entity/devil/devilwings.png");

    public static final ResourceLocation BEAM_TEXTURE = new ResourceLocation(RobotEmployeeUtils.MODID, "textures/entity/devil/devilbeam.png");

    public DevilRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DevilEntityModel());
    }

    @Override
    public boolean shouldRender(@NotNull DevilEntity devil, @NotNull Frustum frustum, double x, double y, double z) {
        // i know this approach can cause the potentially expensive shouldRender() function to be called twice, but whatever
        if (super.shouldRender(devil, frustum, x, y, z)) return true;
        LivingEntity target = devil.getProtectionTarget();
        if (target == null) return false;

        boolean targetIsRendering = entityRenderDispatcher.getRenderer(target).shouldRender(target, frustum, target.getX(), target.getY(), target.getZ());
        if (targetIsRendering) return true;
        Vec3 midpoint = target.position().vectorTo(devil.position()).scale(0.5).add(target.position());
        boolean midpointIsRendering = entityRenderDispatcher.getRenderer(target).shouldRender(target, frustum, midpoint.x, midpoint.y, midpoint.z);
        if (midpointIsRendering) return true;

        return false;
    }

    @Override
    public void render(@NotNull DevilEntity banana, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        // render the devil body
        poseStack.pushPose();
        float posYOffset = (float) (banana.getBoundingBox().getYsize() * 0.5);
        poseStack.translate(0, posYOffset, 0);
        super.render(banana, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        final float scale = 2f;

        // now for the wings

        Vector3f vecToCam = banana.getPosition(partialTick).vectorTo(entityRenderDispatcher.camera.getPosition()).normalize().toVector3f();
        Vector3f vecToCamHorizontal = new Vector3f(vecToCam).mul(1, 0, 1);
        Vector3f normalizedVecToCamHorizontal = new Vector3f(vecToCamHorizontal).normalize();
        float horizontalDistance = vecToCamHorizontal.length();
        float yaw = (float) Math.atan2(normalizedVecToCamHorizontal.x, normalizedVecToCamHorizontal.z);
        Vector2f normalizedPitchTriangleLegs = new Vector2f(horizontalDistance, vecToCam.y).normalize();
        float pitch = (float) Math.atan2(normalizedPitchTriangleLegs.y, normalizedPitchTriangleLegs.x);

        renderWings(banana, entityYaw, partialTick, poseStack, bufferSource, packedLight, yaw, pitch, scale);

        if (banana.isProtecting()) {
            LivingEntity target = banana.getProtectionTarget();
            renderProtectionBeam(banana, target, partialTick, poseStack, bufferSource, packedLight);
        }

        // all done
        poseStack.popPose();
    }

    void renderWings(@NotNull DevilEntity entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, float yaw, float pitch, float scale) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotation(yaw));
        poseStack.mulPose(Axis.XP.rotation(-pitch));
        poseStack.scale(scale, scale, scale);

        VertexConsumer vertexes = bufferSource.getBuffer(RenderType.entityCutoutNoCull(WINGS_TEXTURE));

        float size = 0.5f;

        Matrix4f lastPose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        vertexes.vertex(lastPose, -size, -size, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLightFor2Ds)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, -size, size, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLightFor2Ds)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, size, size, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLightFor2Ds)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, size, -size, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLightFor2Ds)
                .normal(normal, 0, 1, 0)
                .endVertex();

        poseStack.popPose();
    }

    // the beam will be rendered by the entity being protected if the Devil isn't on screen
    protected static final float BEAM_WIDTH = 0.5f;
    protected static final float STRETCH_FACTOR = 2f;

    public void renderProtectionBeam(@NotNull LivingEntity fromEntity, @NotNull LivingEntity toEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        //LOGGER.info("attempting to render the beam");

        float posYOffset = (float) (fromEntity.getBoundingBox().getYsize() * 0.5);
        // swapping the starts and ends on purpose, so that the recipient appears to "slide" along the beam instead of the other way around
        // due to the UV repeating stuff
        Vector3f endPosition = fromEntity.getPosition(partialTick).toVector3f().add(0, posYOffset, 0);
        Vector3f startPosition = toEntity.getPosition(partialTick).toVector3f().add(0, (float)(toEntity.getBoundingBox().getYsize() * 0.5), 0);

        Vector3f diff = new Vector3f(endPosition).sub(startPosition);
        // refer to the above UV repeating comment for why I'm doing this
        poseStack.translate(-diff.x, -diff.y, -diff.z);
        Vector2f hDiff = new Vector2f(diff.x, diff.z);
        Vector2f normalizedHDiff = new Vector2f(hDiff).normalize();
        Vector3f normalizedDiff = new Vector3f(diff).normalize();

        float yaw = (float) Math.atan2(normalizedHDiff.y, normalizedHDiff.x) + (float)(Math.PI / 2);
        float pitch = (float) Math.atan2(diff.y, hDiff.length());

        Vector3f vecToCam = fromEntity.getPosition(partialTick).vectorTo(entityRenderDispatcher.camera.getPosition()).normalize().toVector3f();

        Vector2f hVecToCam = new Vector2f(vecToCam.x, vecToCam.z);

        Vector2f normalizedPitchTriangleLegs = new Vector2f(hVecToCam.length(), vecToCam.y).normalize();
        float pitchToCam = (float) Math.atan2(normalizedPitchTriangleLegs.y, normalizedPitchTriangleLegs.x);

        poseStack.pushPose();

        //poseStack.translate(startPosition.x, startPosition.y, startPosition.z);

        poseStack.mulPose(Axis.YP.rotation(-yaw));
        poseStack.mulPose(Axis.XP.rotation(-(float)Math.PI / 2));
        poseStack.mulPose(Axis.XP.rotation(pitch));
        poseStack.mulPose(Axis.YP.rotation(pitchToCam));

        float length = diff.length();
        // for the V coordinate of the UVs on the toEntity side of the beam
        float endV = length / STRETCH_FACTOR;

        VertexConsumer vertexes = bufferSource.getBuffer(RenderType.entityTranslucent(BEAM_TEXTURE));

        Matrix4f lastPose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        vertexes.vertex(lastPose, -BEAM_WIDTH, 0, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(0, endV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLightFor2Ds)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, -BEAM_WIDTH, length, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLightFor2Ds)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, BEAM_WIDTH, length, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLightFor2Ds)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, BEAM_WIDTH, 0, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, endV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLightFor2Ds)
                .normal(normal, 0, 1, 0)
                .endVertex();


        poseStack.popPose();
    }
}
