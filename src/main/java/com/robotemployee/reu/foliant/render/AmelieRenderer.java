package com.robotemployee.reu.foliant.render;

import com.robotemployee.reu.foliant.entity.AmelieEntity;
import com.robotemployee.reu.foliant.model.AmelieEntityModel;
import com.robotemployee.reu.foliant.model.AsteirtoEntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

public class AmelieRenderer extends FoliantRenderer<AmelieEntity> {
    public AmelieRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AmelieEntityModel());
    }


}
