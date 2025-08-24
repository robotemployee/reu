package com.robotemployee.reu.mixin.base;

import org.spongepowered.asm.mixin.*;

@Mixin(targets = "net.minecraft.world.entity.ai.control.MoveControl$Operation")
public interface MoveControlOperationAccessor {

    //@Shadow public abstract Enum<?> valueOf(String name) throws IllegalArgumentException;

    @Unique
    default Enum<?> reu$get() {
        return (Enum<?>) this;
    }

    @Unique
    default Enum<?> reu$fromString(String name) {
        try {
            return Enum.valueOf(reu$get().getClass(), name);
        } catch (Exception ignored) {
            return null;
        }
    }

    /*
    @Shadow
    @Final
    @Mutable
    Enum<?>[] $VALUES;
    @Unique
    default Enum<?>[] reu$getValues() {
        return $VALUES;
    }

    @Unique
    default Enum<?> reu$fromInt(int ordinal) {
        Enum<?>[] values = reu$getValues();
        return values[ordinal];
    }
     */
}

