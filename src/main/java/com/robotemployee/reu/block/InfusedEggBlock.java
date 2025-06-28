package com.robotemployee.reu.block;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.block.entity.InfusedEggBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;

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
        dropOnRemove(oldState, level, pos, newState, isMoving);
        super.onRemove(oldState, level, pos, newState, isMoving);
    }

    public void dropOnRemove(@NotNull BlockState oldState, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        LOGGER.info("Block was removed!");
        LOGGER.info(oldState.getBlock().getDescriptionId() + " -> " + newState.getBlock().getDescriptionId());
        if (oldState.getBlock() == newState.getBlock() || level.isClientSide()) return;
        LOGGER.info("Passed first check");
        //LOGGER.info(String.valueOf(level.getBlockEntity(pos) != null));
        if (!(level.getBlockEntity(pos) instanceof InfusedEggBlockEntity infusedEggBlockEntity)) return;
        LOGGER.info("and it knows it's a block entity!");
        Container dropsContainer = infusedEggBlockEntity.getDropsContainer((ServerLevel)level);
        if (dropsContainer == null) return;
        LOGGER.info("Size: " + dropsContainer.getContainerSize());
        Containers.dropContents(level, pos, dropsContainer);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity entity, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof InfusedEggBlockEntity infusedEggBlockEntity)) return;
        infusedEggBlockEntity.setOccupant(EntityType.CHICKEN);
        // if it was placed autonomously, just get *a* player
        if (entity instanceof Player player) infusedEggBlockEntity.setOwner(player);
        else infusedEggBlockEntity.setOwner(level.players().get(0));
    }

    @Override
    public InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult result) {
        LOGGER.info("block interacted with!");
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        LOGGER.info("now serverside");
        if (!(level.getBlockEntity(pos) instanceof InfusedEggBlockEntity infusedEggBlockEntity)) return InteractionResult.CONSUME;
        LOGGER.info("it's our block entity!");
        infusedEggBlockEntity.grow(10);
        LOGGER.info(String.valueOf(infusedEggBlockEntity.getPopulation()));
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
