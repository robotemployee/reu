package com.robotemployee.reu.item;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.block.entity.InfusedEggBlockEntity;
import com.robotemployee.reu.capability.FilteredFluidStorage;
import com.robotemployee.reu.core.ModBlocks;
import com.robotemployee.reu.core.ModFluids;
import com.robotemployee.reu.fluid.MobFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

public class InjectorItem extends Item {

    /*
    This item is for:
    - Extracting genetic fluid from mobs
    - Infusing it into blank eggs
    - Manipulating infused eggs

    Filling the injector is done by increasing a number per consecutive tick of using it on an entity.
    Once the maximum fill amount is reached, it can't be filled up anymore.

    Draining into a Blank Egg is handled entirely by this item, because idk sounds funny.
    As it drains, an extra counter goes down per tick starting from the maximum fill amount.
    When that reaches zero, the injector has relevant data cleared and the blank egg is replaced with an infused one.
    If the process is interrupted, the process stops (hopefully) harmlessly.
     */

    /*
    TODO: replace blank egg weird filling mechanics
    // shift right click it and boom
    // the injector can be emptied out by putting it into a crafting table or a bottle or something
    // bottles...
    // people could make collections of fluids lol
     */


    public InjectorItem(Item.Properties properties) {
        super(properties);
    }

    static Logger LOGGER = LogUtils.getLogger();

    public static final String MODE_PATH = "Mode";
    public static final String POSITION_PATH = "Position";

    public static final int USE_DURATION = 72000;

    public static final int BLANK_EGG_FILL_TICKS = 40;

    public final int FILL_PER_TICK = 10;

    // this is for how many segments there are in the tooltip
    public static final int TUBE_LENGTH = 4;

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag flag) {
        // technical debt supreme. unless i just don't touch this ever again
        final String[] levels = {"    .", "░:░", "▒▒",};
        IFluidHandlerItem handler = getHandler(stack);

        FluidStack fluid = handler.getFluidInTank(0);
        int fill = TUBE_LENGTH * fluid.getAmount() / handler.getTankCapacity(0);

        components.add(Component.literal("§b  Δ"));
        components.add(Component.literal("/`§8~\\"));

        int[] indexes = new int[TUBE_LENGTH];

        boolean isFull = fill == TUBE_LENGTH;

        for (int i = TUBE_LENGTH; i > 0; i--) {
            if (fill > i || isFull) {
                indexes[TUBE_LENGTH - i] = 2;
            } else if (fill == i) {
                indexes[TUBE_LENGTH - i] = 1;
            } else {
                indexes[TUBE_LENGTH - i] = 0;
            }
        }

        // horror. horror. horror. horror. DON'T. LOOK. pls and thanks

        String percentAftertouch = isFull ? "§2~§aReady" : String.format("§2~§a%.1f§2%%", 100 * (fluid.getAmount() / (float)handler.getTankCapacity(0)));

        String typeAftertouch = String.format("§3 %s", !fluid.isEmpty() ? fluid.getTag().getString(MobFluid.ENTITY_TYPE_KEY) : "(empty)");

        boolean showedPercent = false;
        boolean firstTime = true;
        for (int visual : indexes) {
            String aftertouch = "";
            boolean shouldShowPercent = visual == 1 || (isFull && firstTime);
            boolean shouldShowContained = showedPercent;

            if (shouldShowPercent) aftertouch = percentAftertouch;
            else if (shouldShowContained) aftertouch = typeAftertouch;

            String border = visual < 3 ? "|" : "";

            components.add(Component.literal(
                    border + colorFromFill(fill, visual) + levels[visual] + "§8" + border + aftertouch

            ));
            showedPercent = shouldShowPercent;
            firstTime = false;
        }

        components.add(Component.literal("`§8`|``|``" + (showedPercent ? typeAftertouch : "")));
        components.add(Component.literal(String.format("-§8'-'- %smb", fluid.getAmount())));
    }

    private String colorFromFill(int fill, int level) {
        if (level == 0 || fill == 0) {
            return "§8";
        } else if (fill == TUBE_LENGTH) {
            return "§5";
        } else if (fill > TUBE_LENGTH / 2) {
            return "§7";
        } else if (fill > TUBE_LENGTH / 4) {
            return "§6";
        } else {
            return "§4";
        }
    }

    @Override @NotNull
    public UseAnim getUseAnimation(@NotNull ItemStack itemStack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) { return USE_DURATION; }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        float fraction = 0.5f;
        return Math.round(13.0F * fraction);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return 0xc5a3e0;
    }

    @Override @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hitResult = superPick(player);
        Mode mode = Mode.fromContext(hitResult, level, player, stack);

        mode.saveToItem(stack);

        if (mode == Mode.ENTITY) {
            IFluidHandlerItem handler = getHandler(stack);
            EntityType<?> contained = MobFluid.typeFromStack(handler.getFluidInTank(0));
            LivingEntity victim = (LivingEntity)((EntityHitResult)hitResult).getEntity();

            if (contained == null) {
                fillWithEntity(handler, victim, 1);
            } else if (victim.getType() != contained) {

                stopInjection(player);
                return InteractionResultHolder.fail(stack);
            }

        }

        if (mode.isBlock() && !level.isClientSide()) {
            CompoundTag tag = stack.getOrCreateTag();
            BlockPos pos = ((BlockHitResult)hitResult).getBlockPos();
            int[] array = {pos.getX(), pos.getY(), pos.getZ()};

            tag.putIntArray(POSITION_PATH, array);
            stack.setTag(tag);
        }

        if (mode.isContinuous()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int tick) {
        if (!(entity instanceof Player player)) return;

        IFluidHandlerItem handler = getHandler(stack);
        assert handler != null;
        //LOGGER.info(handler.getFluidInTank(0).writeToNBT(new CompoundTag()).toString());
        HitResult hitResult = superPick(entity);
        Mode newMode = Mode.fromContext(hitResult, level, entity, stack);
        Mode oldMode = Mode.fromItem(stack);

        // note that only the continuous modes will appear here

        if (oldMode != newMode) {
            stopInjection(player, oldMode == Mode.ENTITY ? 5 : 20);
            //LOGGER.info("Stopping injection due to mode mismatch: " + oldMode + " vs " + newMode);
            return;
        }

        CompoundTag tag = stack.getTag();

        if (newMode.isBlock() && tag != null && tag.contains(POSITION_PATH)) {
            int[] array = tag.getIntArray(POSITION_PATH);
            BlockPos oldPos = new BlockPos(array[0], array[1], array[2]);

            if (!oldPos.equals(((BlockHitResult) hitResult).getBlockPos())) {
                stopInjection(player);
                return;
            }
        }

        switch (newMode) {
            case ENTITY -> {
                LivingEntity victim = (LivingEntity)((EntityHitResult)hitResult).getEntity();
                EntityType<?> contained = MobFluid.typeFromStack(handler.getFluidInTank(0));

                if (contained != null && victim.getType() != contained) {
                    stopInjection(player);
                    return;
                }

                if (level.isClientSide()) return;

                if (handler.getFluidInTank(0).getAmount() == 1000) stopInjection(player);



                victim.hurt(victim.damageSources().playerAttack(player), 1);
                fillWithEntity(handler, victim, FILL_PER_TICK);
            }
            case BLANK_EGG -> {
                if (level.isClientSide()) return;
                LOGGER.info("Remaining Time: " + (USE_DURATION - tick));
                if (USE_DURATION - tick < BLANK_EGG_FILL_TICKS) return;

                if (!level.isClientSide()) {
                    BlockState state = ModBlocks.INFUSED_EGG.get().defaultBlockState();
                    BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();

                    level.setBlockAndUpdate(pos, state);

                    InfusedEggBlockEntity egg = (InfusedEggBlockEntity) level.getBlockEntity(pos);
                    assert egg != null;
                    EntityType<?> occupant = MobFluid.typeFromStack(handler.getFluidInTank(0));
                    assert occupant != null;
                    egg.setOwner(player);
                    egg.setOccupant(occupant);
                    handler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                }
                stopInjection(player);
            }
        }



    }

    // Both entity picking and block picking

    @NotNull
    public static HitResult superPick(LivingEntity picker) {
        double range = picker.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        HitResult blockHitResult = picker.pick(range, 0.0F, false);

        Vec3 eyePos = picker.getEyePosition(1.0F);
        Vec3 view = picker.getViewVector(1.0F);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                picker.level(),
                picker,
                eyePos,
                eyePos.add(view.scale(range)),
                picker.getBoundingBox().expandTowards(view.scale(range)),
                target -> !target.isSpectator() && target.isPickable());

        boolean foundBlock = blockHitResult.getType() != HitResult.Type.MISS;
        boolean foundEntity = entityHitResult != null;
        if (!foundBlock && foundEntity) return entityHitResult;
        if (!foundEntity && foundBlock) return blockHitResult;

        if (!foundEntity || !(entityHitResult.getEntity() instanceof LivingEntity)) return null;

        return blockHitResult.distanceTo(picker) < entityHitResult.distanceTo(picker) ?
                blockHitResult : entityHitResult;
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        //return new FluidHandlerItemStack(stack, 400);
        return new FilteredFluidStorage(new FluidStack(ModFluids.MOB_FLUID.get(), 1000), stack, 1000);
    }

    public void stopInjection(Player player) {
        stopInjection(player, 20);
    }

    public void stopInjection(Player player, int ticks) {
        player.stopUsingItem();
        player.getCooldowns().addCooldown(this, ticks);
    }

    @Nullable
    public IFluidHandlerItem getHandler(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(handler -> handler).orElse(null);
    }

    public void fillWithEntity(IFluidHandlerItem handler, LivingEntity victim, int amount) {
        handler.fill(MobFluid.fromEntity(victim, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    // Mode stuff

    public enum Mode {
        NONE,
        CLEAR,
        BOTTLE,
        ENTITY,
        BLANK_EGG,
        INFUSED_EGG;

        public boolean isContinuous() {
            return (this == ENTITY || this == BLANK_EGG || this == INFUSED_EGG);
        }

        public boolean isBlock() {
            return (this == BLANK_EGG || this == INFUSED_EGG);
        }

        @NotNull
        public static Mode fromString(String input) {
            return Enum.valueOf(Mode.class, input);
        }

        public static Mode fromContext(@Nullable HitResult result, @NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack) {
            // prioritize the hit result first, if we aren't crouching

            if (result instanceof EntityHitResult) return ENTITY;

            ItemStack offhandItem = entity.getOffhandItem();
            if (offhandItem.is(Items.GLASS_BOTTLE) || offhandItem.is(ModFluids.MOB_FLUID.getBottle())) {
                return BOTTLE;
            }

            if (!entity.isCrouching() && result != null) return Mode.fromHitResult(result, level);

            return NONE;
        }

        @NotNull
        public static Mode fromHitResult(@Nullable HitResult result, @NotNull Level level) {
            if (result instanceof EntityHitResult) {
                return ENTITY;

            } else if (result instanceof BlockHitResult blockHit && blockHit.getType() != HitResult.Type.MISS) {
                BlockState state = level.getBlockState(blockHit.getBlockPos());
                if (state == ModBlocks.BLANK_EGG.get().defaultBlockState()) {
                    return BLANK_EGG;

                } else if (state == ModBlocks.INFUSED_EGG.get().defaultBlockState()) {
                    return INFUSED_EGG;
                }
            }
            return NONE;
        }

        @NotNull
        public static Mode fromItem(ItemStack stack) {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains(MODE_PATH)) return NONE;

            return fromString(tag.getString(MODE_PATH));
        }

        public void saveToItem(ItemStack stack) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString(MODE_PATH, this.toString());
        }
    }
}
