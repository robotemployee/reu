package com.robotemployee.reu.extra;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.Objects;

public class CuriosCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void backtankAccessory(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (!AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.matches(stack)) return;
        //LOGGER.info("attaching capability to the funny backtank");

        AccessoriesAPI.registerAccessory(event.getObject().getItem(), new Accessory() {

            @Override
            public boolean canEquip(ItemStack stack, SlotReference reference) {
                return Objects.equals(reference.slotName(), "back");
            }

            @Override
            public void tick(ItemStack stack, SlotReference reference) {

                LivingEntity entity = reference.entity();
                if (!(entity instanceof Player player)) return;

                Level level = entity.level();
                if (level.isClientSide()) return;
                if (level.getGameTime() % 20 != 0) return;
                BlockPos eyePos = BlockPos.containing(player.getEyePosition());
                if (!level.getBlockState(eyePos).isSuffocating(level, eyePos)) return;

                if (!BacktankUtil.hasAirRemaining(stack)) return;
                if (!(player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof DivingHelmetItem)) return;
                if (player.getItemBySlot(EquipmentSlot.CHEST).is(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.tag)) return;

                entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, false, false));
                BacktankUtil.consumeAir(entity, stack, 1);
            }
        });
    }
}
