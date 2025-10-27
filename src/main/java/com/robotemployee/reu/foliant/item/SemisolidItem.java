package com.robotemployee.reu.foliant.item;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.foliant.entity.ThrownSemisolidEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

/**
 * <p>Decays over time. Can be thrown.</p>
 * */
public class SemisolidItem extends Item {

    public static final Logger LOGGER = LogUtils.getLogger();

    //public static final String SECONDS_REMAINING_KEY = "TicksRemaining";
    public static final String ACTIVATED_KEY = "Activated";
    public static final int DECAY_SECONDS = 60;
    public static final int COST_FOR_UNCAUGHT_THROW = 15;
    public static final int USE_DURATION_FOR_FULL_CHARGE = 15;
    public static final float MAX_THROW_FORCE = 10;
    public static final float MIN_THROW_FORCE = 1;

    public static final int MIN_SECONDS_REMAINING_WHEN_UNCAUGHT = 7;
    public static final int MAX_SECONDS_REMAINING_WHEN_UNCAUGHT_TO_SAVE = 10;

    /*
    public static final int CHARGE_DURATION = 20;

    public static final int LASER_MAX_RANGE = 128;

    public static final int LASER_MIN_DAMAGE = 12;
    public static final int LASER_MAX_DAMAGE = 30;
     */

    public SemisolidItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag tipOfYourTool) {
        if (isActivated(stack)) {
            int totalSecondsRemaining = getSecondsRemaining(stack);
            if (totalSecondsRemaining > 0) {
                int secondsClock = totalSecondsRemaining % 60;
                String stringSeconds = (secondsClock < 10 ? "0" : "") + Math.abs(secondsClock);
                int minutesClock = (int) Math.floor(totalSecondsRemaining / 60f);

                String color = totalSecondsRemaining > 10 ? "§5" : "§4";
                components.add(Component.literal(String.format("%s%s:%s", color, minutesClock, stringSeconds)));
                return;
            }

            components.add(Component.literal("§4Expired!"));
            return;
        }

        components.add(Component.literal("Decays after " + DECAY_SECONDS + "s"));
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int ignored1, boolean bignored) {
        super.inventoryTick(stack, level, entity, ignored1, bignored);
        if (level.isClientSide()) return;

        if (entity instanceof Player) initIfNeeded(stack);

        stack.setPopTime(0);

        if (isActivated(stack) && (level.getGameTime() % 20 == 15)) workOnWitheringAway(stack);
        awareVibeChecker(stack, level, entity.blockPosition());
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        if (!player.level().isClientSide() && awareVibeChecker(stack, player.level(), player.blockPosition())) return false;
        return super.onDroppedByPlayer(stack, player);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.level().isClientSide()) return false;
        initIfNeeded(stack);
        if (entity.level().getGameTime() % 20 == 15) workOnWitheringAway(stack);
        if (awareVibeChecker(stack, entity.level(), entity.blockPosition())) {
            entity.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    public boolean isActivated(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(ACTIVATED_KEY);
    }


    public int getSecondsExisted(@NotNull ItemStack stack) {
        if (!isActivated(stack)) return 0;
        return stack.getDamageValue();
    }

    public int getSecondsRemaining(@NotNull ItemStack stack) {
        return Mth.clamp(DECAY_SECONDS - getSecondsExisted(stack), 0, DECAY_SECONDS);
    }


    public void initIfNeeded(ItemStack stack) {
        if (isActivated(stack)) return;
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(ACTIVATED_KEY, true);
        stack.setTag(tag);
    }

    public void workOnWitheringAway(ItemStack stack) {
        workOnWitheringAway(stack, 1);
    }

    public void workOnWitheringAway(ItemStack stack, int amount) {
        stack.setDamageValue(stack.getDamageValue() + amount);
    }

    public boolean vibeChecker(ItemStack stack) {
        if (getSecondsExisted(stack) > DECAY_SECONDS) {
            stack.shrink(1);
            //fixme logger
            LOGGER.info("Stack expired");
            return true;
        }
        return false;
    }

    public boolean awareVibeChecker(ItemStack stack, Level level, BlockPos soundPosition) {
        // suffer
        boolean crushedTheVibesMan = vibeChecker(stack);
        if (crushedTheVibesMan) level.playSound(null, soundPosition, SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL);
        return crushedTheVibesMan;
    }

    public void witherWhenUncaught(ItemStack stack, ThrownSemisolidEntity uncaughtProjectile) {
        if (uncaughtProjectile.level().isClientSide()) return;
        int previousSecondsRemaining = getSecondsRemaining(stack);
        workOnWitheringAway(stack, COST_FOR_UNCAUGHT_THROW);
        if (previousSecondsRemaining > MAX_SECONDS_REMAINING_WHEN_UNCAUGHT_TO_SAVE && getSecondsRemaining(stack) < MIN_SECONDS_REMAINING_WHEN_UNCAUGHT) setTimeRemaining(stack, MIN_SECONDS_REMAINING_WHEN_UNCAUGHT);

        if (awareVibeChecker(stack, uncaughtProjectile.level(), uncaughtProjectile.blockPosition())) {
            uncaughtProjectile.level().playSound(null, uncaughtProjectile.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL);
        }
    }

    public void addTime(ItemStack stack, int addedSeconds) {
        stack.setDamageValue(stack.getDamageValue() - addedSeconds);
    }

    public void setTimeRemaining(ItemStack stack, int newTime) {
        stack.setDamageValue(DECAY_SECONDS - newTime);
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(@NotNull ItemStack stack, @NotNull Slot slot, @NotNull ClickAction clickAction, @NotNull Player player) {
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack stack1, @NotNull ItemStack stack2, @NotNull Slot slot, @NotNull ClickAction clickAction, @NotNull Player player, SlotAccess slotAccess) {
        return true;
    }



    @Override
    @NotNull
    public UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    public static final int MAX_USE_DURATION = 200;
    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return MAX_USE_DURATION;
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        super.onStopUsing(stack, entity, count);
        if (entity.level().isClientSide()) return;
        throwAsProjectile(entity, stack, count - MAX_USE_DURATION);
    }

    @Override
    @NotNull
    public ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        if (!level.isClientSide()) throwAsProjectile(entity, stack, USE_DURATION_FOR_FULL_CHARGE);
        return ItemStack.EMPTY;
    }

    public static float getCharge(LivingEntity user, ItemStack stack, int ticksUsed) {
        return Mth.clamp(ticksUsed / (float)USE_DURATION_FOR_FULL_CHARGE, 0, 1);
    }

    public static void throwAsProjectile(LivingEntity user, ItemStack stack, int ticksUsed) {
        float charge = getCharge(user, stack, ticksUsed);
        ThrownSemisolidEntity projectile = ThrownSemisolidEntity.createFrom(stack, user.level());
        projectile.moveTo(user.getEyePosition().add(user.getLookAngle()));
        if (user instanceof ServerPlayer serverPlayer) {
            //serverPlayer.sendSystemMessage(Component.literal(String.format("§c-%ss", COST_FOR_UNCAUGHT_THROW)), true);
        }
        double force = Mth.lerp(charge, MIN_THROW_FORCE, MAX_THROW_FORCE);
        projectile.addDeltaMovement(user.getLookAngle().scale(force));
        user.level().addFreshEntity(projectile);
        stack.shrink(1);
    }

    // Laser-firing functionality

    /*
    public static final int USE_TICKS = 10000;
    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_TICKS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        ItemStack newLaser = fireLaser(stack, entity, USE_TICKS - count);

        entity.setItemInHand(entity.getUsedItemHand(), newLaser);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return fireLaser(stack, entity, CHARGE_DURATION);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {

    }

    public ItemStack fireLaser(ItemStack stack, LivingEntity user, int chargeTicks) {
        if (!(user instanceof Player player)) return ItemStack.EMPTY;

        if (user.level().isClientSide()) {
            int count = player.getInventory().countItem(ModItems.SEMISOLID.get());
            String side = "-".repeat(Math.min(count - 1, 5));
            player.displayClientMessage(Component.literal(side + (count - 1) + side), true);
        }

        float charge = (float)Math.min(1, chargeTicks / CHARGE_DURATION);
        stack.shrink(1);

        HitResult hit = ProjectileUtil.getHitResultOnViewVector(user, entity -> !entity.isSpectator(), LASER_MAX_RANGE);

        if (!(hit instanceof EntityHitResult entityHitResult)) return getMeANewLaser(player);

        Entity target = entityHitResult.getEntity();

        boolean againstBanana = target instanceof BananaRaidMob;

        float bananaMultiplier = againstBanana ? 1f : 0.5f;

        float baseDamage = Mth.lerp(charge, LASER_MIN_DAMAGE, LASER_MAX_DAMAGE);

        float damage = bananaMultiplier * baseDamage;

        target.hurt(user.damageSources().magic(), damage);

        if (!againstBanana) return getMeANewLaser(player);


        BananaRaidMob banana = (BananaRaidMob)target;

        if (target.isAlive()) return getMeANewLaser(player);

        Collection<ItemEntity> drops = target.captureDrops(new LinkedList<>());

        // drops can be null, look at captureDrops()
        if (drops == null) return getMeANewLaser(player);

        drops.stream().map(ItemEntity::getItem).forEach(drop -> {
            if (drop.getItem() instanceof SemisolidItem semisolid) semisolid.initIfNeeded(drop);
            player.addItem(drop);
        });

        return getMeANewLaser(player);
    }

    public ItemStack getMeANewLaser(Player player) {
        // this isn't optimized but like,,, imo it doesn't really have to be. stream pretty
        Inventory inventory = player.getInventory();
        ItemStack existingSemisolid = inventory.items.stream()
                .filter(stack -> stack.is(ModItems.SEMISOLID.get())).findAny().orElse(null);

        if (existingSemisolid == null) return ItemStack.EMPTY;
        ItemStack newborn = existingSemisolid.copy();
        inventory.removeItem(existingSemisolid);

        return newborn;
    }
     */
}
