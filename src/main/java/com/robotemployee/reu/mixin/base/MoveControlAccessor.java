package com.robotemployee.reu.mixin.base;

import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.lang.reflect.Field;

@Mixin(MoveControl.class)
public interface MoveControlAccessor {
    // i learned a lot
    // about accessing things
    // which are of a type
    // that isn't public
    @Accessor MoveControl.Operation getOperation();
    @Accessor void setOperation(MoveControl.Operation operation);

}
