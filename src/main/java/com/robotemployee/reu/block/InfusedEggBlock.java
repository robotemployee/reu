package com.robotemployee.reu.block;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.block.entity.InfusedEggBlockEntity;
import com.robotemployee.reu.core.ModBlockEntities;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class InfusedEggBlock extends BaseEntityBlock {

    private static final int REGULAR_HATCH_TIME = 24000;
    private static final int BOOSTED_HATCH_TIME = 12000;
    private static final int RANDOM_HATCH_OFFSET = 300;

    Logger LOGGER = LogUtils.getLogger();
    public static final IntegerProperty HATCH = BlockStateProperties.HATCH;
    public InfusedEggBlock(Properties properties) {
        super(properties);
    }


    // Spray resulting items out everywhere
    @Override
    public void onRemove(@NotNull BlockState oldState, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (oldState.is(newState.getBlock())) return;
        InfusedEggBlockEntity blockEntity = (InfusedEggBlockEntity)level.getBlockEntity(pos);
        if (blockEntity == null) {
            LOGGER.warn("level.getBlockEntity() returned null on removal");
            return;
        }
        blockEntity.dropOnRemove(oldState, level, pos, newState, isMoving);
        super.onRemove(oldState, level, pos, newState, isMoving);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        if (level.isClientSide()) return null;

        if (type == ModBlockEntities.INFUSED_EGG_BLOCK_ENTITY.get()) {
            return (lvl, pos, st, blockEntity) -> {
                InfusedEggBlockEntity.tick(lvl, pos, st, (InfusedEggBlockEntity) blockEntity);
            };
        } else return null;
    }

    @Override
    @NotNull
    public InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult result) {
        LOGGER.info("block interacted with!");
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        LOGGER.info("now serverside");
        if (!(level.getBlockEntity(pos) instanceof InfusedEggBlockEntity infusedEggBlockEntity)) return InteractionResult.CONSUME;
        LOGGER.info("it's our block entity!");
        infusedEggBlockEntity.grow(10);
        LOGGER.info(String.valueOf(infusedEggBlockEntity.getPopulation()));
        LOGGER.info(String.valueOf(infusedEggBlockEntity.getOccupantString()));
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new InfusedEggBlockEntity(pos, state);
    }



    /*
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTicker(level, type, RobotEmployeeUtils.EGG_BLOCK_ENTITY);
    }

    private <T extends BlockEntity> BlockEntityTicker<T> createTicker(Level level, BlockEntityType<T> type, RegistryObject<BlockEntityType<EggBlockEntity>> eggBlockEntity) {
        return null;
    }*/

}
