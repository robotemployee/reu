package com.robotemployee.reu.banana.entity.sound;

import com.robotemployee.reu.banana.entity.GregEntity;
import com.robotemployee.reu.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GregFlyingSoundInstance extends AbstractTickableSoundInstance {
    private final GregEntity greg;
    protected GregFlyingSoundInstance(GregEntity greg) {
        super(ModSounds.GREG_FLYING.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.greg = greg;
        this.looping = true;
        this.relative = false;
        this.volume = 1;
        this.pitch = 1;
        this.attenuation = Attenuation.LINEAR;
        updatePosition();
    }

    public static GregFlyingSoundInstance startFrom(GregEntity greg) {
        GregFlyingSoundInstance newborn = new GregFlyingSoundInstance(greg);
        Minecraft.getInstance().getSoundManager().queueTickingSound(newborn);
        return newborn;
    }

    @Override
    public void tick() {
        if (greg.isRemoved() || !greg.isAlive() || greg.isInGroundMode()) stop();
        updatePosition();
    }

    private void updatePosition() {
        this.x = greg.getX();
        this.y = greg.getY();
        this.z = greg.getZ();
    }
}
