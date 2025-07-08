package com.robotemployee.reu.item;

import com.robotemployee.reu.core.registry_help.generics.FilledBottleItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class MobFluidFilledBottleItem extends FilledBottleItem {
    public MobFluidFilledBottleItem(RegistryObject<Fluid> registryObject, Properties properties) {
        super(registryObject, properties);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(glassBottle)) {
                player.drop(glassBottle, false);
            }
        }

        return new ItemStack(Items.GLASS_BOTTLE);
    }
}
