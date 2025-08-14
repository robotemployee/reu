package com.robotemployee.reu.extra;

import com.robotemployee.reu.core.registry.ModDamageTypes;
import com.robotemployee.reu.core.registry.ModItems;
import com.robotemployee.reu.core.registry.ModMobEffects;
import com.robotemployee.reu.mobeffect.TummyAcheMobEffect;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.armor.DivingHelmetItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.Optional;

public class TummyAcheEvents {

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        suffocateOnAttack(event);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        cureOnDeath(event);
    }

    @SubscribeEvent
    public static void onPlayerFinishedUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (accelerate(event)) return;
        if (decelerate(event)) return;
        if (cureOnMiraclePill(event)) return;
    }

    public static final int AIR_USE_MULTIPLIER = 4;
    public static final int AIR_USE_ON_SPRINT = 1;
    public static final int AIR_USE_ON_USE = 2;
    @SubscribeEvent
    public static void onPlayerBreathe(LivingBreatheEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) return;
        if (!victim.hasEffect(ModMobEffects.TUMMY_ACHE.get())) return;
        if (!(victim instanceof Player player)) return;

        BlockPos eyePos = BlockPos.containing(player.getEyePosition());

        // vastly increase the amount of air you use underwater / in the nether / etc
        // checking if the position is suffocating because we need to get past backtanks
        if (level.getBlockState(eyePos).isSuffocating(level, eyePos)) {
            // increase backtank air consumption if we have one on and are using it
            ICuriosItemHandler handler = CuriosApi.getCuriosInventory(player).resolve().get();
            Optional<SlotResult> backtankOptional = handler.findFirstCurio(s -> s.is(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.tag));
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            ItemStack curio = backtankOptional.map(SlotResult::stack).orElse(ItemStack.EMPTY);
            ItemStack backtank = !chest.isEmpty() ? chest : curio;

            if (backtank.is(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.tag) && player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof DivingHelmetItem) {
                BacktankUtil.consumeAir(victim, backtank, AIR_USE_MULTIPLIER - 1);
            } else {
                event.setConsumeAirAmount(event.getConsumeAirAmount() * 4);
            }

            // apply blindness if the player's air supply is very low
            if (player.getAirSupply() / (float)player.getMaxAirSupply() < 0.1f) player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 240, 0, false, false, true));
        }

        boolean sappingAir = player.isSprinting() || player.isUsingItem();
        boolean stopRefill = sappingAir || player.isCrouching();

        if (sappingAir) event.setCanBreathe(false);
        if (stopRefill) {
            event.setRefillAirAmount(0);
            if (player.isSprinting()) event.setConsumeAirAmount(event.getConsumeAirAmount() + AIR_USE_ON_SPRINT);
            if (player.isUsingItem()) event.setConsumeAirAmount(event.getConsumeAirAmount() + AIR_USE_ON_USE);

        } //else if (event.canBreathe()) event.setRefillAirAmount(event.getRefillAirAmount() / 2);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        vibeChecker(event); // hi fern !! !!! :3
    }

    public static boolean suffocateOnAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();

        Level level = victim.level();

        if (level.isClientSide()) return false;

        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return false;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player)) attacker = source.getDirectEntity();
        if (!(attacker instanceof Player player)) return false;

        if (!player.hasEffect(ModMobEffects.TUMMY_ACHE.get())) return false;
        int airRemoved = Math.max((int)Math.floor(event.getAmount()), 60);
        player.setAirSupply(player.getAirSupply() - airRemoved);
        return true;
    }

    public static boolean cureOnDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) return false;
        if (!victim.hasEffect(ModMobEffects.TUMMY_ACHE.get())) return false;

        if (!(victim instanceof Player player)) return false;

        TummyAcheMobEffect.cure(victim);

        if (!event.getSource().is(ModDamageTypes.ASBESTOSIS)) return false;

        player.addItem(new ItemStack(ModItems.MUSIC_DISC_GIANT_ROBOTS.get()));

        level.playSound(null, victim.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS);

        return true;
    }

    public static boolean accelerate(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide()) return false;

        ItemStack used = event.getItem();
        if (!used.is(ModItems.INTERESTING_BREAD.get())) return false;

        CompoundTag info = TummyAcheMobEffect.getInfoFrom(entity);
        float severity = TummyAcheMobEffect.getSeverity(entity, info);
        //LOGGER.info("Severity: " + severity);


        TummyAcheMobEffect.bestowInterestingBreadBoons(entity, severity);

        RandomSource random = level.getRandom();


        if (entity.hasEffect(ModMobEffects.TUMMY_ACHE.get())) {
            boolean doALot = random.nextFloat() < 0.2;
            if (severity > 0.5 && random.nextFloat() < Math.pow(severity, 2)) TummyAcheMobEffect.inflictAilment(entity, severity);
            CompoundTag newInfo = TummyAcheMobEffect.advanceTime(info, entity, TummyAcheMobEffect.TICKS_ACCELERATED_ON_BREAD * (doALot ? 8 : 1));
            float newSeverity = TummyAcheMobEffect.getSeverity(entity, newInfo);
            entity.sendSystemMessage(Component.literal(String.format((doALot ? "§3Your condition accelerates a lot." : "§3Your condition accelerates.") + (newSeverity > 0 ? " §8(%.2f)" : ""), newSeverity)));

            //LOGGER.info("Severity:" + severity);
        }

        if (entity.hasEffect(ModMobEffects.TUMMY_ACHE.get())) return true;
        // evil
        TummyAcheMobEffect.inflictUpon(entity);
        return true;
    }

    public static boolean decelerate(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide()) return false;

        ItemStack used = event.getItem();
        if (!used.is(ModItems.ONE_DAY_BLINDING_STEW.get())) return false;

        if (!entity.hasEffect(ModMobEffects.TUMMY_ACHE.get())) return false;

        TummyAcheMobEffect.advanceTime(entity, TummyAcheMobEffect.TICKS_DECELERATED_ON_STEW);

        entity.sendSystemMessage(Component.literal(String.format("§3Your condition improves. §8(%.2f)", TummyAcheMobEffect.getSeverity(entity))));

        return true;
    }

    public static boolean cureOnMiraclePill(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide()) return false;

        ItemStack used = event.getItem();
        if (!used.is(ModItems.MIRACLE_PILL.get())) return false;

        if (!entity.hasEffect(ModMobEffects.TUMMY_ACHE.get())) return false;

        TummyAcheMobEffect.cure(entity);
        return true;
    }

    public static boolean vibeChecker(TickEvent.PlayerTickEvent event) {
        // Renew Tummy Ache if the player should have it. There is no curing asbestosis. The player must die.
        Player player = event.player;
        Level level = player.level();
        if (level.isClientSide()) return false;

        if (level.getGameTime() % 100 > 0) return false;

        if (player.hasEffect(ModMobEffects.TUMMY_ACHE.get())) return false;

        CompoundTag info = TummyAcheMobEffect.getInfoFrom(player);
        //LOGGER.info(tag.getAsString());
        if (info.isEmpty()) return false;

        boolean applicable = TummyAcheMobEffect.getIsApplied(info);
        if (!applicable) return false;
        // it has somehow been cured and we need to renew it

        player.addEffect(TummyAcheMobEffect.defaultInstance());

        MobEffectInstance instance = player.getEffect(ModMobEffects.TUMMY_ACHE.get());

        player.sendSystemMessage(Component.literal("§3There is no cure."));
        TummyAcheMobEffect.inflictAilment(player);
        if (instance == null) return true;
        instance.tick(player, () -> {});

        return true;
    }
}
