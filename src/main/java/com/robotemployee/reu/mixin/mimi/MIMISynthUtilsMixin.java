package com.robotemployee.reu.mixin.mimi;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.Config;
import com.robotemployee.reu.extra.BaseGame;
import io.github.tofodroid.mods.mimi.client.midi.synth.MIMISynthUtils;
import io.github.tofodroid.mods.mimi.common.config.ConfigProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MIMISynthUtils.class)
public class MIMISynthUtilsMixin {

    @Unique
    private static final Logger reu$LOGGER = LogUtils.getLogger();

    /*
    @ModifyVariable(method = "getVelocityForRelativeNoteDistance", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static Double modifyDistance(Double distance) {
        //reu$LOGGER.info("Note range: " + NoteEvent.NOTE_DEF_RANGE + " Other thing: " + MIMIChannel.MAX_NOTE_DIST);
        double modifiedDistanceFraction = Mth.clamp(
                distance / (BaseGame.MIDI_DISTANCE_ATTEUNATION_REDUCTION * BaseGame.MIDI_NOTE_DISTANCE),
                0,
                1 - BaseGame.MIDI_MIN_AUDIBLE_AMOUNT
        );
        return modifiedDistanceFraction * 64;
    }
     */

    @Inject(
            method = "getVelocityForRelativeNoteDistance",
            at = @At("HEAD"),
            remap = false,
            cancellable = true
    )
    private static void getVelocityForRelativeNoteDistance(Double distance, Boolean applyGameVolume, CallbackInfoReturnable<Byte> cir) {
        double volume = 127.0;
        if (distance > 0.0) {
            double adjustment = (BaseGame.MIDI_DISTANCE_EQUATION_OFFSET * BaseGame.MIDI_MAX_AUDIBLE_DISTANCE);
            double adjustedDistance = distance - adjustment;

            if (distance < adjustment) {
                //reu$LOGGER.info("Distance is very close");
                // do nothing if it's below; volume will stay 127
            } else if (distance > adjustment + BaseGame.MIDI_MAX_AUDIBLE_DISTANCE) {
                //reu$LOGGER.info("Distance is very far");
                // set it to minimum volume if the distance is farther than our equatiom allows
                volume = volume - (1 - BaseGame.MIDI_MIN_AUDIBLE_AMOUNT) * 127;
            } else {
                //reu$LOGGER.info("Distance is in the sweet spot");
                // and if it's in the sweet spot, we do the scaling stuff
                volume = volume - Math.floor(
                        (1 / BaseGame.MIDI_DISTANCE_QUIETNESS_FACTOR) *
                                127.0 * Math.pow(adjustedDistance, 2.5) / (
                                Math.pow(adjustedDistance, 2.5) + Math.pow(BaseGame.MIDI_MAX_AUDIBLE_DISTANCE - adjustedDistance, BaseGame.MIDI_DISTANCE_SLOPE_POWER)
                        )
                );
            }


            volume = Mth.clamp(volume, 0, 127);
            volume = Math.max(volume, 127 - ((1 - BaseGame.MIDI_MIN_AUDIBLE_AMOUNT) * 127));
        }

        if (applyGameVolume) {
            double mimiVolume = ConfigProxy.getAudioDeviceVolume() / 10.0;
            volume = volume * mimiVolume;
            double reuVolume = Config.getMIMIVolume();
            volume *= reuVolume;
            volume = Mth.clamp(volume, 0, 127);
            float catVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.RECORDS);
            catVolume = Math.min(Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER), catVolume);
            volume = volume * catVolume;
        }

        volume = Mth.clamp(volume, 0.0,127.0);
        byte byteVal = Integer.valueOf((int) volume).byteValue();
        cir.setReturnValue(byteVal);
    }

    @Inject(
            method = "getLRPanForRelativeNotePosition",
            at = @At("TAIL"),
            cancellable = true,
            remap = false
    )
    private static void getLRPanForRelativeNotePosition(Vec3 playerPos, BlockPos notePos, Float playerHeadRoationYaw, CallbackInfoReturnable<Byte> cir) {
        byte offset = (byte)(cir.getReturnValue() - 64);
        byte output = (byte)(64 + offset / BaseGame.MIDI_PAN_REDUCTION);
        cir.setReturnValue(output);
    }


}
