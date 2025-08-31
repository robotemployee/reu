package com.robotemployee.reu.banana.entity.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class TickingSoundInstance extends AbstractTickableSoundInstance {
    protected final Function<TickingSoundInstance, Boolean> onTick;

    protected TickingSoundInstance(
            SoundEvent sound,
            SoundSource source,
            RandomSource random,
            boolean looping,
            boolean relative,
            float volume,
            float pitch,
            int delay,
            SoundInstance.Attenuation attenuation,
            Function<TickingSoundInstance, Boolean> onTick
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
        boolean shouldStop = !onTick.apply(this);

        if (shouldStop) stop();
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

    public void start() {
        Minecraft.getInstance().getSoundManager().queueTickingSound(this);
    }

    public class Builder {
        protected SoundEvent sound = null;
        protected SoundSource source = SoundSource.NEUTRAL;
        protected Supplier<RandomSource> random = SoundInstance::createUnseededRandom;
        protected boolean looping = false;
        protected boolean relative = false;
        protected float volume = 1;
        protected float pitch = 1;
        protected int delay = 0;
        protected SoundInstance.Attenuation attenuation = Attenuation.LINEAR;

        protected Function<TickingSoundInstance, Boolean> onTick = null;

        protected Builder() {

        }

        public Builder withSound(SoundEvent sound) {
            this.sound = sound;
            return this;
        }

        public Builder withSource(SoundSource source) {
            this.source = source;
            return this;
        }

        /**
         * <p>Takes a function to be evaluated every tick.</p>
         * <p>If that function returns false, the sound is stopped.</p>
         * <p>The function will not be run if the entity is removed.</p>
         * */
        public Builder onTick(Function<TickingSoundInstance, Boolean> onTick) {
            this.onTick = onTick;
            return this;
        }

        public Builder withRandom(RandomSource random) {
            this.random = () -> random;
            return this;
        }

        public Builder looping() {
            this.looping = true;
            return this;
        }

        public Builder relative() {
            this.relative = true;
            return this;
        }

        public Builder withVolume(float volume) {
            this.volume = volume;
            return this;
        }

        public Builder withPitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        public Builder withDelay(int delay) {
            this.delay = delay;
            return this;
        }

        public Builder withAttenuation(SoundInstance.Attenuation attenuation) {
            this.attenuation = attenuation;
            return this;
        }

        public TickingSoundInstance build() {
            checkForInsufficientParams();
            return new TickingSoundInstance(
                    sound,
                    source,
                    random.get(),
                    looping,
                    relative,
                    volume,
                    pitch,
                    delay,
                    attenuation,
                    onTick
            );
        }

        public TickingSoundInstance buildAndStart() {
            TickingSoundInstance instance = build();
            instance.start();
            return instance;
        }

        private void checkForInsufficientParams() {
            if (sound == null) throw new IllegalStateException("Ticking sound instance was not provided with a SoundEvent");
            if (onTick == null) throw new IllegalStateException("Ticking sound instance was not provided with an onTick function");
        }
    }
}
