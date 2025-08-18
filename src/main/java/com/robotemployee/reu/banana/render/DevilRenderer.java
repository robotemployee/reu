package com.robotemployee.reu.banana.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.robotemployee.reu.banana.entity.DevilEntity;
import com.robotemployee.reu.banana.model.DevilEntityModel;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.MathTools;
import com.robotemployee.reu.util.RenderTools;
import com.supermartijn642.core.render.RenderUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
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

import java.text.NumberFormat;

@OnlyIn(Dist.CLIENT)
public class DevilRenderer extends BananaRenderer<DevilEntity> {

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
        // no!! it is possible to have neither entity on-screen while the beam should be visible... as such, we should also check the midpoint

        // we will move the target to the midpoint and move it back after
        Vec3 midpoint = target.position().vectorTo(devil.position()).scale(0.5).add(target.position());
        Vec3 targetOriginalPosition = target.position();
        target.moveTo(midpoint);
        boolean midpointIsRendering = targetRenderer.shouldRender(target, frustum, target.getX(), target.getY(), target.getZ());
        // i know you can just return midpointIsRendering after doing the move back to the original position, but I am structuring it this way so that it retains
        // the "if it reaches the bottom of the function we shouldn't render"
        if (midpointIsRendering) {
            target.moveTo(targetOriginalPosition);
            return true;
        }

        target.moveTo(targetOriginalPosition);
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



        renderWings(banana, entityYaw, partialTick, poseStack, bufferSource, packedLight, scale);

        if (banana.isProtecting()) {
            LivingEntity target = banana.getProtectionTarget();
            renderProtectionBeam(banana, target, partialTick, poseStack, bufferSource, packedLight);
        }

        // all done
        poseStack.popPose();
    }

    void renderWings(@NotNull DevilEntity entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, float scale) {

        Vector3f vecToCam = entity.getPosition(partialTick).vectorTo(entityRenderDispatcher.camera.getPosition()).normalize().toVector3f();
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

    // the beam will be rendered by the entity being protected if the Devil isn't on screen
    // todo: make this function go from 2 positions and more generic so anything can use it, and move it to the superclass
    protected static final float BEAM_WIDTH = 0.5f;
    protected static final float STRETCH_FACTOR = 2f;
    protected static final float MAX_SCROLL_SPEED_MULTIPLIER = 2;
    protected static final int SCROLL_SPEED_INCREASE_MAX_DISTANCE = 24;

    public void renderProtectionBeam(@NotNull LivingEntity fromEntity, @NotNull LivingEntity toEntity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        //LOGGER.info("attempting to render the beam");

        float posYOffset = (float) (fromEntity.getBoundingBox().getYsize() * 0.5);
        // swapping the starts and ends on purpose, so that the recipient appears to "slide" along the beam instead of the other way around
        // due to the UV repeating stuff
        Vector3f endPosition = fromEntity.getPosition(partialTick).toVector3f().add(0, posYOffset, 0);
        Vector3f startPosition = toEntity.getPosition(partialTick).toVector3f().add(0, (float)(toEntity.getBoundingBox().getYsize() * 0.5), 0);

        Vector3f diff = new Vector3f(endPosition).sub(startPosition);
        float distance = diff.length();
        // refer to the above UV repeating comment for why I'm doing this
        poseStack.translate(-diff.x, -diff.y, -diff.z);
        Vector2f hDiff = new Vector2f(diff.x, diff.z);
        Vector2f normalizedHDiff = new Vector2f(hDiff).normalize();
        Vector3f normalizedDiff = new Vector3f(diff).normalize();

        float yaw = (float) Math.atan2(normalizedHDiff.y, normalizedHDiff.x) + (float)(Math.PI / 2);
        float pitch = (float) Math.atan2(diff.y, hDiff.length());

        // we are now going to try and angle the beam towards the camera
        Vector3f cameraPos = entityRenderDispatcher.camera.getPosition().toVector3f();

        // create a plane perpendicular to the xz axis along the beam
        // the plane is centered on fromEntity
        Vector3f planeNormal = new Vector3f(normalizedHDiff.x, 0, normalizedHDiff.y);
        Vector3f planeOrigin = new Vector3f(diff).mul(0.5f).add(startPosition);
        Vector3f vecToCam = new Vector3f(cameraPos).sub(planeOrigin);

        Vector3f planeProjectedCameraPos = MathTools.getPointProjectedToPlane(planeOrigin, planeNormal, vecToCam);

        Vector3f planeProjectedCameraDiff = new Vector3f(planeProjectedCameraPos).sub(planeOrigin);
        // we need to project our resulting vector to a normal vector pointing up because we need arctan for a signed angle
        // can't use the dot product equation because it has to be signed
        // and for that we need ourselves a right triangle
        // we are going to normalize the plane-projected camera displacement so that it's always shorter
        // than a vector going straight up. that way we can always just project it to that up vector
        Vector3f normalizedPlaneProjectedCameraDiff = new Vector3f(planeProjectedCameraDiff).normalize();

        float lengthOfModifiedUpVector = MathTools.getScalarProjection(normalizedPlaneProjectedCameraDiff, new Vector3f(0,1,0));
        Vector3f modifiedUpVector = new Vector3f(0, 1, 0).mul(lengthOfModifiedUpVector);
        // getting the length of the opposite vector to angle alone removes signage; almost there
        // we should create a second plane that is perpendicular to both the xz plane and to the one we previously created
        // then we can just check the sign of the offset of our camera to that point
        Vector3f oppositeVectorOfAngle = new Vector3f(modifiedUpVector).sub(normalizedPlaneProjectedCameraDiff);

        Vector3f secondPlaneNormal = new Vector3f(planeNormal).cross(new Vector3f(0, 1, 0)).normalize();
        boolean shouldNegatePitchToCamera = MathTools.getOffsetFromPlane(planeOrigin, secondPlaneNormal, cameraPos) < 0;

        float oppositeSideOfAngle = oppositeVectorOfAngle.length();
        Vector2f finallyWeAreHere = new Vector2f(lengthOfModifiedUpVector, oppositeSideOfAngle).normalize();
        float pitchToCamera = (float)Math.atan2(finallyWeAreHere.y, finallyWeAreHere.x) * (shouldNegatePitchToCamera ? -1 : 1);
        // christ it works
        // i am so washed at calc 3 i swear to fucking god

        // fixme logger
        // giga logger
        if (fromEntity.level().getGameTime() % 40 == 0) {
            NumberFormat noDecimal = NumberFormat.getInstance();
            noDecimal.setMaximumFractionDigits(0);
            NumberFormat decimal = NumberFormat.getInstance();
            decimal.setMaximumFractionDigits(2);
            decimal.setMinimumFractionDigits(2);
            LOGGER.info(String.format("cameraPos: %s, vecToCam:%s, planeNormal:%s, planeProjectedCameraPos: %s, planeProjectedCameraDiff: %s, nPlaneProjectedCameraDiff: %s ... opvecang: %s ... lOMUV: %.1f, mUV: %s, oSOA: %.1f, fWAH: %s, pitch: %.1f, negating: %s",
                    cameraPos.toString(noDecimal),
                    vecToCam.toString(noDecimal),
                    planeNormal.toString(decimal),
                    planeProjectedCameraPos.toString(noDecimal),
                    planeProjectedCameraDiff.toString(noDecimal),
                    normalizedPlaneProjectedCameraDiff.toString(decimal),

                    oppositeVectorOfAngle.toString(decimal),

                    lengthOfModifiedUpVector,
                    modifiedUpVector.toString(decimal),
                    oppositeSideOfAngle,
                    finallyWeAreHere.toString(decimal),
                    Math.toDegrees(pitchToCamera),
                    shouldNegatePitchToCamera
            ));
        }

        // begin the actual rendering
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotation(-yaw));
        poseStack.mulPose(Axis.XP.rotation(-(float)Math.PI / 2));
        poseStack.mulPose(Axis.XP.rotation(pitch));
        poseStack.mulPose(Axis.YP.rotation(pitchToCamera));

        float length = diff.length();

        // fixme equation makes it jitter when you change the thingamabob
        // scrolling effect!!!
        float scrollSpeed = distance < SCROLL_SPEED_INCREASE_MAX_DISTANCE ? MAX_SCROLL_SPEED_MULTIPLIER - ((MAX_SCROLL_SPEED_MULTIPLIER - 1) / SCROLL_SPEED_INCREASE_MAX_DISTANCE) : 1;
        final float time = fromEntity.level().getGameTime() + partialTick;
        float scrollTime = time % (20 / scrollSpeed);
        float uvModifier = -(scrollTime / (20 / scrollSpeed));
        // for the V coordinate of the UVs on the toEntity side of the beam
        float startV = 0 + uvModifier;
        float endV = length / STRETCH_FACTOR + uvModifier;

        VertexConsumer vertexes = bufferSource.getBuffer(RenderType.entityTranslucent(BEAM_TEXTURE));

        Matrix4f lastPose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        vertexes.vertex(lastPose, -BEAM_WIDTH, 0, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(0, endV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, -BEAM_WIDTH, length, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(0, startV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, BEAM_WIDTH, length, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, startV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, BEAM_WIDTH, 0, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, endV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();


        poseStack.popPose();
    }


}
