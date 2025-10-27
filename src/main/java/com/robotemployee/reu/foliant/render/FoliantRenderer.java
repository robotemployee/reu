package com.robotemployee.reu.foliant.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.robotemployee.reu.foliant.entity.FoliantRaidMob;
import com.robotemployee.reu.util.RenderTools;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.ArrayList;

@OnlyIn(Dist.CLIENT)
public class FoliantRenderer<T extends FoliantRaidMob & GeoAnimatable> extends GeoEntityRenderer<T> {

    public static final Logger LOGGER = LogUtils.getLogger();

    // these will be rendered with global positioning
    public final ArrayList<RenderRequest<T>> RENDER_REQUESTS = new ArrayList<>();

    protected void queueToRenderGlobally(RenderRequest<T> request) {
        RENDER_REQUESTS.add(request);
    }

    protected static final int PACKED_LIGHT_FOR_2DS = RenderTools.PACKED_LIGHT_FOR_2DS;
    public FoliantRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        if (RENDER_REQUESTS.size() == 0) return;

        poseStack.pushPose();
        poseStack.translate(-entity.getX(), -entity.getY(), -entity.getZ());
        for (RenderRequest<T> request: RENDER_REQUESTS) {
            poseStack.pushPose();
            request.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @FunctionalInterface
    public interface RenderRequest<T> {
        void render(T entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight);
    }
}
