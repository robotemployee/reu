package com.robotemployee.reu.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

public class ModFluidTypes {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, RobotEmployeeUtils.MODID);

    public static final RegistryObject<FluidType> MOB_FLUID = FLUID_TYPES.register("mob_fluid", () ->
            new FluidType(FluidType.Properties.create()
                    .descriptionId("block.reu.mob_fluid")
                    .canPushEntity(true)
                    .canSwim(true)
                    .canDrown(true)
                    .fallDistanceModifier(0)
                    .canExtinguish(true)
                    .canConvertToSource(false)
                    .supportsBoating(true)
                    .density(3000)
                    .viscosity(6000)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH))
            {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer)
                {
                    consumer.accept(new IClientFluidTypeExtensions()
                    {
                        private static final ResourceLocation UNDERWATER_LOCATION = new ResourceLocation(RobotEmployeeUtils.MODID, "textures/misc/mob_fluid_overlay.png"),
                                MOB_FLUID_STILL = new ResourceLocation("block/water_still"),
                                MOB_FLUID_FLOW = new ResourceLocation("block/water_flow"),
                                MOB_FLUID_OVERLAY = new ResourceLocation("block/water_overlay");

                        @Override
                        public ResourceLocation getStillTexture()
                        {
                            return MOB_FLUID_STILL;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture()
                        {
                            return MOB_FLUID_FLOW;
                        }

                        @Override
                        public ResourceLocation getOverlayTexture()
                        {
                            return MOB_FLUID_OVERLAY;
                        }

                        @Override
                        public ResourceLocation getRenderOverlayTexture(Minecraft mc)
                        {
                            return UNDERWATER_LOCATION;
                        }

                        @Override
                        public int getTintColor() {return 0xE5DE92E5;}

                        @Override
                        public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos)
                        {
                            return getTintColor();
                        }
                    });
                }
            }


    );

    public static int getTintFromType(FluidType type) {
        if (type == MOB_FLUID.get()) return 0xE5DE92E5;
        return -1;
    }
}
