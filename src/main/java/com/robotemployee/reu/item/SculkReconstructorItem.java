package com.robotemployee.reu.item;

import com.github.sculkhorde.core.ModMobEffects;
import com.robotemployee.reu.compat.SculkHordeCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class SculkReconstructorItem extends ReconstructorItem {

    // Allows you to quickly manually repair your items.
    // Gives you the Sculk Lure effect when you're within Sculk borders.

    protected final int ENERGY_CAPACITY = 400000;
    protected final int FLUID_CAPACITY = 400000;
    protected final int POINTS_REPAIRABLE = Math.min(ENERGY_CAPACITY / ENERGY_COST, FLUID_CAPACITY / FLUID_COST);

    // amount it will try to repair target item on use
    protected final int MANUAL_REPAIR_AMOUNT = 400;

    protected final int LURE_INTERVAL = 200;

    public SculkReconstructorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemStack, @Nullable Level level, @NotNull List<Component> componentList, @Nullable TooltipFlag tooltipFlag) {
        int energy = getEnergy(itemStack);
        int fluid = getFluidAmount(itemStack);
        CompoundTag tag = itemStack.getTag();
        boolean manualOnly = tag != null && tag.getBoolean("DisableAutoRepair");

        componentList.add(Component.literal(String.format("§6(%.1f%%) %s%s", getPoints(itemStack) * 100 / (float)POINTS_REPAIRABLE, manualOnly?"§cOFF":"§aON",
                Screen.hasAltDown()?" §7<- (Points and auto-repair status)":"")));
        componentList.add(Component.literal(String.format("§7= §3%d points", getPoints(itemStack))));
        componentList.add(Component.empty());
        if (Screen.hasAltDown()) {
            componentList.add(Component.literal("§7Automatically repairs items in your inventory"));
            componentList.add(Component.literal("§6Use in hand§7 to quickly manual repair"));
            componentList.add(Component.literal("§6Use while crouching§7 to toggle auto-repair"));
            componentList.add(Component.literal("§7Requires FE and Healing Potion as fuel"));
        } else {
            componentList.add(Component.literal("§7Expand with §r[§7Alt§r]§7 . . ."));
        }
        componentList.add(Component.empty());
        componentList.add(Component.literal(String.format("§7%.1fkFE / %d", energy / (float)1000, ENERGY_CAPACITY / 1000)));
        componentList.add(Component.literal(String.format("§7%.1fB / %d", fluid / (float)1000, FLUID_CAPACITY / 1000)));
        componentList.add(Component.empty());
        componentList.add(Component.literal("§7§ocancer !!"));
    }


    @Override @NotNull
    public UseAnim getUseAnimation(@NotNull ItemStack itemStack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack itemStack) {
        return 20;
    }

    @Override @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (player.isCrouching()) {
            useToToggleAuto(itemStack, level, player);
        } else {
            player.startUsingItem(hand);
        }

        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull Entity entity, int unused1, boolean unused2) {
        if (!level.isClientSide && entity instanceof Player player && level.getGameTime() % LURE_INTERVAL != 0 && !SculkHordeCompat.isOutOfBounds(player)) {
           ModMobEffects.SCULK_LURE.get().applyEffectTick(player, LURE_INTERVAL + 20);
        }
        super.inventoryTick(itemStack, level, entity, unused1, unused2);
    }

    // Repair actively by using the item. Plays an amethyst and additionally an enchanting sound if successful.
    @Override @NotNull
    public ItemStack finishUsingItem(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) return itemStack;
        if (!level.isClientSide) useToRepair(itemStack, level, player);
        player.stopUsingItem();
        return itemStack;
    }

    public void useToRepair(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull Player player) {
        ItemStack repairedItem = player.getOffhandItem();
        if (repairedItem.is(itemStack.getItem())) repairedItem = player.getMainHandItem();
        // 1.189207 -> A
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0F, 0.840896F);
        int energy = getEnergy(itemStack);
        int fluid = getFluidAmount(itemStack);
        // Problem cases
        if (repairedItem.isEmpty()) {
            if (level.isClientSide) player.displayClientMessage(Component.literal("§cRepairable item required in other hand"), true);
            return;
        }
        if (!repairedItem.isRepairable()) {
            if (level.isClientSide) player.displayClientMessage(Component.literal("§cTarget item is not repairable"), true);
            return;
        }

        if (!repairedItem.isDamaged()) {
            if (level.isClientSide) player.displayClientMessage(Component.literal("§cTarget item does not need repairing"), true);
            return;
        }
        if (!hasResources(energy, fluid)) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.literal(fluid >= FLUID_COST ?"§cInsufficient FE":"Insufficient Healing Potion"), true);
            }
            return;
        }

        int repairAmount = Math.min(repairedItem.getDamageValue(), getPoints(energy, fluid));
        if (repairAmount > MANUAL_REPAIR_AMOUNT) repairAmount = MANUAL_REPAIR_AMOUNT;
        if (repairAmount == 0) return;
        /*
        if (level.isClientSide) {
            int max = repairedItem.getMaxDamage();
            int cur = repairedItem.getDamageValue();
            player.displayClientMessage(Component.literal(String.format("%d / %d", max - cur + repairAmount, max)), true);
            return;
        }
         */
        //else {
        repairedItem.setDamageValue(repairedItem.getDamageValue() - repairAmount);
        consumeFluidAndEnergy(itemStack, repairAmount);
        //}
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 2.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return;
    }

    @Override
    public void playToggleAutoRepairSound(boolean ignore, @NotNull Level level, @NotNull Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WOODEN_DOOR_CLOSE,
                SoundSource.PLAYERS, 1.0F, ignore?1.2F:1.5F);
        if (!ignore) level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0F, 0.840896F);
    }
}
