package com.robotemployee.reu.foliant.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.robotemployee.reu.foliant.entity.DevilEntity;
import com.robotemployee.reu.foliant.model.DevilEntityModel;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.RenderTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class DevilRenderer extends FoliantRenderer<DevilEntity> {

    public static final ResourceLocation WINGS_TEXTURE = new ResourceLocation(RobotEmployeeUtils.MODID, "textures/entity/devil/devilwings.png");

    public static final ResourceLocation BEAM_TEXTURE = new ResourceLocation(RobotEmployeeUtils.MODID, "textures/entity/devil/devilbeam.png");

    public DevilRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DevilEntityModel());
    }

    // Render ourselves when the midpoint is rendering
    @Override
    public boolean shouldRender(@NotNull DevilEntity devil, @NotNull Frustum frustum, double x, double y, double z) {
        if (super.shouldRender(devil, frustum, x, y, z)) return true;
        // only if we aren't already rendering, check if the target is rendering

        LivingEntity target = devil.getProtectionTarget();
        if (target == null) return false;

        // potentially volatile, if this function errors / crashes this cast should be the prime suspect
        EntityRenderer<LivingEntity> targetRenderer = (EntityRenderer<LivingEntity>) entityRenderDispatcher.getRenderer(target);
        boolean targetIsRendering = targetRenderer.shouldRender(target, frustum, target.getX(), target.getY(), target.getZ());
        if (targetIsRendering) return true;

        // so the target isn't rendering... this works?
        // no!! it is possible to have neither entity on-screen while the beam should be visible... as such, we should also check points on the beam

        // we will move the target to the in between points
        Vec3 targetOriginalPosition = target.position();
        int inBetweenChecks = 3;
        for (int i = 0; i < inBetweenChecks; i++) {
            Vec3 inBetweenPoint = target.position().vectorTo(devil.position()).scale((i + 1) / (float)(inBetweenChecks + 1)).add(target.position());
            target.moveTo(inBetweenPoint);
            if (targetRenderer.shouldRender(target, frustum, target.getX(), target.getY(), target.getZ())) {
                target.moveTo(targetOriginalPosition);
                return true;
            }
        }

        target.moveTo(targetOriginalPosition);
        return false;
    }

    @Override
    public void render(@NotNull DevilEntity devil, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        // render the devil body
        poseStack.pushPose();
        float posYOffset = (float) (devil.getBoundingBox().getYsize() * 0.5);
        poseStack.translate(0, posYOffset, 0);
        super.render(devil, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        final float scale = 2f;

        // now for the wings

        renderWings(devil, entityYaw, partialTick, poseStack, bufferSource, packedLight, scale);

        if (devil.isProtecting() && !devil.isDeadOrDying()) {
            LivingEntity target = devil.getProtectionTarget();
            renderProtectionBeam(devil, target, partialTick, poseStack, bufferSource, packedLight);
        }

        // all done
        poseStack.popPose();
    }

    void renderWings(@NotNull DevilEntity entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, float scale) {
        RenderTools.renderCameraFacing2DTexture(
                entity.getPosition(partialTick).toVector3f(),
                entityRenderDispatcher.camera.getPosition().toVector3f(),
                RenderType.entityCutoutNoCull(WINGS_TEXTURE),
                poseStack,
                bufferSource,
                scale,
                new Vector2f(0, 0),
                new Vector2f(1, 1)
        );

    }
        /*Vector3f vecToCam = entity.getPosition(partialTick).vectorTo(entityRenderDispatcher.camera.getPosition()).normalize().toVector3f();
        Vector3f vecToCamHorizontal = new Vector3f(vecToCam).mul(1, 0, 1);
        Vector3f normalizedVecToCamHorizontal = new Vector3f(vecToCamHorizontal).normalize();
        float horizontalDistance = vecToCamHorizontal.length();
        float yaw = (float) Math.atan2(normalizedVecToCamHorizontal.x, normalizedVecToCamHorizontal.z);
        Vector2f normalizedPitchTriangleLegs = new Vector2f(horizontalDistance, vecToCam.y).normalize();
        float pitch = (float) Math.atan2(normalizedPitchTriangleLegs.y, normalizedPitchTriangleLegs.x);

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
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, -size, size, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, size, size, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, size, -size, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        poseStack.popPose();
    }
         */

    protected static final float BEAM_WIDTH = 0.5f;
    protected static final float STRETCH_FACTOR = 2f;
    protected static final float MAX_SCROLL_SPEED_MULTIPLIER = 2;
    protected static final int SCROLL_SPEED_INCREASE_MAX_DISTANCE = 24;

    public void renderProtectionBeam(@NotNull LivingEntity fromEntity, @NotNull LivingEntity toEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        Vector3f emanatingFrom = fromEntity.getPosition(partialTick).toVector3f();
        Vector3f receiving = toEntity.getPosition(partialTick).add(0, toEntity.getBbHeight() * 0.5, 0).toVector3f();
        Vector3f cameraPos = entityRenderDispatcher.camera.getPosition().toVector3f();
        float stretchFactor = STRETCH_FACTOR;
        float width = BEAM_WIDTH;
        float time = fromEntity.level().getGameTime() + partialTick;

        float distance = emanatingFrom.distance(receiving);

        // fixme equation makes it jitter when you change the thingamabob
        float scrollSpeed = distance < SCROLL_SPEED_INCREASE_MAX_DISTANCE ? MAX_SCROLL_SPEED_MULTIPLIER - ((MAX_SCROLL_SPEED_MULTIPLIER - 1) / SCROLL_SPEED_INCREASE_MAX_DISTANCE) : 1;
        RenderType renderType = RenderType.entityCutoutNoCull(BEAM_TEXTURE);


        RenderTools.renderCameraFacing2DBeamBetween(emanatingFrom, receiving, cameraPos, stretchFactor, width, time, scrollSpeed, renderType, poseStack, bufferSource, new Vector2f(0, 0), new Vector2f(1, 1));
    }


}
