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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
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

    // so.
    // anything related to the functionality I think is alright
    // anything related to the aesthetics (like the ascii art) sucks. do noooot read do not read. skip. skip. skip.


    public InjectorItem(Item.Properties properties) {
        super(properties);
    }

    static Logger LOGGER = LogUtils.getLogger();

    public static final String MODE_PATH = "Mode";
    public static final String POSITION_PATH = "Position";

    public static final String IS_USING_PATH = "Using";

    public static final int USE_DURATION = 72000;

    public static final int BLANK_EGG_FILL_TICKS = 40;

    // amount of damage this does while filling
    public static final float DAMAGE = 1.5f;
    public final int FILL_PER_HP = 20;

    // this is for how many segments there are in the tooltip
    public static final int TUBE_LENGTH = 4;

    public static final int CAPACITY = 250;

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag flag) {
        // technical debt supreme. unless i just don't touch this ever again
        final String[] levels = {"    .", "░:░", "▒▒",};
        IFluidHandlerItem handler = getHandler(stack);

        FluidStack fluid = handler.getFluidInTank(0);
        float fillFraction = ((float)fluid.getAmount() / handler.getTankCapacity(0));
        int fill = (int)Math.ceil(TUBE_LENGTH * fillFraction);
        //if (fill == TUBE_LENGTH && fillFraction < 0.85) fill--;

        components.add(Component.literal("§b  Δ"));
        components.add(Component.literal("/`§8~\\"));

        int[] indexes = new int[TUBE_LENGTH];

        boolean isFull = fillFraction == 1;

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

        String typeAftertouch = String.format("§9=§3%s", !fluid.isEmpty() ? fluid.getTag().getString(MobFluid.ENTITY_TYPE_KEY) : "(empty)");

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
                    border + purpleColorFromFill(fill, visual, isFull) + levels[visual] + "§8" + border + aftertouch

            ));
            showedPercent = shouldShowPercent;
            firstTime = false;
        }

        components.add(Component.literal("`§8`|``|``" + (showedPercent ? typeAftertouch : "")));
        components.add(Component.literal(String.format("-§8'-'- %smb", fluid.getAmount())));
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
    public Component getName(@NotNull ItemStack stack) {
        IFluidHandlerItem handler = getHandler(stack);
        FluidStack fluid = handler.getFluidInTank(0);
        CompoundTag tag = fluid.getTag();
        MutableComponent base = Component.translatable(this.getDescriptionId(stack));
        if (fluid.isEmpty() || tag == null || !tag.contains(MobFluid.ENTITY_TYPE_KEY) || fluid.getAmount() == CAPACITY) return base;
        Component appended = Component.literal(String.format(" §b(§r%s§7)", getFancySplitEntityType(tag, handler)));
        return base.append(appended);
    }

    // shut up. this was afst

    @NotNull
    public String getFancySplitEntityType(@NotNull CompoundTag tag, @NotNull IFluidHandlerItem handler) {
        FluidStack fluid = handler.getFluidInTank(0);

        String type = tag.getString(MobFluid.ENTITY_TYPE_KEY);
        float fillFraction = (float)fluid.getAmount() / handler.getTankCapacity(0);

        int index = Mth.clamp((int)Math.ceil(type.length() * fillFraction), 0, type.length());
        String before = type.substring(0, index);
        String after = type.substring(index);
        return greenColorFromFill(fillFraction) + before + "§7" + after;
    }

    @Override @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionHand offhand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        HitResult hitResult = superPick(player);
        Mode mode = Mode.fromContext(hitResult, level, player, stack);

        mode.saveToItem(stack);

        if (mode == Mode.BOTTLE) {
            LOGGER.info("bottle");
            IFluidHandlerItem handler = getHandler(stack);
            ItemStack offhandItem = player.getItemInHand(offhand);
            if (offhandItem.is(Items.GLASS_BOTTLE)) {
                if (handler.getFluidInTank(0).getAmount() < 250) return InteractionResultHolder.fail(stack);
                if (!level.isClientSide()) {
                    handler.drain(250, IFluidHandler.FluidAction.EXECUTE);
                }
                player.setItemInHand(offhand, MobFluid.createBottle(handler.getFluidInTank(0)));
            } else if (offhandItem.is(ModFluids.MOB_FLUID.getBottle())) {
                IFluidHandlerItem offhandHandler = getHandler(offhandItem);
                if (handler.fill(offhandHandler.getFluidInTank(0), IFluidHandler.FluidAction.SIMULATE) < 250) return InteractionResultHolder.fail(stack);
                if (!level.isClientSide()) {
                    handler.fill(offhandHandler.getFluidInTank(0), IFluidHandler.FluidAction.EXECUTE);
                }
                player.setItemInHand(offhand, new ItemStack(Items.GLASS_BOTTLE));
            }
            return InteractionResultHolder.consume(stack);
        }

        if (mode == Mode.ENTITY) {
            IFluidHandlerItem handler = getHandler(stack);
            EntityType<?> contained = MobFluid.typeFromStack(handler.getFluidInTank(0));
            LivingEntity victim = (LivingEntity)((EntityHitResult)hitResult).getEntity();

            if (contained == null) {
                fillWithEntity(handler, victim, 1);
            } else if (victim.getType() != contained || handler.getFluidInTank(0).getAmount() == handler.getTankCapacity(0)) {

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
            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean(IS_USING_PATH, true);
            stack.setTag(tag);
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

                if (!level.isClientSide()) {
                    // serverside

                    if (handler.getFluidInTank(0).getAmount() == 1000) stopInjection(player);


                    //LOGGER.info("Invulnerable time: " + victim.invulnerableTime);
                    if (victim.invulnerableTime == 0 && !entity.isInvulnerable()) {
                        victim.hurt(victim.damageSources().playerAttack(player), DAMAGE);
                        fillWithEntity(handler, victim, (int)Math.round(DAMAGE * FILL_PER_HP));
                        // if we're full
                        if (handler.getFluidInTank(0).getAmount() == handler.getTankCapacity(0)) {
                            player.playSound(SoundEvents.NOTE_BLOCK_BELL.get());
                            stopInjection(player);
                        }
                    }
                } /*else {
                    // clientside
                    FluidStack fluid = handler.getFluidInTank(0);
                    CompoundTag fluidTag = fluid.getTag();
                    if (tag != null && tag.contains(MobFluid.ENTITY_TYPE_KEY)) {
                        player.displayClientMessage(Component.literal("MESSAGE: " + getFancySplitEntityType(fluidTag, handler)  ), false);
                    }
                }*/
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

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        super.onStopUsing(stack, entity, count);
        if (entity instanceof Player) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean(IS_USING_PATH, false);
            stack.setTag(tag);
        }
    }

    @Override @NotNull
    public ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        if (entity instanceof Player player) stopInjection(player);
        return stack;
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

        if (!foundEntity || !(entityHitResult.getEntity() instanceof LivingEntity)) return blockHitResult;

        return blockHitResult.distanceTo(picker) < entityHitResult.distanceTo(picker) ?
                blockHitResult : entityHitResult;
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        //return new FluidHandlerItemStack(stack, 400);
        return new FilteredFluidStorage(new FluidStack(ModFluids.MOB_FLUID.get(), CAPACITY), stack, CAPACITY);
    }

    public void stopInjection(Player player) {
        stopInjection(player, 20);
    }

    public void stopInjection(Player player, int ticks) {
        ItemStack stack = player.getUseItem();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(IS_USING_PATH, false);
        stack.setTag(tag);
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

    private String purpleColorFromFill(int fill, int visual, boolean isFull) {
        if (visual == 0 || fill == 0) {
            return "§8";
        } else if (isFull) {
            return "§5";
        } else if (fill > TUBE_LENGTH / 4) {
            return "§6";
        } else {
            return "§4";
        }
    }

    private String greenColorFromFill(float fraction) {
        if (fraction == 0) {
            return "§8";
        } else if (fraction == 1) {
            return "§a";
        } else if (fraction > 0.25) {
            return "§6";
        } else {
            return "§4";
        }
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

        public boolean isBlock() { return (this == BLANK_EGG || this == INFUSED_EGG); }

        @NotNull
        public static Mode fromString(String input) { return Enum.valueOf(Mode.class, input); }

        public static Mode fromContext(@Nullable HitResult result, @NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack) {
            // prioritize the hit result first, if we aren't crouching

            if (result instanceof EntityHitResult) return ENTITY;

            InteractionHand used = entity.getUsedItemHand();

            ItemStack offhandItem = used == InteractionHand.MAIN_HAND ? entity.getOffhandItem() : entity.getMainHandItem();
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
