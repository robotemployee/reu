package com.robotemployee.reu.extra;

import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;
import com.robotemployee.reu.capability.FlowerCounterCapability;
import com.robotemployee.reu.capability.FlowerCounterCapability.FlowerCounter;
import com.robotemployee.reu.core.ModAdvancements;
import com.robotemployee.reu.core.ModItems;
import com.supermartijn642.rechiseled.ChiselItem;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;

public class MusicDiscObtainment {

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) lobotomyCheck(event);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Chicken) chickenDeath(event);
        else if (entity instanceof EntityGrizzlyBear) bearDeath(event);
    }

    @SubscribeEvent
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        eatFlower(event);
    }

    // when you crit another player with a chisel, there is a chance to get Triple Baka
    public static void lobotomyCheck(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();

        if (victim.level().isClientSide()) return;

        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return;

        Entity attacker = source.getEntity();
        if (attacker == null) attacker = source.getDirectEntity();

        // code to grant Triple Baka in reward for giving a lobotomy
        if (
                victim instanceof Player &&
                        attacker instanceof ServerPlayer player &&
                        player.getMainHandItem().getItem() instanceof ChiselItem &&
                        isCritting(player)
        ) {
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));

            Level level = player.level();
            RandomSource random = level.getRandom();
            // 50% chance to break the item

            float chanceToGetDisc = 0.15F;
            float chanceToBreak = 0.5F;

            // if the disc is granted, it wil break anyway.
            boolean shouldGrantDisc = random.nextFloat() < chanceToGetDisc;
            boolean shouldBreak = (shouldGrantDisc || random.nextFloat() < chanceToBreak);

            if (shouldGrantDisc) {
                ItemEntity newborn = new ItemEntity(
                        level,
                        victim.getX(),
                        victim.getY(),
                        victim.getZ(),
                        new ItemStack(ModItems.MUSIC_DISC_TRIPLE_BAKA.get())
                );
                level.addFreshEntity(newborn);
                player.playSound(SoundEvents.TOTEM_USE);
            }

            if (shouldBreak) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.playSound(SoundEvents.ITEM_BREAK);
                return;
            }
            player.playSound(SoundEvents.TURTLE_EGG_BREAK, 1, 2);

        }
    }

    // When a chicken dies to a lightning strike, you get birdbrain
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
        newborn.setInvulnerable(true);

        level.addFreshEntity(newborn);

        victim.playSound(SoundEvents.TOTEM_USE);
    }

    // When you kill a grizzly bear with a punch in a fair way, you get clairo
    public static final int ALLOWED_BEAR_DISTANCE = 8;
    public static final int ALLOWED_BEAR_MAX_HP = 20;
    public static final int ALLOWED_BEAR_ARMOR = 20;

    public static void bearDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        LivingEntity attacker = victim.getLastAttacker();
        Level level = victim.level();

        //LOGGER.info(String.format("victim:%s attacker:%s client:%s player:%s empty:%s maxhealth:%s armor:%s", victim.getEncodeId(), attacker == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType()), level.isClientSide(), attacker instanceof Player, attacker.getMainHandItem().isEmpty(), attacker.getMaxHealth(), attacker.getArmorValue()));
        if (!(attacker instanceof Player player)) return;
        if (level.isClientSide()) return; // note that the attacker is already null on clientside and is caught with the previous check, this is just for readability
        if (!player.getMainHandItem().isEmpty()) return;

        float playerMaxHealth = player.getMaxHealth();
        float distance = victim.distanceTo(attacker);

        PathNavigation nav = ((Mob)victim).getNavigation();
        Path path = nav.createPath(attacker, ALLOWED_BEAR_DISTANCE);

        boolean tooMuchHealth = playerMaxHealth > ALLOWED_BEAR_MAX_HP;
        boolean tooMuchArmor = player.getArmorValue() > ALLOWED_BEAR_ARMOR;
        boolean tooFar = distance > ALLOWED_BEAR_DISTANCE;
        boolean unreachable = path == null || !path.canReach();

        ArrayList<String> complaints = new ArrayList<>();

        if (tooMuchHealth) complaints.add(String.format("%.0f extra hearts", playerMaxHealth));
        if (tooMuchArmor) complaints.add(player.getArmorValue() + " armor points");
        if (tooFar) complaints.add(String.format("a distance of %.0fm", distance));
        if (unreachable) complaints.add("the bear was trapped");

        boolean wasUnfair = complaints.size() > 0;


        ServerPlayer serverPlayer = (ServerPlayer)player;


        if (!ModAdvancements.getAdvancementProgress((ServerLevel)level, serverPlayer, ModAdvancements.OBTAINED_CLAIRO_DISC).isDone()) {
            if (wasUnfair) {
                StringBuilder compressedComplaints = new StringBuilder();
                Iterator<String> iterator = complaints.iterator();
                while (iterator.hasNext()) {
                    String complaint = iterator.next();

                    boolean hasNext = iterator.hasNext();
                    if (!hasNext) compressedComplaints.append("and ");

                    compressedComplaints.append(complaint);
                    if (hasNext) compressedComplaints.append(", ");
                    else compressedComplaints.append(". ");
                }
                serverPlayer.sendSystemMessage(Component.literal(String.format("§3Y'know, I'm sorry, but I'm not gonna give you that one. You just fought and killed a wild animal. " + compressedComplaints + ". Like, isn't the point of fist-fighting a grizzly bear to fist fight the grizzly bear? I'm not saying that the two of you are on exactly equal grounds, but the least you can do is let it defend itself. You probably blasted it to the quantum realm with a Turbo Sword of Killings and Murder(face), then punched the last remnants and memories so you would get all the credit.\n\nNo. That's not how it's gonna roll. Do it again with...\n20 or less max HP and\n10 or less armor points.\n\nThen I'll give you whatever you want, so long as what you want is Clairo by Juna. Good luck.", playerMaxHealth - 20, player.getArmorValue())), false);
                player.playSound(SoundEvents.AMBIENT_CAVE.get());
                // "Nah man you have to do it with uhhh uhhhh no extra hearts anddddd half an armor bar or less. No point in an unfair fight, right?"
                return;
            } else {
                serverPlayer.sendSystemMessage(Component.literal("§3Well, color me surprised. I bet it made you run into the hills screaming and yelling... Anyways uhhhh I'm sorry ig."));
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
        return entity.getDeltaMovement().y < 0 && entity.fallDistance > 0;
    }

    public static void eatFlower(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack flower = event.getItemStack();

        if (!flower.is(ItemTags.FLOWERS)) return;

        LazyOptional<FlowerCounter> optional = player.getCapability(FlowerCounterCapability.CAPABILITY, Direction.DOWN);

        if (!optional.isPresent()) return;

        FlowerCounter counter = optional.resolve().get();

        if (player.pick(player.getBlockReach(), 0, true).getType() != HitResult.Type.MISS) return;

        // then eat the flower
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);

        flower.shrink(1);
        player.getFoodData().eat(1, 0.1f);
        player.playSound(SoundEvents.GENERIC_EAT);
        player.playSound(SoundEvents.PLAYER_BURP);

    }
}
