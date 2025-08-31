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
public class EntityTickingSoundInstance<T extends Entity> extends AbstractTickableSoundInstance {
    private final T entity;
    protected final Function<EntityTickingSoundInstance<T>, Boolean> onTick;

    protected EntityTickingSoundInstance(
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
            Function<EntityTickingSoundInstance<T>, Boolean> onTick
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

        private Function<EntityTickingSoundInstance<T>, Boolean> onTick = null;

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
        public Builder<T> onTick(Function<EntityTickingSoundInstance<T>, Boolean> onTick) {
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

        public EntityTickingSoundInstance<T> build() {
            checkForInsufficientParams();
            return new EntityTickingSoundInstance<>(
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

        public EntityTickingSoundInstance<T> buildAndStart() {
            EntityTickingSoundInstance<T> instance = build();
            instance.start();
            return instance;
        }

        private void checkForInsufficientParams() {
            if (entity == null) throw new IllegalStateException("Ticking sound instance was not provided with entity");
            if (sound == null) throw new IllegalStateException("Ticking sound instance was not provided with a SoundEvent");
            if (onTick == null) throw new IllegalStateException("Ticking sound instance was not provided with an onTick function");
        }
    }

    /**
     * <p>Simple macro that creates a sound which constantly sets its position to the target.</p>
     * @return The created {@link TickingSoundInstance}.
     * @param entity The {@link Entity} the sound will set its position to.
     * @param sound The {@link SoundEvent} to play and loop.
     * @param source The {@link SoundSource} to use.
     * @param customStopLogic If non-null and returns true, causes the sound to stop.
     * */
    public static <T extends Entity> EntityTickingSoundInstance<T> playAndFollow(T entity, SoundEvent sound, SoundSource source, Predicate<T> customStopLogic) {
        return Builder.of(entity).withSound(sound).withSource(source).onTick(instance -> {
            T target = instance.getEntity();
            if (customStopLogic.test(target)) return false;
            instance.setPosition(target.position());
            return true;
        }).looping().buildAndStart();
    }

    /**
     * <p>Simple macro that creates a sound which constantly sets its position to the target.</p>
     * @return The created {@link TickingSoundInstance}.
     * @param entity The {@link Entity} the sound will set its position to.
     * @param sound The {@link SoundEvent} to play and loop.
     * @param source The {@link SoundSource} to use.
     * */
    public static <T extends Entity> EntityTickingSoundInstance<T> playAndFollow(T entity, SoundEvent sound, SoundSource source) {
        return playAndFollow(entity, sound, source, null);
    }

    /**
     * <p>Simple macro that creates a sound which constantly sets its position to the target.</p>
     * <p>Specialized for {@link LivingEntity} - stops the sound when it dies.</p>
     * @param livingEntity The {@link LivingEntity} the sound will set its position to.
     * @param sound The {@link SoundEvent} to play and loop.
     * @param source The {@link SoundSource} to use.
     * @param customStopLogic If non-null and returns true, causes the sound to stop.
     * */
    public static <T extends LivingEntity> EntityTickingSoundInstance<T> playAndFollow(T livingEntity, SoundEvent sound, SoundSource source, @Nullable Predicate<T> customStopLogic) {
        return Builder.of(livingEntity).withSound(sound).withSource(source).onTick(instance -> {
            T target = instance.getEntity();
            if (target.isDeadOrDying()) return false;
            if (customStopLogic != null && customStopLogic.test(target)) return false;
            instance.setPosition(target.position());
            return true;
        }).looping().buildAndStart();
    }

    /**
     * <p>Simple macro that creates a sound which constantly sets its position to the target.</p>
     * <p>Specialized for {@link LivingEntity} - stops the sound when it dies.</p>
     * @param livingEntity The {@link LivingEntity} the sound will set its position to.
     * @param sound The {@link SoundEvent} to play and loop.
     * @param source The {@link SoundSource} to use.
     * */
    public static <T extends LivingEntity> EntityTickingSoundInstance<T> playAndFollow(T livingEntity, SoundEvent sound, SoundSource source) {
        return playAndFollow(livingEntity, sound, source, null);
    }
}
