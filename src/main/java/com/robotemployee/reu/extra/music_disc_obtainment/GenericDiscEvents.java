package com.robotemployee.reu.extra.music_disc_obtainment;

import com.github.alexmodguy.alexscaves.server.entity.living.NucleeperEntity;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;
import com.github.alexthe666.alexsmobs.entity.ai.GroundPathNavigatorWide;
import com.mojang.logging.LogUtils;
import com.robotemployee.reu.capability.BetrayalCapability;
import com.robotemployee.reu.capability.FlowerCounterCapability;
import com.robotemployee.reu.capability.FlowerCounterCapability.FlowerCounter;
import com.robotemployee.reu.mobeffect.TummyAcheMobEffect;
import com.robotemployee.reu.registry.ModAdvancements;
import com.robotemployee.reu.registry.ModItems;
import com.robotemployee.reu.registry.ModSounds;
import com.robotemployee.reu.util.LevelUtils;
import com.supermartijn642.rechiseled.ChiselItem;
import io.wispforest.accessories.api.AccessoriesCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

//@OnlyIn(Dist.DEDICATED_SERVER)
public class GenericDiscEvents {

    static Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (lobotomyCheck(event)) return;
        if (bearSpecialAbilityCheck(event)) return;
        if (ironGolemEvisceratedCheck(event)) return;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (chickenLightningDeathCheck(event)) return;
        if (bearFairDeathCheck(event)) return;
        if (bearLifestealKillCheck(event)) return;
        if (preciseArrowDeathCheck(event)) return;
        betrayalDeathCheck(event);
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
        if (feedNucleeperCheck(event)) return;
        tamedMobCheck(event);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        betrayalSetHomePosOnJoin(event);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (icarusCheck(event)) return;
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (attachFlowerCounterCapability(event)) return;
        attachBetrayalCapability(event);
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

    public static boolean bearSpecialAbilityCheck(LivingAttackEvent event) {
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

        //if (health < 0.35) victim.addEffect(new MobEffectInstance(AMEffectRegistry.KNOCKBACK_RESISTANCE.get(), 200, 0));
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


        if (victim.hasEffect(AMEffectRegistry.KNOCKBACK_RESISTANCE.get()) && random.nextBoolean()) victim.removeEffect(AMEffectRegistry.KNOCKBACK_RESISTANCE.get());
        else victim.addEffect(new MobEffectInstance(AMEffectRegistry.KNOCKBACK_RESISTANCE.get(), 100, 0));

        //if (random.nextFloat() < CHANCE_TO_KNOCKBACK_RESIST) victim.addEffect(new MobEffectInstance(AMEffectRegistry.KNOCKBACK_RESISTANCE.get(), 20, 0));

        victim.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        victim.removeEffect(MobEffects.WEAKNESS);
        victim.removeEffect(MobEffects.BLINDNESS);
        victim.removeEffect(MobEffects.POISON);
        victim.removeEffect(MobEffects.WITHER);
        return true;

    }

    public static final int GOLEM_EVISCERATION = 50;

    // TODO: Swap this to reward a Home Depot disc instead
    public static boolean ironGolemEvisceratedCheck(LivingAttackEvent event) {
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
                new ItemStack(ModItems.MUSIC_DISC_STEEL_HAZE.get())
        );
        newborn.setInvulnerable(true);

        level.addFreshEntity(newborn);
        return true;
    }

    // When a chicken dies to a lightning strike, you get birdbrain
    // 8% chance
    public static final float CHICKEN_CHANCE_TO_DROP = 0.08f;
    public static boolean chickenLightningDeathCheck(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!event.getSource().is(DamageTypes.LIGHTNING_BOLT)) return false;
        Level level = victim.level();
        if (level.isClientSide()) return false;
        RandomSource random = level.getRandom();
        if (random.nextFloat() > CHICKEN_CHANCE_TO_DROP) return false;

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

    public static boolean bearFairDeathCheck(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        LivingEntity attacker = victim.getLastAttacker();
        Level level = victim.level();

        if (!(victim instanceof EntityGrizzlyBear)) return false;

        //LOGGER.info(String.format("victim:%s attacker:%s client:%s player:%s empty:%s maxhealth:%s armor:%s", victim.getEncodeId(), attacker == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType()), level.isClientSide(), attacker instanceof Player, attacker.getMainHandItem().isEmpty(), attacker.getMaxHealth(), attacker.getArmorValue()));
        if (!(attacker instanceof ServerPlayer player)) return false;
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
        BlockPos groundPos = LevelUtils.findSolidGroundBelow(level, player.blockPosition());


        //level.g

        // preliminary
        boolean playerFlying = player.getAbilities().flying;
        boolean pathNull = path == null;
        boolean pathHasNodes = !pathNull && path.getNodeCount() > 0;
        // these are all summarized into unreachable
        boolean pathCantReach = !pathNull && !path.canReach();
        boolean pathTargetTooFar = !pathNull && !path.getTarget().closerToCenterThan(player.position(), 1);
        boolean pathEndNodeVerticalTooFar = pathHasNodes && groundPos != null && path.getEndNode() != null && (groundPos.above().getY() - path.getEndNode().asBlockPos().getY() > 1);
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

        // Code to check if the bear is in a cobweb
        boolean isInCobweb = false;

        AABB checkedBlocks = victim.getBoundingBox().inflate(0.01);
        for (int x = (int)Math.floor(checkedBlocks.minX); x < Math.ceil(checkedBlocks.maxX); x++)
        for (int y = (int)Math.floor(checkedBlocks.minY); y < Math.ceil(checkedBlocks.maxY); y++)
        for (int z = (int)Math.floor(checkedBlocks.minZ); z < Math.ceil(checkedBlocks.maxZ); z++)
            if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.COBWEB)) {
                isInCobweb = true;
                break;
            }

        boolean enchantedArmor = false;
        boolean armorNotDiamondOrEmpty = false;
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.isEnchanted()) {
                enchantedArmor = true;
                if (armorNotDiamondOrEmpty) break;
            }

            if (!armor.isEmpty() && !(armor.getItem() instanceof ArmorItem armorItem && armorItem.getMaterial().equals(ArmorMaterials.DIAMOND))) {
                armorNotDiamondOrEmpty = true;
                if (enchantedArmor) break;
            }
        }

        AccessoriesCapability handler = AccessoriesCapability.get(player);

        boolean hasCurios = handler != null && handler.isEquipped(stack -> !stack.isEmpty());
        boolean tooMuchHealth = playerMaxHealth > ALLOWED_BEAR_MAX_HP;
        //boolean tooMuchArmor = player.getArmorValue() > ALLOWED_BEAR_ARMOR;
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

        if (hasCurios) complaints.add("you were wearing extra accessories");
        if (tooMuchHealth) complaints.add(String.format("you had %.0f extra hearts", playerMaxHealth));
        if (armorNotDiamondOrEmpty) complaints.add("you wore armor that wasn't diamond");
        if (enchantedArmor) complaints.add("your armor was enchanted");
        if (tooFar) complaints.add(String.format("the distance was %.0fm", distance));
        if (friendly) complaints.add("the bear was tamed");
        if (tooMuchReach) complaints.add("you could reach really far");
        if (hampered) complaints.add("the bear was impeded");
        if (unreachable) complaints.add("the bear could not reach you");

        boolean wasUnfair = complaints.size() > 0;
        MinecraftServer server = level.getServer();
        if (server == null) {
            LOGGER.error("Server is null when deter");

        }
        boolean firstCompletion = !ModAdvancements.isAdvancementComplete(player, ModAdvancements.OBTAINED_CLAIRO_DISC);

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

    public static boolean bearLifestealKillCheck(LivingDeathEvent event) {
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

    public static boolean isCritting(LivingEntity entity) {
        return entity.getDeltaMovement().y < 0 && entity.fallDistance > 0;
    }

    public static final int FLOWERS_NEEDED = 25;
    public static final int ANGLE_FOR_EAT_FLOWER = 30;

    public static final String[] IN_BETWEEN_MESSAGES = {
            "Your progress, I'm sure, is coming as it should. %s",
            "Can you feel it? I hope it's not too warm. %s",
            "Share what you love. %s",
            "Eventually, you will finish your quest. Eventually. %s",
            "Is it bitter? %s",
            "Wash on, wash off. Wash on, wash off. %s",
            "The unexpected is ine%svitable.",
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
            "How many more will there be? %s",
            "You aren't learning anything. %s",
            "We're having a one-way conversation. %s",
            "Regardless. %s",
            "Refrigerator. Huge, huge refrigerator. Refrigerator. %s",
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

        if (player.getViewXRot(0) > -ANGLE_FOR_EAT_FLOWER) {
            player.sendSystemMessage(Component.literal("§3Look up."));
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
            player.sendSystemMessage(Component.literal("§3Welcome to your flower quest. Whether you're returning or starting anew, I trust that inner peace will be with you when you are ready.\n\nIn just " + needed + " more unique flowers, the Blossom will be yours."));
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

        if (!player.getAbilities().instabuild) flower.shrink(1);
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
    // if your item stack's count is in between these, you will have a much higher chance
    // aka this is the "vastly increased when food count is low" i just mentioned
    public static final int COUNT_INCREASE_CHANCE_START = 20;
    public static final int COUNT_INCREASE_CHANCE_END = 16;
    public static final Supplier<Item> NUCLEEPER_FOOD = () -> Items.GLOWSTONE_DUST;

    public static boolean feedNucleeperCheck(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        Level level = player.level();

        if (level.isClientSide()) return false;

        if (!(target instanceof NucleeperEntity)) return false;

        ItemStack food = player.getItemInHand(event.getHand());

        if (!food.is(NUCLEEPER_FOOD.get())) return false;

        if (!player.isCrouching()) {
            player.sendSystemMessage(Component.literal("§bCrouch §3in order to give it your respect and admiration"));
        }

        if (player.getCooldowns().isOnCooldown(NUCLEEPER_FOOD.get())) return true;

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
            return true;
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
        return true;
    }

    public static boolean tamedMobCheck(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        Level level = player.level();

        if (level.isClientSide()) return false;

        if (!(target instanceof TamableAnimal tamable)) return false;
        if (!Objects.equals(tamable.getOwnerUUID(), player.getUUID())) return false;

        BetrayalCapability.BetrayalHandler handler = target.getCapability(BetrayalCapability.CAPABILITY).resolve().orElse(null);
        if (handler == null) return false;

        if (handler.hasHomePosition()) return false;

        handler.setHomePosition(target.blockPosition());
        //LOGGER.info("Set the home position to " + target.blockPosition());

        return true;
    }

    // MODIFIED SERVERSIDE ONLY
    // UUID is of course the arrow. The Long is the time that it was fired
    public static final HashMap<UUID, ArrowShotStatistics> PRECISE_ARROWS = new HashMap<>();
    // some of these are used only in ClientOnlyDiscEvents
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

        private static final String ROOT_PATH = "ShotStatistics";
        private static final String TICK_PATH = "Tick";
        private static final String DATE_PATH = "Date";
        private static final String MESSAGE_INDEX_PATH = "Message";

        // for when the arrow is fired
        public ArrowShotStatistics(long time, Vec3 position, Date date) {
            this.tick = time;
            this.position = position;
            this.date = date;
        }

        // basically just deserializing from an item stack lol
        public ArrowShotStatistics(ItemStack stack) {
            CompoundTag bigTag = stack.getTag();
            if (bigTag == null) {
                tick = 0;
                position = Vec3.ZERO;
                date = Date.from(Instant.EPOCH);
                return;
            }
            CompoundTag tag = bigTag.getCompound(ROOT_PATH);
            this.position = new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));

            this.tick = tag.getLong(TICK_PATH);
            this.date = Date.from(Instant.ofEpochMilli(tag.getLong(DATE_PATH)));
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();

            tag.putDouble("X", position.x);
            tag.putDouble("Y", position.y);
            tag.putDouble("Z", position.z);
            tag.putLong(TICK_PATH, tick);
            tag.putLong(DATE_PATH, date.getTime());

            return tag;
        }

        // Extra information that is only available after the arrow has hit

        public static final String PERFORMER_PATH = "Performer";
        public static final String VICTIM_PATH = "Victim";
        public static final String POINTS_PATH = "Points";
        public static final String H_DIST_PATH = "Distance";
        public static final String AIRTIME_PATH = "Airtime";
        public static final String HIT_VELOCITY_PATH = "Velocity";

        public CompoundTag saveHit(String coolGuy, String victim, int points, float distance, long airtime, Vec3 hitPosition, float hitVelocity, int messageIndex) {
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
            tag.putInt(MESSAGE_INDEX_PATH, messageIndex);
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

            int messageIndex = tag.getInt(MESSAGE_INDEX_PATH);
            String message = messageIndex == -1 ? "Congratulations." : ARROW_FIRED_MESSAGES[messageIndex];

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
            "Have at it.",
            "And away it goes.",
            "See you.",
            "Off to the hills.",
            "Far and away.",
            "Until next.",
            "Bye.",
            "Hey there.",
            "Farewell.",
            "早上好中国。",
            "现在我有冰淇淋。",
            "You murderer.",
            "How astute.",
            "Away with you.",
            "太マラ。",
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
            "Nine. Eleven.",
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
    }; // i might have made too many of these messages

    public static boolean betrayalSetHomePosOnJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = event.getLevel();
        // shut up
        BetrayalCapability.BetrayalHandler.BetrayalType betrayalType = BetrayalCapability.BetrayalHandler.getBetrayalType(entity);
        if (betrayalType != BetrayalCapability.BetrayalHandler.BetrayalType.GOLEM) return false;
        // we want to set the home position as the spawn position
        // when it's a golem
        BetrayalCapability.BetrayalHandler handler = entity.getCapability(BetrayalCapability.CAPABILITY).resolve().orElse(null);
        if (handler == null) return false;
        if (handler.hasHomePosition()) return false;
        handler.setHomePosition(entity.blockPosition());
        return true;
    }

    public static boolean preciseArrowDeathCheck(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        Entity attacker = event.getSource().getDirectEntity();

        if (!(attacker instanceof Arrow arrow)) return false;
        if (!(arrow.getOwner() instanceof ServerPlayer player)) return false;
        boolean selfAttack = attacker.getUUID() == victim.getUUID();

        if (level.isClientSide()) return false;

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

        boolean doneBefore = ModAdvancements.isAdvancementComplete(player, ModAdvancements.OBTAINED_KOKOROTOLUNANOFUKAKAI_DISC);

        int messageIndex = -1;

        String message;
        if (doneBefore) {
            messageIndex = level.getRandom().nextInt(ARROW_FIRED_MESSAGES.length);
            message = ARROW_FIRED_MESSAGES[messageIndex];
        } else {
            message = "Congratulations.";
        }

        player.sendSystemMessage(Component.literal(String.format(
                "§3// " + message + "\n\n" +
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
            ModAdvancements.completeAdvancement(player, ModAdvancements.VICTORY_ROYALE);
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
                (float)airspeed,
                messageIndex
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

    public static boolean betrayalDeathCheck(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) return false;

        DamageSource source = event.getSource();

        if (!(source.getDirectEntity() instanceof Player player)) return false;

        if (!player.getMainHandItem().isEmpty()) return false;

        BetrayalCapability.BetrayalHandler handler = victim.getCapability(BetrayalCapability.CAPABILITY).resolve().orElse(null);
        if (handler == null) return false;

        if (!handler.isBeingBetrayed(victim, player)) return false;

        player.sendSystemMessage(Component.literal("§3How could you?"));

        // A player has killed a snow golem 200 blocks away from where it was made.

        level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1, 1);
        ItemEntity newborn = new ItemEntity(
                level,
                victim.getX(),
                victim.getY(),
                victim.getZ(),
                new ItemStack(ModItems.MUSIC_DISC_STEEL_HAZE.get())
        );
        level.addFreshEntity(newborn);

        return true;
    }

    public static final int ALTITUDE_FOR_ICARUS = 250;
    public static final int ANGLE_FOR_ICARUS = 70;
    public static final int DURABILITY_FOR_ICARUS = 15;
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
        if (glider.getMaxDamage() - glider.getDamageValue() > DURABILITY_FOR_ICARUS) return false;

        //LOGGER.info("and it was damaged enough");

        // award the disc

        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);

        player.addItem(new ItemStack(ModItems.MUSIC_DISC_PROVIDENCE.get()));

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1, 1);

        return true;
    }

    public static boolean attachFlowerCounterCapability(AttachCapabilitiesEvent<Entity> event) {
        if (!(event.getObject() instanceof Player player)) return false;

        if (player.getCapability(FlowerCounterCapability.CAPABILITY).isPresent()) return false;

        event.addCapability(FlowerCounterCapability.ID, new FlowerCounterCapability());
        return true;
    }
    public static boolean attachBetrayalCapability(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (!BetrayalCapability.BetrayalHandler.canBeBetrayed(entity)) return false;

        if (entity.getCapability(BetrayalCapability.CAPABILITY).isPresent()) return false;

        event.addCapability(BetrayalCapability.ID, new BetrayalCapability(BlockPos.ZERO));

        return true;
    }
}