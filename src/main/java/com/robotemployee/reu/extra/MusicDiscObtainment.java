package com.robotemployee.reu.extra;

import com.github.alexmodguy.alexscaves.server.entity.living.NucleeperEntity;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;
import com.github.alexthe666.alexsmobs.entity.ai.GroundPathNavigatorWide;
import com.mojang.logging.LogUtils;
import com.robotemployee.reu.capability.FlowerCounterCapability;
import com.robotemployee.reu.capability.FlowerCounterCapability.FlowerCounter;
import com.robotemployee.reu.core.registry.*;
import com.robotemployee.reu.mobeffect.TummyAcheMobEffect;
import com.supermartijn642.rechiseled.ChiselItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Supplier;

public class MusicDiscObtainment {

    static Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (lobotomyCheck(event)) return;
        if (bearAttacked(event)) return;
        if (ironGolemDamageCheck(event)) return;
        if (asbestosSuffocateOnAttack(event)) return;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (chickenDeathCheck(event)) return;
        if (bearDeathCheck(event)) return;
        if (bearHealOnPlayerDeathCheck(event)) return;
        if (asbestosDeathCheck(event)) return;
        if (preciseArrowDeathCheck(event)) return;
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        preciseArrowDamageBoost(event);
    }

    @SubscribeEvent
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        eatFlowerCheck(event);
    }

    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        feedNucleeperCheck(event);
    }

    @SubscribeEvent
    public static void onPlayerFinishedUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (asbestosAccelerateCheck(event)) return;
        if (asbestosDecelerateCheck(event)) return;
        if (asbestosPillCheck(event)) return;
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        fallingArrowFiredCheck(event);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (icarusCheck(event)) return;
        if (asbestosRenewCheck(event)) return;
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player player)) return;

        if (player.getCapability(FlowerCounterCapability.CAPABILITY).isPresent()) return;

        event.addCapability(FlowerCounterCapability.ID, new FlowerCounterCapability());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player grandpa = event.getOriginal();
        Player newborn = event.getEntity();

        // i hate this
        grandpa.getCapability(FlowerCounterCapability.CAPABILITY).ifPresent((grandpaPeace) ->
                newborn.getCapability(FlowerCounterCapability.CAPABILITY).ifPresent((newbornPeace) -> {
                    newbornPeace.deserializeNBT(grandpaPeace.serializeNBT());
                })
        );
    }

    // when you crit another player with a chisel, there is a chance to get Triple Baka
    public static boolean lobotomyCheck(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();

        if (level.isClientSide()) return false;

        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return false;

        Entity attacker = source.getEntity();
        if (attacker == null) attacker = source.getDirectEntity();



        // code to grant Triple Baka in reward for giving a lobotomy
        if (!(victim instanceof Chicken)) return false;
        if (!(attacker instanceof ServerPlayer player)) return false;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof ChiselItem)) return false;
        if (!isCritting(player)) return false;

        victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 160, 0));
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));

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
            level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1, 1);
        }

        if (shouldBreak) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1, 1);

            ItemParticleOption particleOption = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(stack.getItem()));

            ((ServerLevel)level).sendParticles(
                    particleOption,
                    player.getX(),
                    player.getY() + 0.5 * player.getEyeHeight(),
                    player.getZ(),
                    16,
                    0.3, 0.3, 0.3,
                    0
            );

            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            return true;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TURTLE_EGG_BREAK, SoundSource.PLAYERS, 1, 1);
        return true;
    }

    public static final float LEAP_FORCE_SCALE = 0.5f;
    public static final float LEAP_FORCE_ADD_Y = 1;
    public static final double LEAP_FORCE_MAX = 8;
    public static final float CHANCE_TO_KNOCKBACK_RESIST = 0.25f;

    public static boolean bearAttacked(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();

        Level level = victim.level();

        if (level.isClientSide()) return false;

        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return false;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof Player)) attacker = source.getDirectEntity();
        if (!(attacker instanceof Player player)) return false;

        boolean followingTheRules = player.getMainHandItem().isEmpty();

        float health = (victim.getHealth() / victim.getMaxHealth());

        if (!(victim instanceof EntityGrizzlyBear) || health > 0.5) return false;

        // if the player is killing a grizzly bear without having obtained Clairo
        /*
        if (!player.getMainHandItem().isEmpty() && !ModAdvancements.isAdvancementComplete((ServerLevel) level, (ServerPlayer) player, ModAdvancements.OBTAINED_CLAIRO_DISC)) {
            if (!victim.hasEffect(MobEffects.REGENERATION)) {
                player.sendSystemMessage(Component.literal("§3The bear resists, because you are not worthy of Clairo by Juna."));
            }
            victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, health < 0.3 ? 3 : 2));
        }*/

        if (!victim.hasEffect(MobEffects.DAMAGE_BOOST))
            player.sendSystemMessage(Component.literal("§3The bear is enraged."));

        final int KNOCKBACK_RESIST_DURATION = 20;
        MobEffectInstance knockbackResist = victim.getEffect(AMEffectRegistry.KNOCKBACK_RESISTANCE.get());
        if (knockbackResist != null && knockbackResist.getDuration() > KNOCKBACK_RESIST_DURATION) {
            victim.removeEffect(AMEffectRegistry.KNOCKBACK_RESISTANCE.get());
        }

        RandomSource random = level.getRandom();
        float chanceToLeap = 0.1f * (1 + 4 * (1 - (health * 2))); //+ (victim.hasEffect(AMEffectRegistry.KNOCKBACK_RESISTANCE.get()) ? 0.3f: 0)
        if (!followingTheRules || random.nextFloat() < chanceToLeap) {
            Vec3 diff = victim.position().vectorTo(player.position());

            double x = diff.x;
            double y = Math.min(2, diff.y * 0.3) + LEAP_FORCE_ADD_Y;
            double z = diff.z;

            Vec3 initialForce = new Vec3(x, y, z).scale(LEAP_FORCE_SCALE);

            Vec3 force = initialForce.scale(Math.min(1, LEAP_FORCE_MAX / initialForce.length()));
            victim.addDeltaMovement(force);
            victim.addEffect(new MobEffectInstance(AMEffectRegistry.KNOCKBACK_RESISTANCE.get(), 20, 0));
            victim.heal(2);
        }

        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
        victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
        victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));

        if (health < 0.25) victim.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 1));

        if (health < 0.15) {
            victim.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2));
        }
        //victim.heal(Math.min(1, event.getAmount() - 4));
        // regen lowers your damage by too much
        //victim.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        // knockback resistance is just too much with the new leap mechanic
        // maybe not with a short duration and being removed on leap?? i tried with no resist and it was boring

        /*
        if (victim.hasEffect(AMEffectRegistry.KNOCKBACK_RESISTANCE.get()) && random.nextBoolean()) victim.removeEffect(AMEffectRegistry.KNOCKBACK_RESISTANCE.get());
        else victim.addEffect(new MobEffectInstance(AMEffectRegistry.KNOCKBACK_RESISTANCE.get(), 20, 0));
         */
        //if (random.nextFloat() < CHANCE_TO_KNOCKBACK_RESIST) victim.addEffect(new MobEffectInstance(AMEffectRegistry.KNOCKBACK_RESISTANCE.get(), 20, 0));

        victim.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        victim.removeEffect(MobEffects.WEAKNESS);
        victim.removeEffect(MobEffects.BLINDNESS);
        victim.removeEffect(MobEffects.POISON);
        victim.removeEffect(MobEffects.WITHER);
        return true;

    }

    public static final int GOLEM_EVISCERATION = 50;

    public static boolean ironGolemDamageCheck(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();

        Level level = victim.level();

        if (level.isClientSide()) return false;

        if (!(victim instanceof IronGolem)) return false;

        //LOGGER.info("you just hit that iron golem for " + event.getAmount() + " damage");

        if (event.getAmount() < GOLEM_EVISCERATION) return false;

        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1, 1);

        ItemEntity newborn = new ItemEntity(
                level,
                victim.getX(),
                victim.getY(),
                victim.getZ(),
                new ItemStack(ModItems.MUSIC_DISC_MECHANIZED_MEMORIES.get())
        );
        newborn.setInvulnerable(true);

        level.addFreshEntity(newborn);
        return true;
    }

    public static boolean asbestosSuffocateOnAttack(LivingAttackEvent event) {
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

    // When a chicken dies to a lightning strike, you get birdbrain
    // 8% chance
    float CHICKEN_CHANCE_TO_DROP = 0.08f;
    public static boolean chickenDeathCheck(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!event.getSource().is(DamageTypes.LIGHTNING_BOLT)) return false;
        Level level = victim.level();
        if (level.isClientSide()) return false;
        RandomSource random = level.getRandom();
        if (random.nextFloat() < 0.08f) return false;

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
        return true;
    }

    // When you kill a grizzly bear with a punch in a fair way, you get clairo
    public static final int ALLOWED_BEAR_DISTANCE = 6;
    public static final int ALLOWED_BEAR_MAX_HP = 20;
    public static final int ALLOWED_BEAR_ARMOR = 20;

    public static boolean bearDeathCheck(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        LivingEntity attacker = victim.getLastAttacker();
        Level level = victim.level();

        //LOGGER.info(String.format("victim:%s attacker:%s client:%s player:%s empty:%s maxhealth:%s armor:%s", victim.getEncodeId(), attacker == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType()), level.isClientSide(), attacker instanceof Player, attacker.getMainHandItem().isEmpty(), attacker.getMaxHealth(), attacker.getArmorValue()));
        if (!(attacker instanceof Player player)) return false;
        if (level.isClientSide()) return false; // note that the attacker is already null on clientside and is caught with the previous check, this is just for readability
        if (!player.getMainHandItem().isEmpty()) return false;

        float playerMaxHealth = player.getMaxHealth();
        float distance = victim.distanceTo(attacker);

        // i,,,,
        // is there worse code?
        // have i ever seen worse code?
        // maybe.
        // but this is still erm. erm. erm.
        // if i didn't do this the path would be null randomly. so

        Mob copiumFakeShitMob = AMEntityRegistry.GRIZZLY_BEAR.get().create(level);
        assert copiumFakeShitMob != null;
        copiumFakeShitMob.setPos(victim.getPosition(0f));
        PathNavigation nav = new GroundPathNavigatorWide(copiumFakeShitMob, level) {
            @Override
            protected boolean canUpdatePath() {
                return true;
            }

        };

        Path path = nav.createPath(attacker.blockPosition(), ALLOWED_BEAR_DISTANCE + 5);

        BlockHitResult footResult = level.clip(new ClipContext(
                victim.position(),
                player.position(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                victim
        ));

        HitResult eyeResult = level.clip(new ClipContext(
                victim.getEyePosition(),
                player.getEyePosition(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                victim
        ));

        //LOGGER.info(String.format("Going from %s to %s", victim, attacker));

        BlockState footBlock = level.getBlockState(footResult.getBlockPos());

        boolean badFootBlock = footBlock.is(BlockTags.FENCES) || footBlock.is(BlockTags.FENCE_GATES);

        // simple algorithm to find the block position corresponding to the air above the ground below us
        // this is important because if we directly check the player's y-position
        BlockPos airAboveGroundPos = player.blockPosition();
        int depth = 4;
        for (int i = 0; i < depth; i++) {
            BlockPos pos = airAboveGroundPos.below();
            BlockState examined = level.getBlockState(pos);
            boolean hasCollision = !examined.getCollisionShape(level, pos).isEmpty();
            boolean air = examined.isAir();

            if (air || !hasCollision) airAboveGroundPos = airAboveGroundPos.below();
            else break;
        }

        //level.g

        // preliminary
        boolean playerFlying = player.getAbilities().flying;
        boolean pathNull = path == null;
        boolean pathHasNodes = !pathNull && path.getNodeCount() > 0;
        // these are all summarized into unreachable
        boolean pathCantReach = !pathNull && !path.canReach();
        boolean pathTargetTooFar = !pathNull && !path.getTarget().closerToCenterThan(player.position(), 0.5);
        boolean pathEndNodeVerticalTooFar = pathHasNodes && airAboveGroundPos.getY() - path.getEndNode().asBlockPos().getY() > 1;
        // no longer dependent on path existing
        boolean footObscured = footResult.getType() == HitResult.Type.BLOCK && badFootBlock;
        boolean eyeObscured = eyeResult.getType() == HitResult.Type.BLOCK;

        boolean unreachable = playerFlying || pathCantReach || pathTargetTooFar || pathEndNodeVerticalTooFar || footObscured || eyeObscured;

        //|| path.getTarget().equals(player.blockPosition())

        if (path == null) {
            LOGGER.info("Path is null.");
        } else {
            LOGGER.info("Booleans: " + pathNull + pathHasNodes + pathCantReach + pathTargetTooFar + pathEndNodeVerticalTooFar + footObscured + eyeObscured);
            LOGGER.info("player position: " + attacker.blockPosition());
            LOGGER.info("stepheight: " + copiumFakeShitMob.getStepHeight());
            LOGGER.info("nodecount:" + path.getNodeCount());
            for (int i=0; i < path.getNodeCount(); i++) {
                LOGGER.info("Node: " + path.getNodePos(i));
            }
            LOGGER.info("End node: " + path.getEndNode().asBlockPos());
        }

        boolean isInCobweb = false;

        AABB checkedBlocks = victim.getBoundingBox().inflate(0.01);
        for (int x = (int)Math.floor(checkedBlocks.minX); x < Math.ceil(checkedBlocks.maxX); x++)
        for (int y = (int)Math.floor(checkedBlocks.minY); y < Math.ceil(checkedBlocks.maxY); y++)
        for (int z = (int)Math.floor(checkedBlocks.minZ); z < Math.ceil(checkedBlocks.maxZ); z++)
            if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.COBWEB)) {
                isInCobweb = true;
                break;
            }


        boolean tooMuchHealth = playerMaxHealth > ALLOWED_BEAR_MAX_HP;
        boolean tooMuchArmor = player.getArmorValue() > ALLOWED_BEAR_ARMOR;
        boolean tooFar = distance > ALLOWED_BEAR_DISTANCE;
        boolean friendly = ((TamableAnimal)victim).getOwnerUUID() == player.getUUID();
        boolean tooMuchReach = player.getEntityReach() > 6.5;
        boolean hampered = victim.isFreezing() ||
                victim.isInWaterOrBubble() ||
                victim.isInLava() ||
                isInCobweb ||
                victim.isSuppressingBounce() ||
                victim.isPassenger();

        ArrayList<String> complaints = new ArrayList<>();

        if (tooMuchHealth) complaints.add(String.format("you had %.0f extra hearts", playerMaxHealth));
        if (tooMuchArmor) complaints.add(String.format("you had %s armor points", player.getArmorValue()));
        if (tooFar) complaints.add(String.format("the distance was %.0fm", distance));
        if (friendly) complaints.add("the bear was tamed");
        if (tooMuchReach) complaints.add("you could reach really far");
        if (hampered) complaints.add("the bear was impeded");
        if (unreachable) complaints.add("the bear could not reach you");

        boolean wasUnfair = complaints.size() > 0;

        boolean firstCompletion = !ModAdvancements.isAdvancementComplete((ServerLevel)level, (ServerPlayer)player, ModAdvancements.OBTAINED_CLAIRO_DISC);

        if (wasUnfair) {
            StringBuilder compressedComplaints = new StringBuilder();
            Iterator<String> iterator = complaints.iterator();
            boolean first = true;
            while (iterator.hasNext()) {
                String complaint = iterator.next();

                boolean hasNext = iterator.hasNext();
                if (!hasNext && !first) compressedComplaints.append("and ");

                if (first) complaint = complaint.substring(0, 1).toUpperCase(Locale.ROOT) + complaint.substring(1);

                compressedComplaints.append(complaint);
                if (hasNext) {
                    if (complaints.size() > 1) compressedComplaints.append(", ");
                    else compressedComplaints.append(" ");
                }
                first = false;
            }
            if (firstCompletion && complaints.size() > 2) {
                player.sendSystemMessage(Component.literal(String.format("§3Y'know, I'm sorry, but I'm not gonna give you that one. You just fought and killed a wild animal. §b" + compressedComplaints + ".§3 Like, isn't the point of fist-fighting a grizzly bear to fist fight the grizzly bear? I'm not saying that the two of you are on exactly equal grounds, but the least you can do is let it defend itself. You probably blasted it to the quantum realm with a Turbo Sword of Killings and Murder(face), then punched the last remnants and memories so you would get all the credit.\n\nNo. That's not how it's gonna roll. Respect the damn bear!\n\nThen I'll give you whatever you want, so long as what you want is Clairo by Juna. Good luck.", playerMaxHealth - 20, player.getArmorValue())));
                // "Nah man you have to do it with uhhh uhhhh no extra hearts anddddd half an armor bar or less. No point in an unfair fight, right?"
            } else {
                player.sendSystemMessage(Component.literal("§3Not fair enough. §b" + compressedComplaints + "."));
            }
        } else {
            if (firstCompletion) {
                player.sendSystemMessage(Component.literal("§3Well, color me surprised. I bet it made you run into the hills screaming and yelling... Anyways uhhhh I'm sorry ig. My bad. Oops. :3c"));
            } else {
                player.sendSystemMessage(Component.literal("§3NOOOOOOO FREDDY FAZBEAR WHAT HAVE THEY DONE TO YOU"));
            }
        }

        // we've gotten our complaints out of the way
        // but do not reward the disc if it was unfair
        if (wasUnfair) return true;

        // secret way to cure tummy ache
        if (player.isCrouching()) TummyAcheMobEffect.cure(player);

        ItemEntity newborn = new ItemEntity(
                level,
                victim.getX(),
                victim.getY(),
                victim.getZ(),
                new ItemStack(ModItems.MUSIC_DISC_CLAIRO.get())
        );
        level.addFreshEntity(newborn);

        victim.playSound(SoundEvents.TOTEM_USE);
        return true;
    }

    public static boolean bearHealOnPlayerDeathCheck(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        LivingEntity attacker = victim.getLastAttacker();
        Level level = victim.level();
        if (level.isClientSide()) return false;
        if (!(attacker instanceof EntityGrizzlyBear)) return false;
        boolean isPlayer = victim instanceof Player;
        attacker.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, isPlayer ? 4 : 2));
        if (!isPlayer) return false;
        victim.sendSystemMessage(Component.literal("§3The bear recovers."));
        return true;
    }

    public static boolean asbestosDeathCheck(LivingDeathEvent event) {
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

    public static boolean isCritting(LivingEntity entity) {
        return entity.getDeltaMovement().y < 0 && entity.fallDistance > 0;
    }

    public static final int FLOWERS_NEEDED = 15;

    public static final String[] IN_BETWEEN_MESSAGES = {
            "Your progress, I'm sure, is coming as it should. %s",
            "Can you feel it? I hope it's not too warm. %s",
            "Share what you love. %s",
            "Eventually, you will finish your quest. Eventually. %s",
            "Is it bitter? %s",
            "Wash on, wash off. Wash on, wash off. %s",
            "The unexpected is ine%svitable. Live with it.",
            "don't be afraid to change form! %s",
            "Are you near the end? Or have you just begun? %s",
            "Are you enjoying this? Do you care? %s",
            "These messages are hand-picked by fate. %s",
            "Don't get too ahead of yourself. %s",
            "I will write. %s",
            "Only you can see this. %s",
            "Do you want someone else to experience this? %s",
            "Is this something you want to forget? %s",
            "Are you where you want to be? %s",
            "Is there anywhere you should be? %s",
            "You should be getting experience points for this. %s",
            "There are more. %s",
            "This is the last message. %s"
    };

    public static boolean eatFlowerCheck(PlayerInteractEvent.RightClickItem event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return false;

        Player player = event.getEntity();
        ItemStack flower = event.getItemStack();

        if (!flower.is(ItemTags.FLOWERS)) return false;

        if (!player.isCrouching()) {
            player.sendSystemMessage(Component.literal("§3To consume this flower, you must crouch first."));
            return true;
        }

        Optional<FlowerCounter> optional = player.getCapability(FlowerCounterCapability.CAPABILITY, null).resolve();

        if (optional.isEmpty()) return true;

        FlowerCounter innerPeace = optional.get();

        int needed = innerPeace.getRemaining();


        if (innerPeace.hasEaten(flower)) {
            player.sendSystemMessage(Component.literal("§3You've already eaten that flower... Here's what you've had:\n" + innerPeace.getMultiline()));
            return true;
        }

        if (player.pick(player.getBlockReach(), 0, true).getType() != HitResult.Type.MISS) return true;

        if (!innerPeace.addFlower(flower)) {
            player.sendSystemMessage(Component.literal("§3Flower system failure, this message should never show up. Tell me if you're reading this."));
            return true;
        }

        if (innerPeace.getCount() == 1) {
            player.sendSystemMessage(Component.literal("§3Welcome to your flower quest. Whether you're returning or starting anew, I trust that inner peace will be with you when you are ready.\n\n Consume " + (FLOWERS_NEEDED - 1) + " more unique flowers, and it will be yours."));
        }

        // the flower has been added by counter.addFlower()

        // if the journey is on its way
        else if (!innerPeace.isBlossoming()) {
            int index = level.getRandom().nextInt(IN_BETWEEN_MESSAGES.length);
            player.sendSystemMessage(Component.literal(String.format("§3" + IN_BETWEEN_MESSAGES[index], String.format("§7(%s/%s)§3", innerPeace.getCount(), FLOWERS_NEEDED))));
        }
        // if the journey is complete
        else {
            player.sendSystemMessage(Component.literal("§3Your meal is complete, and the sickly parts of your life are away.\n\nYou are at peace.\n\nShould you ever desire to embark with me again, I will be waiting."));
            innerPeace.reset();
            player.addItem(new ItemStack(ModItems.MUSIC_DISC_ORANGE_BLOSSOMS.get()));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1, 1);
            //player.playSound(SoundEvents.BOOK_PAGE_TURN, 1, 2);
        }

        //LOGGER.info("the code is getting to this point");

        ServerLevel serverLevel = (ServerLevel)level;
        ItemParticleOption particleOption = new ItemParticleOption(ParticleTypes.ITEM, flower);

        serverLevel.sendParticles(
                particleOption,
                player.getX(),
                player.getY() + 0.5 * player.getEyeHeight(),
                player.getZ(),
                16,
                0.3, 0.3, 0.3,
                0
        );

        flower.shrink(1);
        player.getFoodData().eat(1, 0.1f);

        //LOGGER.info(String.valueOf(player.isSilent()));

        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 10));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1, 1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1, 1);

        // just some eating effects
        //event.setCanceled(true);
        //event.setCancellationResult(InteractionResult.CONSUME);
        return true;
    }

    // chance is vastly increased when food count is low
    // but not too low.
    public static final float CHANCE_TO_NUCLEEPER_BARTER = 0.03f;
    public static final int COUNT_INCREASE_CHANCE_START = 20;
    public static final int COUNT_INCREASE_CHANCE_END = 16;
    public static final Supplier<Item> NUCLEEPER_FOOD = () -> Items.GLOWSTONE_DUST;

    public static void feedNucleeperCheck(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        Level level = player.level();

        if (level.isClientSide()) return;

        if (!(target instanceof NucleeperEntity)) return;

        ItemStack food = player.getItemInHand(event.getHand());

        if (!food.is(NUCLEEPER_FOOD.get())) return;

        if (!player.isCrouching()) {
            player.sendSystemMessage(Component.literal("§bCrouch §3in order to give it your respect and admiration"));
        }

        if (player.getCooldowns().isOnCooldown(NUCLEEPER_FOOD.get())) return;

        // The player is feeding a Nucleeper.

        float distance = player.distanceTo(target);

        float chanceMultiplier = Mth.clamp(1 - (distance - 5) / 6, 0.1f, 1);

        int count = food.getCount();

        float chance = CHANCE_TO_NUCLEEPER_BARTER * chanceMultiplier + ((distance < 3 && count < COUNT_INCREASE_CHANCE_START && count > COUNT_INCREASE_CHANCE_END) ? 0.5f : 0);

        target.playSound(SoundEvents.HORSE_EAT);
        food.shrink(1);

        ItemParticleOption particleOption = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(NUCLEEPER_FOOD.get()));

        ((ServerLevel)level).sendParticles(
                particleOption,
                target.getX(),
                target.getY() + 0.5 * target.getEyeHeight(),
                target.getZ(),
                16,
                0.3, 0.3, 0.3,
                0
        );

        if (distance > 7) {
            player.sendSystemMessage(Component.literal("§3Vastly reduced chances - come closer so it can feel your love"));
        }

        float fate = level.getRandom().nextFloat();

        if (fate > chance) {
            if (fate > 0.9f) {
                player.getCooldowns().addCooldown(food.getItem(), 15);
                player.sendSystemMessage(Component.literal("§3Be more gentle!"));
            }
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, false, false, false));

        // The player has won the lottery.

        target.playSound(SoundEvents.TOTEM_USE);
        target.playSound(SoundEvents.WOLF_AMBIENT);

        player.getCooldowns().addCooldown(food.getItem(), 40);

        ItemEntity newborn = new ItemEntity(
                level,
                target.getX(),
                target.getY(),
                target.getZ(),
                new ItemStack(ModItems.MUSIC_DISC_SO_BE_IT.get())
        );

        Vec3 direction = target.getEyePosition().vectorTo(player.getPosition(0)).normalize();
        Vec3 force = new Vec3(direction.x, 1 + direction.y * 0.2, direction.z).scale(2);
        newborn.addDeltaMovement(force);

        newborn.setInvulnerable(true);
        level.addFreshEntity(newborn);
    }

    public static boolean asbestosAccelerateCheck(LivingEntityUseItemEvent.Finish event) {
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

    public static boolean asbestosDecelerateCheck(LivingEntityUseItemEvent.Finish event) {
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

    public static boolean asbestosPillCheck(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide()) return false;

        ItemStack used = event.getItem();
        if (!used.is(ModItems.MIRACLE_PILL.get())) return false;

        if (!entity.hasEffect(ModMobEffects.TUMMY_ACHE.get())) return false;

        TummyAcheMobEffect.cure(entity);
        return true;
    }

    // MODIFIED SERVERSIDE ONLY
    // UUID is of course the arrow. The Long is the time that it was fired
    public static final HashMap<UUID, ArrowShotStatistics> PRECISE_ARROWS = new HashMap<>();
    // ONLY MODIFIED ON CLIENT
    public static final Deque<Float> SPIN_TRACKER = new ArrayDeque<>();
    // the rest of these are fine tho
    public static final int TICKS_TRACKED = 20;
    public static final int ROTATION_FOR_EPICNESS = 270;
    public static final int MIN_ARROW_H_DISTANCE = 5;
    public static final double NEEDED_FALLEN_BLOCKS = 0.2; //1.2
    public static final int POINTS_PER_RANK = 8;
    public static final int RANK_FOR_S = 10;

    public static class ArrowShotStatistics {
        public final long tick;
        public final Vec3 position;
        public final Date date;
        public final int messageIndex;

        private static final String ROOT_PATH = "ShotStatistics";
        private static final String TICK_PATH = "Tick";
        private static final String DATE_PATH = "Date";
        private static final String MESSAGE_INDEX_PATH = "Message";


        public ArrowShotStatistics(long time, Vec3 position, Date date, int messageIndex) {
            this.tick = time;
            this.position = position;
            this.date = date;
            this.messageIndex = messageIndex;
        }

        public ArrowShotStatistics(ItemStack stack) {
            CompoundTag bigTag = stack.getTag();
            if (bigTag == null) {
                tick = 0;
                position = Vec3.ZERO;
                date = Date.from(Instant.EPOCH);
                messageIndex = 0;
                return;
            }
            CompoundTag tag = bigTag.getCompound(ROOT_PATH);
            this.position = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));

            this.tick = tag.getLong(TICK_PATH);
            this.date = Date.from(Instant.ofEpochMilli(tag.getLong(DATE_PATH)));
            this.messageIndex = tag.getInt(MESSAGE_INDEX_PATH);
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();

            tag.putDouble("X", position.x);
            tag.putDouble("Y", position.y);
            tag.putDouble("Z", position.z);
            tag.putLong(TICK_PATH, tick);
            tag.putLong(DATE_PATH, date.getTime());
            tag.putInt(MESSAGE_INDEX_PATH, messageIndex);

            return tag;
        }

        // Extra information that is only available after the arrow has hit

        public static final String PERFORMER_PATH = "Performer";
        public static final String VICTIM_PATH = "Victim";
        public static final String POINTS_PATH = "Points";
        public static final String H_DIST_PATH = "Distance";
        public static final String AIRTIME_PATH = "Airtime";
        public static final String HIT_VELOCITY_PATH = "Velocity";

        public CompoundTag saveHit(String coolGuy, String victim, int points, float distance, long airtime, Vec3 hitPosition, float hitVelocity) {
            CompoundTag tag = save();
            tag.putString(PERFORMER_PATH, coolGuy);
            tag.putString(VICTIM_PATH, victim);
            tag.putInt(POINTS_PATH, points);
            tag.putFloat(H_DIST_PATH, distance);
            tag.putLong(AIRTIME_PATH, airtime);
            tag.putFloat("HitX", (float)hitPosition.x);
            tag.putFloat("HitY", (float)hitPosition.y);
            tag.putFloat("HitZ", (float)hitPosition.z);
            tag.putFloat(HIT_VELOCITY_PATH, hitVelocity);
            return tag;
        }

        public static List<Component> getTooltip(ItemStack stack, boolean displayShotData) {
            ArrowShotStatistics stats = new ArrowShotStatistics(stack);
            SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy',' EEEE 'at' hh':'mma");
            String coolDate = format.format(stats.date);

            CompoundTag big = stack.getTag();
            if (big == null) return null;
            CompoundTag tag = big.getCompound(ROOT_PATH);
            if (tag.isEmpty()) return null;

            String coolGuy = tag.getString(PERFORMER_PATH);
            String victim = tag.getString(VICTIM_PATH);
            float hitX = tag.getFloat("HitX");
            float hitY = tag.getFloat("HitY");
            float hitZ = tag.getFloat("HitZ");

            int points = tag.getInt(POINTS_PATH);
            String rankSymbol = rankSymbol(points / POINTS_PER_RANK);
            long airtime = tag.getLong(AIRTIME_PATH) - stats.tick;
            float horizontalDistance = tag.getFloat(H_DIST_PATH);

            float distance = (float)stats.position.distanceTo(new Vec3(hitX, hitY, hitZ));
            float velocity = tag.getFloat(HIT_VELOCITY_PATH);

            String message = ARROW_FIRED_MESSAGES[stats.messageIndex];

            ArrayList<Component> result = new ArrayList<>();

            result.add(Component.empty());
            //result.add(Component.literal("§3Playing this disc will wipe the shot data."));
            if (!displayShotData) {
                result.add(Component.literal("§3Press Shift to see the shot data."));
                return result;
            } else {
                result.add(Component.literal("§3Viewing shot data . . ."));
            }

            result.add(Component.literal(String.format("§7// %s", message)));
            result.add(Component.literal(String.format("§7The date was %s.", coolDate)));
            result.add(Component.literal(String.format("§7%s 360'd a %s.", coolGuy, victim)));
            result.add(Component.literal(String.format("§7Fired from (%.0f, %.0f, %.0f)", stats.position.x, stats.position.y, stats.position.z)));
            result.add(Component.literal(String.format("§7Hit (%.0f, %.0f, %.0f)", hitX, hitY, hitZ)));
            result.add(Component.literal(String.format("§7Velocity: %.1fm/s", velocity)));
            result.add(Component.literal(String.format("§7Airtime: %st", airtime)));
            result.add(Component.literal(String.format("§7Dist: %.1fm", distance)));
            result.add(Component.literal(String.format("§7H-dist: %.1fm", horizontalDistance)));
            result.add(Component.literal(String.format("§7%s points - %s", points, rankSymbol)));

            return result;
        }
    }

    public static int airTicksTrackable = 0;
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

    @SubscribeEvent
    public static void onLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // i don't know what i was thinking but im too scared to remove this ?
        SPIN_TRACKER.clear();
    }

    @SubscribeEvent
    public static void onEntityRemoved(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide()) return;
        if (!(entity instanceof Arrow)) return;
        PRECISE_ARROWS.remove(entity.getUUID());
    }

    @SubscribeEvent
    public static void onProjectileHit(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        Level level = projectile.level();
        if (level.isClientSide()) return;
        if (!(projectile instanceof Arrow)) return;
        HitResult hitResult = event.getRayTraceResult();
        if (hitResult.getType() != HitResult.Type.BLOCK) return;
        PRECISE_ARROWS.remove(projectile.getUUID());
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

    public static boolean preciseArrowDamageBoost(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) return false;

        Entity culprit = event.getSource().getDirectEntity();
        //LOGGER.info("remote viewing is real? what the fuck? anyways, here's the arrow " + culprit);
        if (!(culprit instanceof Arrow arrow)) return false;
        //LOGGER.info("wow. that sure looks like an arrow");

        if (!(arrow.getOwner() instanceof Player player)) return false;

        boolean selfAttack = victim.getUUID() == player.getUUID();

        //LOGGER.info("shot by a player");

        if (player.isCrouching()) return false;

        ArrowShotStatistics stats = PRECISE_ARROWS.get(arrow.getUUID());

        if (stats == null) return false;
        Vec3 diff = player.position().vectorTo(victim.position());
        double horizontalDistance = new Vector2d(player.getX() - victim.getX(), player.getZ() - victim.getZ()).length();

        if (!selfAttack && horizontalDistance < MIN_ARROW_H_DISTANCE) {
            victim.playSound(SoundEvents.ITEM_BREAK, 2, 1);
            return false;
        }

        //LOGGER.info("they're not crouching");



        //LOGGER.info("the arrow was epic");

        // this is a precise arrow

        boolean manslaughter = victim instanceof Player;

        int minDamage = (manslaughter) ? 20 : 10;

        float damageAdded = (manslaughter) ? 1 : 0;

        float airCoef = Math.min(1, (level.getGameTime() - stats.tick) / 100f);

        float damageMultiplier = 2 + 3 * airCoef;

        float damage = Math.max((Math.max(4, event.getAmount()) + damageAdded) * damageMultiplier, minDamage);

        event.setAmount(damage);
        //LOGGER.info("the damage: " + damage);
        if (victim.getHealth() - damage <= 0) return true;

        // if it's not dead
        //LOGGER.info("it lives");
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.7f, 1);
        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, SoundSource.PLAYERS, 1, 1.4f);
        //level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1, 1);
        return true;
    }

    public static final String[] ARROW_FIRED_MESSAGES = {
            "Safe travels.",
            "Bon voyage.",
            "Sent.",
            "And away it goes.",
            "See you.",
            "Off to the hills.",
            "Far and away.",
            "Until next.",
            "Bye.",
            "Ciao.",
            "Hey there.",
            "Farewell.",
            "早上好中国",
            "现在我有",
            "冰淇淋",
            "You murderer.",
            "How astute.",
            "Away with you.",
            "太マラ",
            "Good morning China.",
            "Smooth.",
            "Hour after hour.",
            "Don't look.",
            "No hands.",
            "For whom it may concern.",
            "Fuck you in particular.",
            "Read.",
            "Watch this.",
            "Respect your elders.",
            "Play rain world.",
            "Begone.",
            "Leave me.",
            "Break a leg.",
            "Forgive me.",
            "I insist.",
            "You're dead to me.",
            "For the record.",
            "Meow.",
            "Avoid at all costs.",
            "Identify yourself.",
            "Butter.",
            "Dirt.",
            "Again.",
            "I ejaculate fire.",
            "Big penis.",
            "You know what you did.",
            "Seduce me.",
            "Red spy in base.",
            "Don't touch Sasha.",
            "Listen here.",
            "Remember me.",
            "Stop repeating me.",
            "Look away.",
            "Drink water.",
            "Go to the bathroom.",
            "You're shitting yourself.",
            "How the mighty have fallen.",
            "How the mighty have fallen.",
            "Touch grass.",
            "Job application.",
            "Do not touch.",
            "Destroyer of worlds.",
            "Nuclear.",
            "Oops.",
            "Sorry.",
            "My mistake.",
            "No second chances.",
            "Dropped this.",
            "Wide open.",
            "Strike.",
            "God is coming.",
            "Don't miss.",
            "Don't fail me now.",
            "I thought I hired goons.",
            "Don't quote me on this.",
            "Survive.",
            "Pull aside.",
            "Stay down.",
            "Quickly, now.",
            "Quiet.",
            "Forever and always.",
            "Protect the pilot.",
            "9/11.",
            "So long.",
            "Smite them.",
            "Cope and seethe.",
            "Cry about it.",
            "Don't kill me.",
            "Mushroom.",
            "Good.",
            "Keep it rolling.",
            "Caught on camera.",
            "En route.",
            "Out for delivery.",
            "Something cool.",
            "Impress me.",
            "Did I do that?",
            "On the way.",
            "For you.",
            //"You're on my nerves.",
            "Secret message."
    }; // there are. over . 50. of these messages. dear god.

    public static void fallingArrowFiredCheck(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = event.getLevel();
        // opposite of usual affairs. don't run this on the server!
        if (!level.isClientSide()) return;
        //LOGGER.info("Clientside!!");
        if (!(entity instanceof Arrow arrow)) return;
        if (!(arrow.getOwner() instanceof Player player)) return;

        //LOGGER.info("An arrow just got fired by a player");

        if (player.getDeltaMovement().y > -0.1) return;
        if (!isEpicAndCoolSpin()) return;

        if (!player.getMainHandItem().is(Items.BOW)) {
            player.sendSystemMessage(Component.literal("§3You must be using a Bow."));
            return;
        }

        if (player.fallDistance < NEEDED_FALLEN_BLOCKS) {
            if (player.fallDistance > 0.6) {
                player.sendSystemMessage(Component.literal("§3You must be falling for longer before firing."));
            }
            return;
        }

        if (!player.isCrouching()) {
            player.sendSystemMessage(Component.literal("§3Crouch when you fire the arrow, but be upright when it hits."));
            return;
        }

        if (player.hasEffect(MobEffects.SLOW_FALLING)) {
            player.sendSystemMessage(Component.literal("§3You must be falling at a normal rate."));
            return;
        }

        //player.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.5f, 1.3f);
        player.playSound(SoundEvents.IRON_GOLEM_HURT, 0.65f, 2);
        //player.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.5f, 1.5f);
        int messageIndex = level.getRandom().nextInt(ARROW_FIRED_MESSAGES.length);
        String message = ARROW_FIRED_MESSAGES[messageIndex];

        player.displayClientMessage(Component.literal(String.format("§3/ %s \\", message)), true);
        //player.displayClientMessage(Component.literal(String.format("§3%s", message)), false);

        //LOGGER.info("A really cool arrow just got fired");
        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        ServerboundPreciseArrowPacket.INSTANCE.sendToServer(new ServerboundPreciseArrowPacket(arrow.getUUID(), player.getUUID(), arrow.tickCount, player.position(), date, messageIndex));
    }

    public static boolean preciseArrowDeathCheck(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        Entity attacker = event.getSource().getDirectEntity();

        if (!(attacker instanceof Arrow arrow)) return false;
        if (!(arrow.getOwner() instanceof Player player)) return false;
        boolean selfAttack = attacker.getUUID() == victim.getUUID();

        //LOGGER.info("Arrow fired by player killed something");

        if (!PRECISE_ARROWS.containsKey(arrow.getUUID())) return false;

        ArrowShotStatistics statistics = PRECISE_ARROWS.get(arrow.getUUID());

        long airtime = arrow.tickCount - statistics.tick;
        PRECISE_ARROWS.remove(arrow.getUUID());

        if (player.isCrouching()) {
            player.sendSystemMessage(Component.literal("§3You need to be crouched when the arrow is fired and standing when it hits."));
            return true;
        }

        double horizontalDistance = new Vector2d(player.getX() - victim.getX(), player.getZ() - victim.getZ()).length();

        if (!selfAttack && horizontalDistance < MIN_ARROW_H_DISTANCE) {
            player.sendSystemMessage(Component.literal(String.format("§3Target was too close - must be at least §b" + MIN_ARROW_H_DISTANCE + "§3 blocks away horizontally. The target was %.1fm to you", horizontalDistance)));
            return true;
        }

        //LOGGER.info("it was a precise arrow");

        double airspeed = arrow.getDeltaMovement().length();

        // The player has hit a "precise" arrow.

        double pointsFromDistance = horizontalDistance;
        double pointsFromVelocity = Math.max(0, 5 * (airspeed - 2));
        double pointsFromAirtime = Math.min(60, airtime * 2);
        int points = (int)Math.round(pointsFromDistance + pointsFromVelocity + pointsFromAirtime);

        player.sendSystemMessage(Component.literal(String.format(
                "§3Congratulations.\n\n" +
                        "Horizontal distance to the target: §b%.1fm\n" +
                        "§3Arrow velocity: §b%.1fm/s.\n" +
                        "§3Flight duration: §b%.1fs §3(%s ticks) §8. . %.1f + %.1f + %.1f\n",
                horizontalDistance,
                airspeed,
                (airtime) / 20f,
                airtime,
                pointsFromDistance, pointsFromVelocity, pointsFromAirtime
                )));


        int rank = points / POINTS_PER_RANK;
        String rankSymbol = rankSymbol(rank);

        player.sendSystemMessage(Component.literal(String.format("§b%s points §3- %s",
                points,
                rankSymbol
        )));

        SoundEvent cheer;
        if (rank < 4) {
            cheer = ModSounds.MEH_CHEERING.get();
        } else if (rank < RANK_FOR_S) {
            cheer = ModSounds.GOOD_CHEERING.get();
        } else {
            for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
                if (p.getUUID() == player.getUUID()) continue;
                p.sendSystemMessage(
                        Component.literal(String.format("§b%s§3 just got a Victory Royale. %s points - %s\n§7(%.1fm / %st)",
                                player.getDisplayName().getString(),
                                points, rankSymbol,
                                horizontalDistance, airtime
                        )));
            }
            ModAdvancements.completeAdvancement((ServerLevel)level, (ServerPlayer)player, ModAdvancements.VICTORY_ROYALE);
            cheer = ModSounds.EPIC_CHEERING.get();
            Entity newborn = EntityType.LIGHTNING_BOLT.create(level);
            newborn.setPos(victim.getPosition(0));
            level.addFreshEntity(newborn);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), cheer, SoundSource.PLAYERS, 1, 1);

        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1, 1);


        double distance = player.distanceTo(victim);
        if (distance > 13) {
            Vector3d direction = new Vector3d(
                    victim.getX() - player.getX(),
                    victim.getY() - player.getY(),
                    victim.getZ() - player.getZ())
                    .normalize();
            int soundDistance = 10;
            Vector3d pos = new Vector3d(
                    player.getX() + soundDistance * direction.x,
                    player.getY() + soundDistance * direction.y,
                    player.getZ() + soundDistance * direction.z);

            float distanceAttenuation = Math.min((-6.5f/(float)distance) + 0.5f, 0.5f);

            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1 - distanceAttenuation, 1);
        }

        /*
        public static final String PERFORMER_PATH = "Performer";
        public static final String VICTIM_PATH = "Victim";
        public static final String POINTS_PATH = "Points";
        public static final String DIST_PATH = "Distance";
        public static final String HIT_TICK_PATH = "HitTick";
         */

        ItemStack disc = new ItemStack(ModItems.MUSIC_DISC_KOKOROTOLUNANOFUKAKAI.get());
        CompoundTag big = disc.getOrCreateTag();
        CompoundTag tag = statistics.saveHit(
                player.getDisplayName().getString(),
                victim.getDisplayName().getString(),
                points,
                (float)horizontalDistance,
                airtime,
                victim.position(),
                (float)airspeed
        );
        big.put(ArrowShotStatistics.ROOT_PATH, tag);
        disc.setTag(big);

        ItemEntity newborn = new ItemEntity(
                level,
                victim.getX(),
                victim.getY(),
                victim.getZ(),
                disc
        );
        newborn.setInvulnerable(true);

        level.addFreshEntity(newborn);
        return true;
    }

    public static String rankSymbol(int rank) {
        StringBuilder rankSymbol = new StringBuilder();

        switch(rank) {
            case 0 -> rankSymbol.append("§4§lF-");
            case 1,2 -> rankSymbol.append("§2§lD");
            case 3 -> rankSymbol.append("§3§lC");
            case 4 -> rankSymbol.append("§a§lB");
            default -> {
                if (rank < RANK_FOR_S) {
                    rankSymbol.append("§d§lA");
                } else {
                    rankSymbol.append("§6\uD83D\uDD25 §e§lS§6");
                    int godCount = (rank - 7) / 2;
                    // thanks jetbrains /pos. i guess? i mean this looks neat
                    rankSymbol.append("+".repeat(Math.max(0, godCount)));
                    rankSymbol.append(" \uD83D\uDD25");
                    // basically it adds a "+" for every 6 points above 30 you get
                }
            }
        }
        return rankSymbol.toString();
    }

    public static int ALTITUDE_FOR_ICARUS = 250;
    public static int ANGLE_FOR_ICARUS = 70;
    public static String GLIDER = "immersiveengineering:glider";

    public static boolean icarusCheck(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        Level level = player.level();

        if (level.isClientSide()) return false;

        if (!level.isDay()) return false;

        if (player.getY() < ALTITUDE_FOR_ICARUS) return false;

        //LOGGER.info("above altitude for icarus. look angle is " + player.getViewXRot(0));
        if (player.getViewXRot(0) > -ANGLE_FOR_ICARUS) return false;
        //LOGGER.info("you're looking in the right direction");

        ItemStack glider = player.getItemBySlot(EquipmentSlot.CHEST);

        if (glider.isEmpty()) return false;

        ResourceLocation loc = ForgeRegistries.ITEMS.getKey(glider.getItem());
        //LOGGER.info("the code is here");
        if (loc == null) return false;
        //LOGGER.info(loc.toString());
        if (!loc.toString().equals(GLIDER)) return false;

        //LOGGER.info("damage: " + (glider.getMaxDamage() - glider.getDamageValue() > 10));
        if (glider.getMaxDamage() - glider.getDamageValue() > 10) return false;

        //LOGGER.info("and it was damaged enough");

        // award the disc

        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);

        player.addItem(new ItemStack(ModItems.MUSIC_DISC_PROVIDENCE.get()));

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1, 1);

        return true;
    }

    public static boolean asbestosRenewCheck(TickEvent.PlayerTickEvent event) {
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