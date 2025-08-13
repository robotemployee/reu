package com.robotemployee.reu.mixin.mimi;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.extra.BaseGame;
import io.github.tofodroid.mods.mimi.common.api.event.note.NoteEvent;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoteEvent.class)
public class NoteEventMixin {

    @Unique
    private static final Logger reu$LOGGER = LogUtils.getLogger();

    @Redirect(method = "*",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Lio/github/tofodroid/mods/mimi/common/api/event/note/NoteEvent;NOTE_DEF_RANGE:Ljava/lang/Integer;"
            ),
            remap = false
    )
    private static Integer getNoteDefRange() {
        //reu$LOGGER.info("have you ever thought about");
        return BaseGame.MIDI_NOTE_DISTANCE;
    }
}
