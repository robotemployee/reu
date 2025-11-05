package com.robotemployee.reu.util.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public abstract class ASoundInstanceThatTicks extends AbstractTickableSoundInstance {
    protected final Function<ASoundInstanceThatTicks, Boolean> onTick;

    protected ASoundInstanceThatTicks(
            SoundEvent sound,
            SoundSource source,
            RandomSource random,
            boolean looping,
            boolean relative,
            float volume,
            float pitch,
            int delay,
            SoundInstance.Attenuation attenuation,
            Function<ASoundInstanceThatTicks, Boolean> onTick
    ) {
        super(sound, source, random);
        this.looping = looping;
        this.relative = relative;
        this.volume = volume;
        this.pitch = pitch;
        this.delay = delay;
        this.attenuation = attenuation;
        this.onTick = onTick;
    }


    @Override
    public void tick() {
        if (isStopped()) return;
        if (!onTick.apply(this)) stop();
    }

    public void stopPlaying() {
        stop();
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public void setPosition(Vec3 pos) {
        this.x = pos.x();
        this.y = pos.y();
        this.z = pos.z();
    }

    protected void start() {
        Minecraft.getInstance().getSoundManager().queueTickingSound(this);
    }

    public static abstract class Builder<B extends Builder<?, ?>, I extends ASoundInstanceThatTicks> {
        protected SoundEvent sound = null;
        protected SoundSource source = SoundSource.NEUTRAL;
        protected Supplier<RandomSource> random = SoundInstance::createUnseededRandom;
        protected boolean looping = false;
        protected boolean relative = false;
        protected float volume = 1;
        protected float pitch = 1;
        protected int delay = 0;
        protected SoundInstance.Attenuation attenuation = Attenuation.LINEAR;

        protected Function<I, Boolean> onTick = null;

        protected Builder() {

        }

        protected B self() {
            return (B)this;
        };

        public B withSound(SoundEvent sound) {
            this.sound = sound;
            return self();
        }

        public B withSource(SoundSource source) {
            this.source = source;
            return self();
        }

        /**
         * <p>Takes a function to be evaluated every tick.</p>
         * <p>If that function returns false, the sound is stopped.</p>
         * <p>The function will not be run if the entity is removed.</p>
         * */
        public B onTick(Function<I, Boolean> onTick) {
            this.onTick = onTick;
            return self();
        }

        public B withRandom(RandomSource random) {
            this.random = () -> random;
            return self();
        }

        public B looping() {
            this.looping = true;
            return self();
        }

        public B relative() {
            this.relative = true;
            return self();
        }

        public B withVolume(float volume) {
            this.volume = volume;
            return self();
        }

        public B withPitch(float pitch) {
            this.pitch = pitch;
            return self();
        }

        public B withDelay(int delay) {
            this.delay = delay;
            return self();
        }

        public B withAttenuation(SoundInstance.Attenuation attenuation) {
            this.attenuation = attenuation;
            return self();
        }

        public abstract I build();

        public I buildAndPlay() {
            I instance = build();
            instance.start();
            return instance;
        }

        protected void checkForInsufficientParams() {
            if (sound == null) throw new IllegalStateException("Ticking sound instance was not provided with a SoundEvent");
            if (onTick == null) throw new IllegalStateException("Ticking sound instance was not provided with an onTick function");
        }
    }
}
