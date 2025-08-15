package com.robotemployee.reu.item;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.capability.FluidAndEnergyStorage;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ReconstructorItem extends Item {

    public static final String DISABLE_AUTO_REPAIR_TAG = "DisableAutoRepair";
    public static final String LAST_AUTO_REPAIR_TAG = "LastAutoRepairValue";
    
    private final Logger LOGGER = LogUtils.getLogger();
    // how much of each resource is consumed per point of durability
    protected final int ENERGY_COST = 20;
    protected final int FLUID_COST = 20;
    // uses integer division to truncate
    protected final int ENERGY_CAPACITY = 80000;
    protected final int FLUID_CAPACITY = 80000;
    protected final int POINTS_REPAIRABLE = Math.min(ENERGY_CAPACITY / ENERGY_COST, FLUID_CAPACITY / FLUID_COST);

    // ticks between automatic repairs
    // repair calculations for tweaking and the like https://www.desmos.com/calculator/fzxbo285l9
    protected final int PASSIVE_REPAIR_INTERVAL = 200;
    // amount it will try to repair for each item when automatic repairing
    // note that the amount repaired scales with max durability as seen in getAdjustedRepairAmount(int maxDurability)
    protected final int PASSIVE_REPAIR_AMOUNT = 12;

    public ReconstructorItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    /*
    @Override
    public boolean isBarVisible(@NotNull ItemStack itemStack) {
        return getEnergy(itemStack) < ENERGY_CAPACITY || getFluidAmount(itemStack) < FLUID_CAPACITY;
    }
     */

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        IEnergyStorage energy = getEnergyHandler(stack);
        IFluidHandler fluid = getFluidHandler(stack);
        if (energy == null || fluid == null) return 0;
        return Math.round(13.0F * getPoints(energy, fluid) / POINTS_REPAIRABLE);
    }

    @Override
    public int getBarColor(@NotNull ItemStack itemStack) {
        /*float f = Math.max(0.0F, (getEnergy(itemStack) + getFluidAmount(itemStack)) / (float)(ENERGY_CAPACITY + FLUID_CAPACITY));
        f = 1 - f;
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);*/
        return Mth.hsvToRgb(0, 89, 96);
    }

    @Override
    public boolean isRepairable(@NotNull ItemStack itemStack) {
        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> componentList, @Nullable TooltipFlag tooltipFlag) {

        IEnergyStorage energy = getEnergyHandler(stack);
        IFluidHandler fluid = getFluidHandler(stack);

        CompoundTag tag = stack.getTag();
        boolean manualOnly = tag != null && tag.getBoolean(DISABLE_AUTO_REPAIR_TAG);
        if (energy == null || fluid == null) return;
        int points = getPoints(energy, fluid);

        componentList.add(Component.literal(String.format("§6(%.1f%%) %s%s", points * 100 / (float)POINTS_REPAIRABLE, manualOnly?"§cOFF":"§aON",
                Screen.hasAltDown()?" §7<- (Points and auto-repair status)":"")));
        componentList.add(Component.literal(String.format("§7= §3%d points", points)));
        componentList.add(Component.empty());
        if (Screen.hasAltDown()) {
            componentList.add(Component.literal("§7Automatically repairs items in your inventory"));
            componentList.add(Component.literal("§6Use in hand§7 to toggle auto-repair"));
            componentList.add(Component.literal("§7Requires FE and Healing Potion as fuel"));
        } else {
            componentList.add(Component.literal("§7Expand with §r[§7Alt§r]§7 . . ."));
        }
        componentList.add(Component.empty());
        componentList.add(Component.literal(String.format("§7%.1f / %dkFE", energy.getEnergyStored() / (float)1000, ENERGY_CAPACITY / 1000)));
        componentList.add(Component.literal(String.format("§7%.1f / %dB", fluid.getFluidInTank(0).getAmount() / (float)1000, FLUID_CAPACITY / 1000)));
        componentList.add(Component.empty());
        componentList.add(Component.literal("§7§omiao"));
    }

    /*
    @Override
    public boolean isFoil(@NotNull ItemStack itemStack) {
        CompoundTag tag = itemStack.getTag();
        return tag != null && tag.getInt(LAST_AUTO_REPAIR_TAG) == 0 && !tag.getBoolean(DISABLE_AUTO_REPAIR_TAG);
    }
     */

    @Override @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        useToToggleAuto(itemStack, level, player);

        return InteractionResultHolder.consume(itemStack);
    }

    public void useToToggleAuto(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull Player player) {
        CompoundTag tag = itemStack.getOrCreateTag();
        boolean ignore = !tag.getBoolean(DISABLE_AUTO_REPAIR_TAG);

        if (level.isClientSide) {
            player.displayClientMessage(Component.literal(String.format("Auto-repair functionality %s", ignore?"§cdisabled":"§aenabled")), true);
            return;
        }
        if (ignore) {tag.putInt(LAST_AUTO_REPAIR_TAG, 0);}
        tag.putBoolean(DISABLE_AUTO_REPAIR_TAG, ignore);

        playToggleAutoRepairSound(ignore, level, player);
    }

    // Auto-repair functionality

    @Override
    public void inventoryTick(@NotNull ItemStack itemStack, @NotNull Level level, @NotNull Entity entity, int unused1, boolean unused2) {
        if ((level.getGameTime() % PASSIVE_REPAIR_INTERVAL) > 0 || level.isClientSide || !(entity instanceof Player player)) return;
        //("regular: " + getFluidAmount(itemStack));
        CompoundTag tag = itemStack.getOrCreateTag();
        if (tag.getBoolean(DISABLE_AUTO_REPAIR_TAG)) {
            //LOGGER.info("auto-repair is disabled!");
            tag.putInt(LAST_AUTO_REPAIR_TAG, 0);
            itemStack.setTag(tag);
            return;
        }

        //player.displayClientMessage(Component.literal("Attempting to repair inventory"), false);

        boolean result = repairInventory(level, player, itemStack);
        if (!result) {
            tag.putInt(LAST_AUTO_REPAIR_TAG, 0);
        }
    }
    // Only used by the auto repair
    private boolean repairInventory(@NotNull Level level, @NotNull Player player, @NotNull ItemStack repairer) {
        IEnergyStorage energy = getEnergyHandler(repairer);
        IFluidHandler fluid = getFluidHandler(repairer);

        if (energy == null || fluid == null) return false;

        if (!hasResources(energy, fluid)) {
            CompoundTag tag = repairer.getOrCreateTag();
            tag.putInt(LAST_AUTO_REPAIR_TAG, 0);
            repairer.setTag(tag);
            return false;
        }
        int totalRepaired = 0;
        boolean hasRepairedToFull = false;
        NonNullList<ItemStack> candidates = NonNullList.create();
        candidates.addAll(player.getInventory().items);
        candidates.addAll(player.getInventory().armor);
        candidates.addAll(player.getInventory().offhand);

        AccessoriesCapability accessoriesHandler = AccessoriesCapability.get(player);
        if (accessoriesHandler != null) {
            List<SlotEntryReference> equippedAccessories = accessoriesHandler.getAllEquipped();
            candidates.addAll(equippedAccessories.stream().map(SlotEntryReference::stack).toList());
        }

        for (ItemStack victim : candidates) {
            if (victim.isRepairable() && victim.isDamaged()) {
                if (!hasResources(energy, fluid)) break;
                // man
                int repairAmount = Math.min(getAdjustedRepairAmount(victim.getMaxDamage()), getPoints(energy, fluid));
                if (repairAmount >= victim.getDamageValue()) {
                    repairAmount = victim.getDamageValue();
                    hasRepairedToFull = true;
                }
                victim.setDamageValue(victim.getDamageValue() - repairAmount);
                totalRepaired += repairAmount;
            }
        }



        //player.sendSystemMessage(Component.literal(String.format("repaired %s items in inventory... %s FE %s mB", repairCount, energy, fluid)));

        if (!(totalRepaired > 0)) return false;
        CompoundTag tag = repairer.getOrCreateTag();
        tag.putInt(LAST_AUTO_REPAIR_TAG, totalRepaired);
        repairer.setTag(tag);
        consumeFluidAndEnergy(energy, fluid, totalRepaired);
        return true;
    }

    public void playToggleAutoRepairSound(boolean ignore, Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ignore?SoundEvents.IRON_TRAPDOOR_CLOSE:SoundEvents.IRON_TRAPDOOR_OPEN,
                SoundSource.PLAYERS, 1.0F, 1.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0F, 1.189207F);
    }

    // Attempts to prevent problems like "my big bore rifle is self-repairing faster than i can consume the durability
    public int getAdjustedRepairAmount(int maxDurability) {
        return Math.max(1, Math.min(PASSIVE_REPAIR_AMOUNT, (int)Math.floor(Math.sqrt(0.08F * maxDurability))));
    }

    @Nullable
    public static IFluidHandler getFluidHandler(@NotNull ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
    }

    @Nullable
    public static IEnergyStorage getEnergyHandler(@NotNull ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).resolve().orElse(null);
    }

    public int getPoints(int energy, int fluid) {
        return Math.min(energy / ENERGY_COST, fluid / FLUID_COST);
    }

    public int getPoints(IEnergyStorage energy, IFluidHandler fluid) {
        return getPoints(energy.getEnergyStored(), fluid.getFluidInTank(0).getAmount());
    }

    public boolean hasResources(int energy, int fluid) {
        return (energy >= ENERGY_COST && fluid >= FLUID_COST);
    }
    public boolean hasResources(IEnergyStorage energy, IFluidHandler fluid) {
        return hasResources(energy.getEnergyStored(), fluid.getFluidInTank(0).getAmount());
    }

    public void consumeFluidAndEnergy(IEnergyStorage energy, IFluidHandler fluid, int amountRepaired) {
        energy.extractEnergy(amountRepaired * ENERGY_COST, false);
        fluid.drain(amountRepaired * FLUID_COST, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public ICapabilityProvider initCapabilities(@NotNull ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidAndEnergyStorage(stack, FLUID_CAPACITY, ENERGY_CAPACITY);
    }

}
