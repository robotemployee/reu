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
