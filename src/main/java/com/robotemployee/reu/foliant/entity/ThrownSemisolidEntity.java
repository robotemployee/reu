package com.robotemployee.reu.foliant.entity;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.foliant.item.SemisolidItem;
import com.robotemployee.reu.registry.ModEntities;
import com.robotemployee.reu.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class ThrownSemisolidEntity extends ThrowableItemProjectile {

    static final Logger LOGGER = LogUtils.getLogger();

    public ThrownSemisolidEntity(EntityType<? extends ThrownSemisolidEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static ThrownSemisolidEntity createFrom(ItemStack stack, Level level) {
        ThrownSemisolidEntity newborn = ModEntities.THROWN_SEMISOLID.get().create(level);

        newborn.setItem(stack);
        return newborn;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        //fixme logger
        LOGGER.info("Hit entity");
        Entity target = entityHitResult.getEntity();
        if (target.level().isClientSide()) return;
        if (!(target instanceof Player player)) {
            dropItemAt(target.position().add(getDeltaMovement().normalize().scale(-1)), target.level());
        } else {
            player.playSound(SoundEvents.BEEHIVE_EXIT);
            // fixme logger
            LOGGER.info("Adding item to player: " + getItem());
            LOGGER.info(String.format("%ss remaining", getSemisolid(getItem()).getSecondsExisted(getItem())));
            player.addItem(getItem());
        }
        discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult blockHit) {
        //fixme logger
        LOGGER.info("Hit block");
        super.onHitBlock(blockHit);
        dropItemAt(blockHit.getBlockPos().relative(blockHit.getDirection()).getCenter(), level());
        discard();
    }

    protected void dropItemAt(Vec3 pos, Level level) {
        //fixme logger
        LOGGER.info("Dropping item");
        ItemStack contained = getItem();
        //getSemisolid(contained).witherWhenUncaught(contained, this);
        if (contained == ItemStack.EMPTY || contained.getCount() <= 0) return;
        ItemEntity newborn = new ItemEntity(
                level,
                pos.x(),
                pos.y(),
                pos.z(),
                contained
        );
        setItem(contained);
        level.addFreshEntity(newborn);
    }

    @Override
    @NotNull
    protected Item getDefaultItem() {
        return ModItems.SEMISOLID.get();
    }

    public SemisolidItem getSemisolid(ItemStack stack) {
        return (SemisolidItem) stack.getItem();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || level().getGameTime() % 20 != 15) return;
        ItemStack contained = getItem();
        SemisolidItem semisolidItem = getSemisolid(contained);
        semisolidItem.initIfNeeded(contained);
        semisolidItem.workOnWitheringAway(contained);
        if (getSemisolid(contained).awareVibeChecker(contained, level(), blockPosition())) {
            discard();
        }
        setItem(contained);
    }



    /*
    @FunctionalInterface
    public interface OnHitConsumer {
        public void apply(ThrownItemEntity entity, HitResult hitResult);
    }
     */
}
