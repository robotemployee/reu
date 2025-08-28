package com.robotemployee.reu.banana.item;

import com.robotemployee.reu.banana.entity.BananaRaidMob;
import com.robotemployee.reu.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>This item decays over time. In order to facilitate that,
 * it remembers the time it was created in the NBT tag
 * and references that in order to calculate what to set the damage to.</p>
 * <p>Specifically, it remembers the (truncated) seconds since epoch it was created.</p>
 * <p>It doesn't use ticks because I couldn't figure out how to get the Level from an ItemStack.
 * It seems like it is meant to be provided with it only at the exact time and scope that such is needed.</p>
 * <p>Note that this item's decay mechanics do NOT respect pausing or downtime of any kind.
 * It will continue to decay in that time, as their frame of reference is only set once and all measurements are made from it.
 * I don't feel the need to fix it because it lasts 30 seconds.</p>
 * */
public class SemisolidItem extends Item {

    public static final String DECAY_KEY = "DecayTime";
    public static final int DECAY_SECONDS = 30;

    public static final int CHARGE_DURATION = 20;

    public static final int LASER_MAX_RANGE = 128;

    public static final int LASER_MIN_DAMAGE = 12;
    public static final int LASER_MAX_DAMAGE = 30;

    // FIXME
    // scrolling while charging causes whatever item you scroll into to be INSTANTLY AND IRREVERSIBLY DELETED

    public SemisolidItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> components, TooltipFlag tipOfYourTool) {
        if (isActivated(stack)) {
            int timeRemaining = DECAY_SECONDS - (int) getTimeExisted(stack);
            int seconds = timeRemaining % 60;
            String stringSeconds = (seconds < 0 ? "-" : "") + (seconds < 10 ? "0" : "") + Math.abs(seconds);
            int minutes = (int) Math.floor(timeRemaining / 60f);

            String color = seconds > 10 ? "§5" : "§4";
            components.add(Component.literal(String.format("%s%s:%s", color, minutes, stringSeconds)));
        } else {
            components.add(Component.literal("Decays after " + DECAY_SECONDS + "s"));
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack p_41452_) {
        return UseAnim.BOW;
    }

    public boolean isActivated(ItemStack stack) {
        return stack.getOrCreateTag().contains(DECAY_KEY);
    }

    @Override
    public boolean isRepairable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public int getBarWidth(ItemStack p_150900_) {
        return Math.max(0, super.getBarWidth(p_150900_));
    }

    @Override
    public int getDamage(ItemStack stack) {
        long time = getTimeExisted(stack);
        return (int)time;
    }

    public long getTimeExisted(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(DECAY_KEY)) return 0;

        return (System.currentTimeMillis() / 1000) - tag.getLong(DECAY_KEY);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int ignored1, boolean bignored) {
        super.inventoryTick(stack, level, entity, ignored1, bignored);
        if (level.isClientSide()) return;

        if (entity instanceof Player player && player.containerMenu.getCarried() == stack) {
            initIfNeeded(stack);
        }

        vibeChecker(stack);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        if (vibeChecker(stack)) return false;
        return super.onDroppedByPlayer(stack, player);
    }

    public void initIfNeeded(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(DECAY_KEY)) return;
        tag.putLong(DECAY_KEY, System.currentTimeMillis() / 1000);
        stack.setTag(tag);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        initIfNeeded(stack);
        if (vibeChecker(stack)) {
            entity.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    public boolean vibeChecker(ItemStack stack) {
        if (getDamage(stack) > getMaxDamage(stack)) {
            stack.shrink(1);
            return true;
        }
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    // Laser-firing functionality

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
}
