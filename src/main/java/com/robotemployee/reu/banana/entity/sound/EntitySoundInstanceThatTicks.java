package com.robotemployee.reu.banana.entity.sound;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public class EntitySoundInstanceThatTicks<T extends Entity> extends ASoundInstanceThatTicks {
    private final T entity;

    protected EntitySoundInstanceThatTicks(
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
            Function<EntitySoundInstanceThatTicks<T>, Boolean> onTick
    ) {
        super(sound, source, random, looping, relative, volume, pitch, delay, attenuation, tickingSound -> onTick.apply((EntitySoundInstanceThatTicks<T>) tickingSound));
        this.entity = entity;
    }


    @Override
    public void tick() {
        if (entity.isRemoved()) stop();
        else super.tick();
    }

    public T getEntity() {
        return entity;
    }

    public static class Builder<E extends Entity> extends ASoundInstanceThatTicks.Builder<Builder<E>, EntitySoundInstanceThatTicks<E>> {
        private final E entity;

        protected Builder(E entity) {
            this.entity = entity;
        }

        @Override
        public EntitySoundInstanceThatTicks<E> build() {
            checkForInsufficientParams();
            return new EntitySoundInstanceThatTicks<>(
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
                    (tickingSound) -> onTick.apply(tickingSound)
            );
        }

        public static <E extends Entity> Builder<E> of(E entity) {
            return new Builder<>(entity);
        }
    }

    /**
     * <p>Simple macro that creates a sound which constantly sets its position to the target.</p>
     * @return The created {@link ASoundInstanceThatTicks}.
     * @param entity The {@link Entity} the sound will set its position to.
     * @param sound The {@link SoundEvent} to play and loop.
     * @param source The {@link SoundSource} to use.
     * @param customStopLogic If non-null and returns true, causes the sound to stop.
     * */
    public static <T extends Entity> EntitySoundInstanceThatTicks<T> playAndFollow(T entity, SoundEvent sound, SoundSource source, Predicate<T> customStopLogic) {
        return Builder.of(entity).withSound(sound).withSource(source).onTick(instance -> {
            T target = instance.getEntity();
            if (customStopLogic != null && customStopLogic.test(target)) return false;
            instance.setPosition(target.position());
            return true;
        }).looping().buildAndPlay();
    }

    /**
     * <p>Simple macro that creates a sound which constantly sets its position to the target.</p>
     * <p>The sound will only stop when the entity is removed.</p>
     * @return The created {@link ASoundInstanceThatTicks}.
     * @param entity The {@link Entity} the sound will set its position to.
     * @param sound The {@link SoundEvent} to play and loop.
     * @param source The {@link SoundSource} to use.
     * */
    public static <T extends Entity> EntitySoundInstanceThatTicks<T> playAndFollow(T entity, SoundEvent sound, SoundSource source) {
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
    public static <T extends LivingEntity> EntitySoundInstanceThatTicks<T> playAndFollow(T livingEntity, SoundEvent sound, SoundSource source, @Nullable Predicate<T> customStopLogic) {
        return Builder.of(livingEntity).withSound(sound).withSource(source).onTick(instance -> {
            T target = instance.getEntity();
            if (target.isDeadOrDying()) return false;
            if (customStopLogic != null && customStopLogic.test(target)) return false;
            instance.setPosition(target.position());
            return true;
        }).looping().buildAndPlay();
    }

    /**
     * <p>Simple macro that creates a sound which constantly sets its position to the target.</p>
     * <p>The sound will only stop when the entity is removed.</p>
     * <p>Specialized for {@link LivingEntity} - stops the sound when it dies.</p>
     * @param livingEntity The {@link LivingEntity} the sound will set its position to.
     * @param sound The {@link SoundEvent} to play and loop.
     * @param source The {@link SoundSource} to use.
     * */
    public static <T extends LivingEntity> EntitySoundInstanceThatTicks<T> playAndFollow(T livingEntity, SoundEvent sound, SoundSource source) {
        return playAndFollow(livingEntity, sound, source, null);
    }
}
