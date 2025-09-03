package com.robotemployee.reu.banana;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class BananaRaidEvents {
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        BananaRaidServerManager.getManager(serverLevel);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        BananaRaidServerManager.removeManager(serverLevel);
    }

    /*
    public static final float DEVIL_DAMAGE_TRANSFER_MULTIPLIER = 0.2f;
    @SubscribeEvent
    public static void onEntityDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof BananaRaidMob victim)) return;
        if (!victim.isBeingProtected()) return;

        Level level = victim.level();
        if (level.isClientSide()) return;

        // Spreads the damage among all devils equally

        float damageRemaining = event.getAmount() * DEVIL_DAMAGE_TRANSFER_MULTIPLIER;
        int devilsAmount = victim.getDevilsProtectingMeIds().size();
        for (DevilEntity devil : victim.getDevilsProtectingMe().toList()) {
            float damageToApply = Math.min(devil.getHealth(), damageRemaining / devilsAmount);
            devil.hurt(victim.damageSources().magic(), damageToApply);
            damageRemaining -= damageToApply;
        }


        float damageToApply = damageRemaining > 0 ? damageRemaining / DEVIL_DAMAGE_TRANSFER_MULTIPLIER : 0;
        boolean shieldingFromDeath = victim.getHealth() - 1 < damageToApply;

        if (shieldingFromDeath) {
            level.playSound(victim, victim.blockPosition(), SoundEvents.BLAZE_HURT, SoundSource.HOSTILE, 1, 1);
        }

        event.setAmount(shieldingFromDeath ? victim.getHealth() - 1 : damageToApply);
    }

     */

    /*
    @SubscribeEvent
    public static void onItemInsertion(EntityItemPickupEvent event) {
        if (!event.getItem().getItem().is(ModItems.SEMISOLID.get())) return;
        if (event.getEN)
    }
     */
}
