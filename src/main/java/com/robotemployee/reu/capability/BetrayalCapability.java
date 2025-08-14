package com.robotemployee.reu.capability;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;

public class BetrayalCapability implements ICapabilitySerializable<CompoundTag> {

    static Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation ID = new ResourceLocation(RobotEmployeeUtils.MODID, "betrayal_handler");
    public BetrayalHandler handler;
    private final LazyOptional<BetrayalHandler> OPTIONAL;
    public static final Capability<BetrayalHandler> CAPABILITY = CapabilityManager.get(new CapabilityToken<BetrayalHandler>() {});

    public BetrayalCapability(BlockPos homePosition) {
        getOrCreateHandler(homePosition);
        OPTIONAL = LazyOptional.of(() -> handler);
    }

    public BetrayalHandler getOrCreateHandler(BlockPos pos) {
        if (handler == null) {
            //LOGGER.info("Obtaining handler with position " + pos.asLong());
            handler = new BetrayalHandler(pos);
        }
        return handler;
    }

    @Override
    public CompoundTag serializeNBT() {
        return handler.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        handler.deserializeNBT(tag);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return (cap == CAPABILITY) ? OPTIONAL.cast() : LazyOptional.empty();
    }

    public static class BetrayalHandler implements INBTSerializable<CompoundTag> {
        public static final String BORN_PATH = "BornPosition";

        Logger LOGGER = LogUtils.getLogger();
        public static final int BETRAYAL_DISTANCE = 200;

        protected BlockPos homePosition;

        public BetrayalHandler(BlockPos homePosition) {
            this.homePosition = homePosition;
        }

        public BlockPos getHomePosition() {
            return homePosition;
        }

        public void setHomePosition(BlockPos homePosition) {
            this.homePosition = homePosition;
        }

        public boolean isBeingBetrayed(LivingEntity entity, Player killer) {
            if (Math.pow(entity.getX() - homePosition.getX(), 2) + Math.pow(entity.getZ() - homePosition.getZ(), 2) < Math.pow(BETRAYAL_DISTANCE, 2)) return false;
            BetrayalType betrayalType = getBetrayalType(entity);

            switch (betrayalType)  {
                case GOLEM -> {
                    return true;
                }
                case TAMABLE -> {
                    TamableAnimal tamable = (TamableAnimal) entity;
                    UUID owner = tamable.getOwnerUUID();
                    if (owner == null) return false;
                    return owner.equals(killer.getUUID());
                }
            }

            return true;
        }

        public static BetrayalType getBetrayalType(Entity entity) {
            if (entity instanceof SnowGolem || entity instanceof IronGolem) return BetrayalType.GOLEM;
            if (entity instanceof TamableAnimal) return BetrayalType.TAMABLE;
            return BetrayalType.INVALID;
        }

        // this is for when you're only checking if something can be betrayed - e.g attaching a capability
        public static boolean canBeBetrayed(Entity entity) {
            return getBetrayalType(entity) != BetrayalType.INVALID;
        }

        public boolean hasHomePosition() {
            return homePosition != BlockPos.ZERO;
        }

        public enum BetrayalType {
            GOLEM,
            TAMABLE,
            INVALID
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putLong(BORN_PATH, homePosition.asLong());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            homePosition = BlockPos.of(tag.getLong(BORN_PATH));
        }
    }

}