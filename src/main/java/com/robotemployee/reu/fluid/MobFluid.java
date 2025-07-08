package com.robotemployee.reu.fluid;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.capability.FilteredSimpleFluidStorage;
import com.robotemployee.reu.core.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class MobFluid extends ForgeFlowingFluid {

    public static Logger LOGGER = LogUtils.getLogger();

    public static final String ENTITY_TYPE_KEY = "entity_type";

    public MobFluid(Properties properties) {
        super(properties);
    }

    @Override
    @NotNull
    public Fluid getSource() {
        return ModFluids.MOB_FLUID.getSource();
    }

    @Override
    protected boolean canConvertToSource(@NotNull Level level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(@NotNull LevelAccessor accessor, @NotNull BlockPos pos, @NotNull BlockState state) {

    }

    @Override
    protected int getSlopeFindDistance(@NotNull LevelReader reader) {
        return 4;
    }

    @Override
    protected int getDropOff(@NotNull LevelReader reader) {
        return 1;
    }

    @Override
    public Item getBucket() {
        return ModFluids.getBucketFromFluid(this);
    }

    @Override
    protected boolean canBeReplacedWith(@NotNull FluidState state, @NotNull BlockGetter getter, @NotNull BlockPos pos, @NotNull Fluid fluid, @NotNull Direction direction) {
        return false;
    }

    @Override
    public int getTickDelay(@NotNull LevelReader reader) {
        return 0;
    }

    @Override
    protected float getExplosionResistance() {
        return 0;
    }

    @Override
    @NotNull
    protected BlockState createLegacyBlock(@NotNull FluidState state) {
        return ModFluids.MOB_FLUID.getBlock().defaultBlockState().setValue(LEVEL, state.getValue(LEVEL));
    }

    @Override
    public boolean isSource(@NotNull FluidState state) {
        return getAmount(state) == 9;
    }

    @Override
    public int getAmount(FluidState state) {
        return state.getValue(LEVEL);
    }

    public static FluidStack fromEntity(@NotNull EntityType<?> type, int amount) {
        FluidStack result = new FluidStack(ModFluids.MOB_FLUID.get(), amount);
        CompoundTag tag = new CompoundTag();
        String typeKey = ForgeRegistries.ENTITY_TYPES.getKey(type).toString();
        //LOGGER.info(typeKey);
        tag.putString(ENTITY_TYPE_KEY, typeKey);
        result.setTag(tag);
        return result;
    }

    @NotNull
    public static FluidStack fromEntity(@NotNull LivingEntity entity, int amount) {
        return fromEntity(entity.getType(), amount);
    }

    @Nullable
    public static EntityType<?> typeFromStack(@NotNull FluidStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getFluid().isSame(ModFluids.MOB_FLUID.get())) return null;
        CompoundTag tag = stack.getTag();
        if (!tag.contains(ENTITY_TYPE_KEY)) return null;
        return ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(tag.getString(ENTITY_TYPE_KEY)));
    }

    @Nullable
    public static ItemStack createBottle(@NotNull FluidStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ENTITY_TYPE_KEY)) return null;

        ItemStack newborn = new ItemStack(ModFluids.MOB_FLUID.getBottle());
        FilteredSimpleFluidStorage handler = (FilteredSimpleFluidStorage)newborn.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                .map(h -> h).orElse(null);

        handler.setFluid(stack);
        return newborn;
    }

    public static class Flowing extends MobFluid {
        protected Flowing(Properties properties) {
            super(properties);
        }

        protected void createFluidStateDefinition(@NotNull StateDefinition.Builder<Fluid, FluidState> stateDefinition) {
            super.createFluidStateDefinition(stateDefinition);
            stateDefinition.add(LEVEL);
        }

        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        public boolean isSource(@NotNull FluidState state) {
            return false;
        }
    }

    public static class Source extends MobFluid {
        protected Source(Properties properties) {
            super(properties);
        }

        public int getAmount(FluidState state) {
            return 8;
        }

        public boolean isSource(@NotNull FluidState state) {
            return true;
        }
    }
}
