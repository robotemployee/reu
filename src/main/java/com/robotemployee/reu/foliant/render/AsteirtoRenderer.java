package com.robotemployee.reu.foliant.render;

import com.robotemployee.reu.foliant.entity.AsteirtoEntity;
import com.robotemployee.reu.foliant.model.AsteirtoEntityModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class AsteirtoRenderer extends FoliantRenderer<AsteirtoEntity> {

/*
    public static final ResourceLocation TEXTURE = new ResourceLocation(RobotEmployeeUtils.MODID, "textures/entity/asteirto/rosepike.png");
    public static final Pair<Vector2f, Vector2f> ROSE_UVS = new Pair<>(new Vector2f(0.296875f, 0), new Vector2f(1, 0.703125f));

 */
    public AsteirtoRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AsteirtoEntityModel());
    }

    @Override
    public RenderType getRenderType(AsteirtoEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public boolean shouldRender(@NotNull AsteirtoEntity asteirto, @NotNull Frustum frustum, double x, double y, double z) {
        if (asteirto.getTarget() != null && asteirto.getTarget().getUUID() != Minecraft.getInstance().player.getUUID()) {
            return false;
        }
        return super.shouldRender(asteirto, frustum, x, y, z);
    }

    /*
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
    */

}

