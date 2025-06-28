package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.systems.chunk_cursor_system.ChunkCursorBase;
import com.robotemployee.reu.compat.SculkHordeCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkCursorBase.class)
public class ChunkCursorBaseMixin {

    // "Lcom/github/sculkhorde/systems/chunk_cursor_system/ChunkCursorBase;disableObstruction:Z"
    // ewoudje and my introduction to mixins
    @Redirect(method = "*", at = @At(value = "FIELD", target = "Lcom/github/sculkhorde/systems/chunk_cursor_system/ChunkCursorBase;disableObstruction:Z", opcode = Opcodes.GETFIELD), remap = SculkHordeCompat.remapNormalSculkHorde)
    public boolean disableObstruction(ChunkCursorBase chunkCursorBase) {
        return false;
    }

    // and make it consider whether the thingy was out of bounds
    @Inject(method = "isObstructed", at = @At(value = "HEAD"), cancellable = true, remap = SculkHordeCompat.remapNormalSculkHorde)
    void isObstructed(ServerLevel serverLevel, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (SculkHordeCompat.isOutOfBounds(pos, serverLevel)) cir.setReturnValue(true);
    }
}
