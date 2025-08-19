package com.robotemployee.reu.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class RenderTools {
    private static final ResourceLocation MARKER_TEXTURE = new ResourceLocation(RobotEmployeeUtils.MODID, "textures/misc/debug_marker.png");

    public static final int PACKED_LIGHT_FOR_2DS = LightTexture.FULL_BRIGHT;

    public static void renderMarker(Vector3f position, PoseStack poseStack, MultiBufferSource bufferSource, EntityRenderDispatcher dispatcher) {
        renderMarker(position, Colors.RED.get(), poseStack, bufferSource, dispatcher);
    }

    public static void renderMarker(Vector3f position, int color, PoseStack poseStack, MultiBufferSource bufferSource, EntityRenderDispatcher dispatcher) {
        renderMarker(position, color, 1, poseStack, bufferSource, dispatcher);
    }

    public static void renderMarker(Vector3f position, int color, float scale, PoseStack poseStack, MultiBufferSource bufferSource, EntityRenderDispatcher dispatcher) {

        Vector3f vecToCam = dispatcher.camera.getPosition().toVector3f().sub(position).normalize();
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

        VertexConsumer vertexes = bufferSource.getBuffer(RenderType.entityCutoutNoCull(MARKER_TEXTURE));

        float size = 0.5f;

        Matrix4f lastPose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();



        vertexes.vertex(lastPose, -size, -size, 0)
                .color(color)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, -size, size, 0)
                .color(color)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, size, size, 0)
                .color(color)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, size, -size, 0)
                .color(color)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        poseStack.popPose();
    }

    public static void renderCameraFacing2DBeamBetween(Vector3f emanatingPoint, Vector3f receivingPoint, Vector3f cameraPos, float stretchFactor, float width, float time, float scrollSpeed, RenderType renderType, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource) {
        // swapping the starts and ends on purpose, so that the recipient appears to "slide" along the beam instead of the other way around
        // due to the UV repeating stuff
        Vector3f endPosition = emanatingPoint;
        Vector3f startPosition = receivingPoint;

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

        // create a plane perpendicular to the xz axis along the beam
        // the plane is centered on fromEntity
        Vector3f planeNormal = new Vector3f(normalizedHDiff.x, 0, normalizedHDiff.y);
        Vector3f planeOrigin = MathTools.getClosestPointOnVector(startPosition, normalizedDiff, cameraPos);

        Vector3f planeProjectedCameraPos = MathTools.getPointProjectedToPlane(planeOrigin, planeNormal, cameraPos);

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

        // giga logger
        /*
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
         */

        // begin the actual rendering
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotation(-yaw));
        poseStack.mulPose(Axis.XP.rotation(-(float)Math.PI / 2));
        poseStack.mulPose(Axis.XP.rotation(pitch));
        poseStack.mulPose(Axis.YP.rotation(pitchToCamera));

        float length = diff.length();

        // scrolling effect!!!
        float scrollTime;
        float uvModifier;

        if (scrollSpeed != 0) {
            scrollTime = time % (20 / scrollSpeed);
            uvModifier = -(scrollTime / (20 / scrollSpeed));
        } else {
            uvModifier = 0;
        }
        // for the V coordinate of the UVs on the toEntity side of the beam
        float startV = 0 + uvModifier;
        float endV = (stretchFactor != 0 ? length / stretchFactor : 1) + uvModifier;

        VertexConsumer vertexes = bufferSource.getBuffer(renderType);

        Matrix4f lastPose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        vertexes.vertex(lastPose, -width, 0, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(0, endV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, -width, length, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(0, startV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, width, length, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, startV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();

        vertexes.vertex(lastPose, width, 0, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .uv(1, endV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(PACKED_LIGHT_FOR_2DS)
                .normal(normal, 0, 1, 0)
                .endVertex();


        poseStack.popPose();
    }


    public enum Colors {
        RED(0xFF0000),
        GREEN(0x00FF00),
        BLUE(0x0000FF),
        YELLOW(0xFFF300),
        WHITE(0xFFFFFF),
        BLACK(0x000000),
        GRAY(0x818181),
        AQUA(0x0083FF),
        MAGENTA(0xFF00E3),
        GRIMACE(0x9C008B),
        LOVE(0x9C0032),
        NEON(0xFFFFFF);

        private final int hex;

        Colors(int hex) {
            this.hex = hex;
        }

        public int get() {
            return hex;
        }
    }
}
