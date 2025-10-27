package com.robotemployee.reu.foliant.render;

import com.robotemployee.reu.foliant.entity.GregEntity;
import com.robotemployee.reu.foliant.model.GregEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GregRenderer extends FoliantRenderer<GregEntity> {
    public GregRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GregEntityModel());
    }
}
