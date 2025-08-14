package com.robotemployee.reu.mixin.mimi;

import com.robotemployee.reu.extra.BaseGame;
import io.github.tofodroid.mods.mimi.client.midi.synth.MIMIChannel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
