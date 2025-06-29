package com.robotemployee.reu.item;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.block.entity.InfusedEggBlockEntity;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    Logger LOGGER = LogUtils.getLogger();

    public static final String MODE_PATH = "Mode";
    public static final String HELD_ENTITY_TYPE_PATH = "HeldEntityType";
    public static final String FILL_CURRENT_PATH = "FillCurrent";
    public static final String FILL_MAX_PATH = "FillMax";
    // note that blank egg filling is complete when it reaches 0; the number goes down
    public static final String FILL_BLANK_EGG_PATH = "FillBlankEgg";

    public static final List<String> CLEARED_PATHS = List.of(HELD_ENTITY_TYPE_PATH, FILL_CURRENT_PATH, FILL_MAX_PATH, FILL_BLANK_EGG_PATH);

    public final int FILL_PER_TICK = 1;

    @Override @NotNull
    public UseAnim getUseAnimation(@NotNull ItemStack itemStack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack) {
        return 72000;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        float fraction = (getMode(stack) == Mode.BLANK_EGG ? 1 - getFillFraction(stack, FILL_BLANK_EGG_PATH) : getFillFraction(stack, FILL_CURRENT_PATH));
        return Math.round(13.0F * fraction);
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return !isFilled(stack) && hasFluid(stack);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return 0xc5a3e0;
    }

    @Override @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        LOGGER.info("using");

        if (player.isCrouching()) {
            if (!level.isClientSide()) {
                setMode(stack, Mode.CLEAR);
                player.startUsingItem(hand);
            }
            return InteractionResultHolder.consume(stack);
        }

        return startPerforming(level, player, hand, stack);
    }

    public InteractionResultHolder<ItemStack> startPerforming(Level level, Player player, InteractionHand hand, ItemStack stack) {
        HitResult hitResult = superPick(player);
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) return InteractionResultHolder.fail(stack);
        if (hitResult instanceof BlockHitResult blockHitResult) {
            LOGGER.info("hit a block");
            BlockState state = level.getBlockState(blockHitResult.getBlockPos());
            if (state.is(RobotEmployeeUtils.INFUSED_EGG.get())) {
                return beginInteractWithInfusedEgg(level, player, hand, stack);
            }
            else if (state.is(RobotEmployeeUtils.BLANK_EGG.get())) {
                return beginInteractWithBlankEgg(level, player, hand, stack);
            }
        } else if (hitResult instanceof EntityHitResult entityHitResult) {
            LOGGER.info("hit an entity");
            if (entityHitResult.getEntity() instanceof LivingEntity entity) return beginInteractWithEntity(level, player, hand, stack, entity);
        }
        return InteractionResultHolder.fail(stack);
    }



    public InteractionResultHolder<ItemStack> beginInteractWithEntity(Level level, Player player, InteractionHand hand, ItemStack stack, Entity entity) {
        LOGGER.info("beginning interaction with entity");
        if (isFilled(stack) || hasFluid(stack) && getHeldEntityType(stack) != entity.getType()) {
            // no entity mixing (for now)
            return InteractionResultHolder.fail(stack);
        } else if (!level.isClientSide()) {
            setHeldEntityType(stack, entity.getType());
            setMode(stack, Mode.ENTITY);
            // TODO: add required fill values for each entity
            setFillAmount(stack, 100, FILL_MAX_PATH);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    public InteractionResultHolder<ItemStack> beginInteractWithBlankEgg(Level level, Player player, InteractionHand hand, ItemStack stack) {
        LOGGER.info("beginning interaction with blank egg");
        if (isFilled(stack)) {
            if (!level.isClientSide()) {
                // blank egg filling begins at max and goes down to 0 over time from there
                setFillAmount(stack, getFillAmount(stack, FILL_MAX_PATH), FILL_BLANK_EGG_PATH);
                setMode(stack, Mode.BLANK_EGG);
            }
        } else return InteractionResultHolder.fail(stack);

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    public InteractionResultHolder<ItemStack> beginInteractWithInfusedEgg(Level level, Player player, InteractionHand hand, ItemStack stack) {
        LOGGER.info("beginning interaction with infused egg");
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(@NotNull Level level, @NotNull LivingEntity entity, @NotNull ItemStack stack, int remainingTime) {
        switch (getMode(stack)) {
            case NONE -> {
                LOGGER.info("mode is none; stopping");
                entity.stopUsingItem();
                // do nothing. lol.?
            }
            case CLEAR -> {
                LOGGER.info("mode is clear");
                if (entity.isCrouching()) {
                    drainInSteps(stack, 60);
                    if (getFillAmount(stack, FILL_CURRENT_PATH) == 0) entity.stopUsingItem();
                } else {
                    entity.stopUsingItem();
                }
            }
            case ENTITY -> {
                LOGGER.info("mode is entity");
                HitResult hitResult = superPick(entity);
                if (!(hitResult instanceof EntityHitResult entityHitResult)) return;
                EntityType<?> entityType = entityHitResult.getEntity().getType();
                if (entityType == getHeldEntityType(stack)) {
                    if (!level.isClientSide()) modifyFill(stack, FILL_PER_TICK, FILL_CURRENT_PATH);

                    if (isFilled(stack)) {
                        entity.stopUsingItem();
                    }
                }
            }
            case BLANK_EGG -> {
                LOGGER.info("mode is blank egg");
                HitResult hitResult = superPick(entity);
                if (!(hitResult instanceof BlockHitResult blockHitResult)) return;
                BlockPos pos = blockHitResult.getBlockPos();
                BlockState state = level.getBlockState(pos);
                if (state != RobotEmployeeUtils.BLANK_EGG.get().defaultBlockState()) {
                    entity.stopUsingItem();
                }

                if (getFillAmount(stack, FILL_BLANK_EGG_PATH) > 0) {
                    // aim to have it full emptied in 5 seconds
                    if (!level.isClientSide()) modifyFill(stack, -(int)Math.ceil(getFillAmount(stack, FILL_MAX_PATH) / (float)100), FILL_BLANK_EGG_PATH);
                } else {
                    // filling has been successful
                    if (!(entity instanceof Player player)) return;
                    // replacing the blank egg with an infused egg!
                    if (level.isClientSide()) return;
                    BlockState newState = RobotEmployeeUtils.INFUSED_EGG.get().defaultBlockState();
                    level.setBlockAndUpdate(pos, newState);
                    InfusedEggBlockEntity blockEntity = (InfusedEggBlockEntity)level.getBlockEntity(pos);
                    assert blockEntity != null; // look i , just don't like the yellow lines. they peeve me
                    EntityType<?> type = getHeldEntityType(stack);
                    assert type != null;
                    blockEntity.setup(player, type);
                    clear(stack);
                    entity.stopUsingItem();
                    // i just realized that maybe
                    // it will . work ? ? ?????????
                }
            }
            case INFUSED_EGG -> {
                LOGGER.info("mode is infused egg");
            }
        }

    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        LOGGER.info("stopped using");
        setMode(stack, Mode.NONE);
    }

    // NBT manipulation stuff

    public void clear(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        for (String path : CLEARED_PATHS) tag.remove(path);
    }

    public void setFillAmount(ItemStack stack, int amount, String path) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(path, amount);
        stack.setTag(tag);
    }

    public int getFillAmount(ItemStack stack, String path) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(path)) return 0;
        return tag.getInt(path);
    }

    public float getFillFraction(ItemStack stack, String path) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(path) || !tag.contains(FILL_MAX_PATH)) return 0;
        return (float)tag.getInt(path) / tag.getInt(FILL_MAX_PATH);
    }

    public void modifyFill(ItemStack stack, int amount, String path) {
        CompoundTag tag = stack.getOrCreateTag();
        int fillAmount = tag.getInt(path);
        fillAmount += amount;
        fillAmount = Math.max(fillAmount, 0);
        tag.putInt(path, fillAmount);
        stack.setTag(tag);
    }

    // if you execute this function 'amount' times, the injector is completely drained
    public void drainInSteps(ItemStack stack, int amount) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(FILL_MAX_PATH)) return;
        modifyFill(stack, -(int)Math.ceil(tag.getInt(FILL_MAX_PATH) / (float)amount), FILL_CURRENT_PATH);
    }

    public boolean isFilled(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(FILL_CURRENT_PATH) || !tag.contains(FILL_MAX_PATH)) return false;
        return tag.getInt(FILL_CURRENT_PATH) >= tag.getInt(FILL_MAX_PATH);
    }

    public boolean hasFluid(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(FILL_CURRENT_PATH)) return false;
        return tag.getInt(FILL_CURRENT_PATH) > 0;
    }

    public void setHeldEntityType(ItemStack stack, EntityType<?> entityType) {
        LOGGER.info("Setting held entity type: " + entityType);
        ResourceLocation loc = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        if (loc == null) return;

        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(HELD_ENTITY_TYPE_PATH, loc.toString());
        stack.setTag(tag);
    }

    public EntityType<?> getHeldEntityType(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        LOGGER.info("Attempting to retrieve held entity type");
        if (tag == null || !tag.contains(HELD_ENTITY_TYPE_PATH)) return null;
        LOGGER.info("Tag is valid");
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(tag.getString(HELD_ENTITY_TYPE_PATH)));
        LOGGER.info("Retrieving held entity type: " + tag.getString(HELD_ENTITY_TYPE_PATH) + " -> " + type);
        return type;
    }

    // Both entity picking and block picking

    public HitResult superPick(LivingEntity entity) {
        double range = entity.getAttributeValue(ForgeMod.BLOCK_REACH.get());
        HitResult result = entity.pick(range, 0.0F, false);
        if (result.getType() == HitResult.Type.MISS) {
            Vec3 eyePos = entity.getEyePosition();
            Vec3 view = entity.getViewVector(1.0F);
            EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                    entity.level(),
                    entity,
                    eyePos,
                    eyePos.add(view.scale(range)),
                    entity.getBoundingBox().expandTowards(view.scale(range)),
                    target -> !target.isSpectator() && target.isPickable());
            if (entityHitResult != null) result = entityHitResult;
        }
        return result;
    }

    // Mode stuff

    public void setMode(ItemStack stack, Mode mode) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(MODE_PATH, mode.toInt());
        stack.setTag(tag);
    }

    public Mode getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(MODE_PATH)) return Mode.NONE;
        return modeFromInt(tag.getInt(MODE_PATH));
    }

    private final Mode[] values = Mode.values();
    public Mode modeFromInt(int i) {
        return values[i];
    }

    public enum Mode {
        NONE,
        CLEAR,
        ENTITY,
        BLANK_EGG,
        INFUSED_EGG;

        public int toInt() {
            return ordinal();
        }
    }
}
