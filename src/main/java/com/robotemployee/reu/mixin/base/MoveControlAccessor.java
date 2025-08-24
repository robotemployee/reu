package com.robotemployee.reu.mixin.base;

import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.lang.reflect.Field;

@Mixin(MoveControl.class)
public abstract class MoveControlAccessor {

    @Shadow
    protected Enum<?> operation;

    @Unique
    public void reu$setOperation(Enum<?> operation) {
        try {
            Field field = MoveControl.class.getDeclaredField("operation");
            field.setAccessible(true);
            field.set(this, operation);
        } catch (Exception ignored) {

        }
    }

    @Unique
    public Enum<?> reu$getOperation() {
        return operation;
    }

    @Unique
    public void reu$stop() {
        Class<? extends Enum> opClass = operation.getClass();
        Enum<?> wait = Enum.valueOf(opClass, "WAIT");
        reu$setOperation(wait);
    }


}
