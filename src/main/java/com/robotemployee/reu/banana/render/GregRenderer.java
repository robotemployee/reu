package com.robotemployee.reu.banana.render;

import com.robotemployee.reu.banana.entity.GregEntity;
import com.robotemployee.reu.banana.model.GregEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

public class GregRenderer extends BananaRenderer<GregEntity> {
    public GregRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GregEntityModel());
    }
}
