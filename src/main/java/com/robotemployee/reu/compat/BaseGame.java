package com.robotemployee.reu.compat;

import com.github.alexthe666.alexsmobs.entity.EntityGorilla;
import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.ModAdvancements;
import com.robotemployee.reu.core.ModItems;
import com.supermartijn642.rechiseled.ChiselItem;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public class BaseGame {

    static Logger LOGGER = LogUtils.getLogger();
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().is(DamageTypes.SONIC_BOOM)) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            return;
        }

        LivingEntity victim = event.getEntity();
        LivingEntity attacker = victim.getLastAttacker();

        if (!(victim instanceof Chicken) || attacker == null) return;

        LOGGER.info(String.format("victim:%s attacker:%s fallspeed:%s falldistance:%s ischisel:%s",
                victim.getEncodeId(),
                attacker.getEncodeId(),
                attacker.getDeltaMovement().y,
                attacker.fallDistance,
                attacker.getMainHandItem().getItem() instanceof ChiselItem));

        // code to grant Triple Baka in reward for giving a lobotomy
        if (
                victim instanceof Chicken &&
                attacker instanceof Player player &&
                player.getMainHandItem().getItem() instanceof ChiselItem &&
                isCritting(player)
        ) {
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));

            Level level = player.level();
            RandomSource random = level.getRandom();
            // 50% chance to break the item

            float chanceToGetDisc = 1F;//0.15F;
            float chanceToBreak = 1F;//0.5F;

            // if the disc is granted, it wil break anyway.
            boolean shouldGrantDisc = random.nextFloat() < chanceToGetDisc;
            boolean shouldBreak = (shouldGrantDisc || random.nextFloat() < chanceToBreak) && !player.isCreative();

            if (shouldGrantDisc) {
                ItemEntity newborn = new ItemEntity(
                        level,
                        victim.getX(),
                        victim.getY(),
                        victim.getZ(),
                        new ItemStack(ModItems.MUSIC_DISC_TRIPLE_BAKA.get())
                );
                level.addFreshEntity(newborn);
            }

            if (shouldBreak) {
                player.setItemInHand(InteractionHand.MAIN_HAND, shouldGrantDisc ? new ItemStack(ModItems.MUSIC_DISC_TRIPLE_BAKA.get()) : ItemStack.EMPTY);
                player.playSound(SoundEvents.ITEM_BREAK);
                if (shouldGrantDisc) player.playSound(SoundEvents.TOTEM_USE);
            }

        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Chicken) chickenDeath(event);
        else if (entity instanceof EntityGorilla) gorillaDeath(event);
    }

    public static void chickenDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!event.getSource().is(DamageTypes.LIGHTNING_BOLT)) return;
        Level level = victim.level();
        if (level.isClientSide()) return;

        ItemEntity newborn = new ItemEntity(
                level,
                victim.getX(),
                victim.getY(),
                victim.getZ(),
                new ItemStack(ModItems.MUSIC_DISC_BIRDBRAIN.get())
        );
        newborn.invulnerableTime = 60;
        level.addFreshEntity(newborn);

        victim.playSound(SoundEvents.TOTEM_USE);
    }

    public static void gorillaDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        LivingEntity attacker = victim.getLastAttacker();
        Level level = victim.level();

        //LOGGER.info(String.format("victim:%s attacker:%s client:%s player:%s empty:%s maxhealth:%s armor:%s", victim.getEncodeId(), attacker == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType()), level.isClientSide(), attacker instanceof Player, attacker.getMainHandItem().isEmpty(), attacker.getMaxHealth(), attacker.getArmorValue()));
        if (!(attacker instanceof Player player)) return;
        if (level.isClientSide()) return; // note that the attacker is already null on clientside and is caught with the previous check, this is just for readability
        if (!player.getMainHandItem().isEmpty()) return;

        float playerMaxHealth = player.getMaxHealth();

        boolean tooMuchHealth = playerMaxHealth > 20;
        boolean tooMuchArmor = player.getArmorValue() > 10;

        boolean wasUnfair = tooMuchHealth || tooMuchArmor;


        ServerPlayer serverPlayer = (ServerPlayer)player;


        if (!ModAdvancements.getAdvancementProgress((ServerLevel)level, serverPlayer, ModAdvancements.OBTAINED_CLAIRO_DISC).isDone()) {
            if (wasUnfair) {
                serverPlayer.sendSystemMessage(Component.literal(String.format("§3Y'know, I'm sorry, but I'm not gonna give you that one. You fought and killed a wild animal with " + (tooMuchHealth ? "%.0f extra hearts" + (tooMuchArmor ? " and " : "") : "") + (tooMuchArmor ? "%s armor points" : "") + ". Like, isn't the point of fist-fighting a gorilla to fist fight the gorilla? I'm not saying that the two of you are on exactly equal grounds, but the least you can do is let it defend itself. You probably blasted it to the quantum realm with a Turbo Sword of Killings and Murder(face), then punched the last remnants and memories so you would get all the credit.\n\nNo. That's not how it's gonna roll. Do it again with...\n20 or less max HP and\n10 or less armor points.\n\nThen I'll give you whatever you want, so long as what you want is Clairo by Juna. Good luck.", playerMaxHealth - 20, player.getArmorValue())), false);
                player.playSound(SoundEvents.AMBIENT_CAVE.get());
                // "Nah man you have to do it with uhhh uhhhh no extra hearts anddddd half an armor bar or less. No point in an unfair fight, right?"
                return;
            } else {
                serverPlayer.sendSystemMessage(Component.literal("§3Well, color me surprised. You killed a gorilla. Consider me proud."));
            }
        }

        ItemEntity newborn = new ItemEntity(
                level,
                victim.getX(),
                victim.getY(),
                victim.getZ(),
                new ItemStack(ModItems.MUSIC_DISC_CLAIRO.get())
        );
        level.addFreshEntity(newborn);

        victim.playSound(SoundEvents.TOTEM_USE);
    }

    public static boolean isCritting(LivingEntity entity) {
        return entity.getDeltaMovement().y < 0;
    }
}
