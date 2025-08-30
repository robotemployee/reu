package com.robotemployee.reu.banana.entity.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;
import java.util.function.Supplier;

public class TickingSoundInstance<T extends Entity> extends AbstractTickableSoundInstance {
    private final T entity;
    protected final Function<TickingSoundInstance<T>, Boolean> onTick;

    protected TickingSoundInstance(
            T entity,
            SoundEvent sound,
            SoundSource source,
            RandomSource random,
            boolean looping,
            boolean relative,
            float volume,
            float pitch,
            int delay,
            SoundInstance.Attenuation attenuation,
            Function<TickingSoundInstance<T>, Boolean> onTick
    ) {
        super(sound, source, random);
        this.entity = entity;
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
        boolean shouldStop = entity.isRemoved() || !onTick.apply(this);

        if (shouldStop) stop();
    }

    public T getEntity() {
        return entity;
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

    public static class Builder <T extends Entity> {

        private final T entity;
        private SoundEvent sound = null;
        private SoundSource source = SoundSource.NEUTRAL;
        private Supplier<RandomSource> random = SoundInstance::createUnseededRandom;
        private boolean looping = false;
        private boolean relative = false;
        private float volume = 1;
        private float pitch = 1;
        private int delay = 0;
        private SoundInstance.Attenuation attenuation = Attenuation.LINEAR;

        private Function<TickingSoundInstance<T>, Boolean> onTick = null;

        private Builder(T entity) {
            this.entity = entity;
        }

        public static <T extends Entity> Builder<T> of(T entity) {
            return new Builder<>(entity);
        }

        public Builder<T> withSound(SoundEvent sound) {
            this.sound = sound;
            return this;
        }

        public Builder<T> withSource(SoundSource source) {
            this.source = source;
            return this;
        }

        /**
         * <p>Takes a function to be evaluated every tick.</p>
         * <p>If that function returns false, the sound is stopped.</p>
         * <p>The function will not be run if the entity is removed.</p>
         * */
        public Builder<T> onTick(Function<TickingSoundInstance<T>, Boolean> onTick) {
            this.onTick = onTick;
            return this;
        }

        public Builder<T> withRandom(RandomSource random) {
            this.random = () -> random;
            return this;
        }

        public Builder<T> looping() {
            this.looping = true;
            return this;
        }

        public Builder<T> relative() {
            this.relative = true;
            return this;
        }

        public Builder<T> withVolume(float volume) {
            this.volume = volume;
            return this;
        }

        public Builder<T> withPitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        public Builder<T> withDelay(int delay) {
            this.delay = delay;
            return this;
        }

        public Builder<T> withAttenuation(SoundInstance.Attenuation attenuation) {
            this.attenuation = attenuation;
            return this;
        }

        public TickingSoundInstance<T> build() {
            if (hasInsufficientParams()) throw new IllegalStateException("Had insufficient parameters");
            return new TickingSoundInstance<>(
                    entity,
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

        public TickingSoundInstance<T> buildAndStart() {
            TickingSoundInstance<T> instance = build();
            instance.start();
            return instance;
        }

        private boolean hasInsufficientParams() {
            return entity == null ||
                    sound == null ||
                    onTick == null;
        }
    }

    public static <T extends Entity> TickingSoundInstance<T> playAndFollow(T entity, SoundEvent sound, SoundSource source) {
        return Builder.of(entity).withSound(sound).withSource(source).onTick(instance -> {
            T target = instance.getEntity();
            instance.setPosition(target.position());
            return true;
        }).buildAndStart();
    }
}
