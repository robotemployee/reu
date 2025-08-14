package com.robotemployee.reu.extra;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
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
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.Objects;

public class CuriosCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void attachCurioToBacktank(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (!AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.matches(stack)) return;
        //LOGGER.info("attaching capability to the funny backtank");
        event.addCapability(CuriosCapability.ID_ITEM, CuriosApi.createCurioProvider(new ICurio() {
            final Logger LOGGER = LogUtils.getLogger();
            @Override
            public ItemStack getStack() {
                return stack;
            }

            @Override
            public boolean canEquip(SlotContext slotContext) {
                //LOGGER.info(slotContext.identifier());
                return Objects.equals(slotContext.identifier(), "back");
            }

            @Override
            public void curioTick(SlotContext context) {
                LivingEntity entity = context.entity();
                if (!(entity instanceof Player player)) return;

                Level level = entity.level();
                if (level.isClientSide()) return;
                if (level.getGameTime() % 20 != 0) return;
                BlockPos eyePos = BlockPos.containing(player.getEyePosition());
                if (!level.getBlockState(eyePos).isSuffocating(level, eyePos)) return;

                if (!BacktankUtil.hasAirRemaining(stack)) return;
                if (!(player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof DivingHelmetItem)) return;

                entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, false, false));
                BacktankUtil.consumeAir(entity, stack, 1);
            }
        }));
    }
}
