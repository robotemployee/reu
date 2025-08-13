package com.robotemployee.reu.mixin.mimi;

import com.robotemployee.reu.extra.BaseGame;
import io.github.tofodroid.com.sun.media.sound.SoftChannelProxy;
import io.github.tofodroid.mods.mimi.client.midi.synth.MIMIChannel;
import io.github.tofodroid.mods.mimi.client.midi.synth.MIMISynthUtils;
import io.github.tofodroid.mods.mimi.util.EntityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MIMIChannel.class)
public abstract class MIMIChannelMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "FIELD",
                    target = "Lio/github/tofodroid/mods/mimi/client/midi/synth/MIMIChannel;MAX_NOTE_DIST:Ljava/lang/Integer;"
            ),
            remap = false
    )
    private Integer redirectGetRange() {
        return BaseGame.MIDI_MAX_AUDIBLE_DISTANCE;
    }

}
