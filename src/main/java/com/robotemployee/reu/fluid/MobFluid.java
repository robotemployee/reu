package com.robotemployee.reu.fluid;

import com.robotemployee.reu.core.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.jetbrains.annotations.NotNull;

public class MobFluid extends ForgeFlowingFluid {

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
