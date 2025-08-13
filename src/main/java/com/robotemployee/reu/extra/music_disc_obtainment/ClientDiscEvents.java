package com.robotemployee.reu.extra.music_disc_obtainment;

import com.robotemployee.reu.extra.ServerboundPreciseArrowPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;

import static com.robotemployee.reu.extra.music_disc_obtainment.GenericDiscEvents.*;

@OnlyIn(Dist.CLIENT)
public class ClientDiscEvents {
    public static int airTicksTrackable = 0;

    // for phillip's arrow challenge
    public static final Deque<Float> SPIN_TRACKER = new ArrayDeque<>();

    @SubscribeEvent
    public static void onLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // i don't know what i was thinking but im too scared to remove this ?
        SPIN_TRACKER.clear();
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (fallingArrowFiredCheck(event)) return;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        // only the ticks where you were in air will count for a 360
        trackAirTime(player);
        // just updating the deque
        track360(player);
    }

    // note that it isn't just being reset when you're on the ground
    // so that there is a buffer and you don't need to be exact with the jump
    // i guess
    // since we can track slightly more ticks than we should to have a grace period

    public static void trackAirTime(Player player) {
        if (player.onGround()) airTicksTrackable = 0;
        else airTicksTrackable = Math.min(airTicksTrackable + 1, TICKS_TRACKED);
    }

    public static void track360(Player player) {
        // progress towards doing a 360 is only tracked if you aren't on the ground
        // what kinda logic is that. we need a constantly updated tracker
        //if (player.onGround()) return;


        //Deque<Float> tracker = PLAYER_SPIN_TRACKER.computeIfAbsent(player.getUUID(), uuid -> new LinkedList<>());

        if (player.onGround()) {
            SPIN_TRACKER.clear();
            return;
        }
        SPIN_TRACKER.addFirst(player.getYHeadRot());
        while (SPIN_TRACKER.size() > TICKS_TRACKED) SPIN_TRACKER.removeLast();
        //LOGGER.info("Tracking spin: " + SPIN_TRACKER);
    }

    public static boolean isEpicAndCoolSpin() {
        float basis = SPIN_TRACKER.getLast();

        float totalSpin = 0;

        int i = -5;
        for (Float rawSpin : SPIN_TRACKER) {
            if (++i > airTicksTrackable) break;
            float spin = rawSpin - basis;
            totalSpin += spin;
            //LOGGER.info("Total spin: " + totalSpin);
            if (Math.abs(totalSpin) > ROTATION_FOR_EPICNESS) return true;
        }
        //LOGGER.info("Spin wasn't cool enough. " + totalSpin);
        return false;
    }

    public static boolean fallingArrowFiredCheck(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = event.getLevel();
        // opposite of usual affairs. don't run this on the server!
        if (!level.isClientSide()) return false;
        //LOGGER.info("Clientside!!");
        if (!(entity instanceof Arrow arrow)) return false;
        if (!(arrow.getOwner() instanceof Player player)) return false;

        //LOGGER.info("An arrow just got fired by a player");

        if (player.getDeltaMovement().y > -0.1) return false;
        if (!isEpicAndCoolSpin()) return false;

        if (!player.getMainHandItem().is(Items.BOW)) {
            player.sendSystemMessage(Component.literal("§3You must be using a Bow."));
            return true;
        }

        if (player.fallDistance < NEEDED_FALLEN_BLOCKS) {
            if (player.fallDistance > 0.6) {
                player.sendSystemMessage(Component.literal("§3You must be falling for longer before firing."));
            }
            return true;
        }

        if (!player.isCrouching()) {
            player.sendSystemMessage(Component.literal("§3Crouch when you fire the arrow, but be upright when it hits."));
            return true;
        }

        if (player.hasEffect(MobEffects.SLOW_FALLING)) {
            player.sendSystemMessage(Component.literal("§3You must be falling at a normal rate."));
            return true;
        }

        //player.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.5f, 1.3f);
        player.playSound(SoundEvents.IRON_GOLEM_HURT, 0.65f, 2);
        //player.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.5f, 1.5f);
        //player.displayClientMessage(Component.literal(String.format("§3%s", message)), false);

        //LOGGER.info("A really cool arrow just got fired");
        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        ServerboundPreciseArrowPacket.INSTANCE.sendToServer(new ServerboundPreciseArrowPacket(arrow.getUUID(), player.getUUID(), arrow.tickCount, player.position(), date));
        return true;
    }
}
