package com.robotemployee.reu.mixin.sculkhorde;

import com.github.sculkhorde.core.ModSavedData;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.systems.HitSquadDispatcherSystem;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.EntityAlgorithms;
import com.github.sculkhorde.util.PlayerProfileHandler;
import com.robotemployee.reu.extra.SculkHordeCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;
import java.util.Optional;

@Mixin(HitSquadDispatcherSystem.class)
public class HitSquadDispatcherSystemMixin {

    @Shadow(remap = SculkHordeCompat.remapNormalSculkHorde)
    public static int MAX_RELATIONSHIP;
    @Shadow(remap = SculkHordeCompat.remapNormalSculkHorde)
    public static int MIN_NODES_DESTROYED;

    @Shadow(remap = SculkHordeCompat.remapNormalSculkHorde)
    public static int DISTANCE_REQUIRED_FROM_NODE;

    // things i changed have CHANGED next to them
    @Redirect(method = "getNextTarget", at = @At(value = "INVOKE", target = "Ljava/util/Optional;empty()Ljava/util/Optional;"), remap = SculkHordeCompat.remapNormalSculkHorde)
    //@Redirect(method = "getNextTarget", at = @At("HEAD"), remap = false)
    Optional<Player> getNextTarget() {
        Optional<Player> target = Optional.empty();
        int worstReputationSoFar = MAX_RELATIONSHIP + 1;
        Iterator var3 = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().iterator();

        while(true) {
            ModSavedData.PlayerProfileEntry profile;
            boolean hasNotDestroyedEnoughNodes;
            do {
                boolean hasGoodRelationshipWithHorde;
                do {
                    boolean isHitCooldownNotOver;
                    do {
                        boolean isTooFarFromNode;
                        do {
                            do {
                                Player player;
                                Optional entry;
                                do {
                                    do {
                                        do {
                                            do {
                                                do {
                                                    if (!var3.hasNext()) {
                                                        return target;
                                                    }

                                                    player = (Player)var3.next();
                                                    profile = PlayerProfileHandler.getOrCreatePlayerProfile(player);
                                                } while(!profile.isPlayerOnline());
                                            } while(EntityAlgorithms.isLivingEntityExplicitDenyTarget((LivingEntity)profile.getPlayer().get()));
                                        } while(ModSavedData.getSaveData().getNodeEntries().isEmpty());
                                    } while(!SculkHorde.gravemind.isEvolutionInMatureState());

                                    hasNotDestroyedEnoughNodes = profile.getNodesDestroyed() < MIN_NODES_DESTROYED;
                                    hasGoodRelationshipWithHorde = profile.getRelationshipToTheHorde() > MAX_RELATIONSHIP;
                                    isHitCooldownNotOver = !profile.isHitCooldownOver();
                                    entry = ModSavedData.getSaveData().getClosestNodeEntry((ServerLevel)player.level(), player.blockPosition());
                                } while(entry.isEmpty());

                                // CHANGED
                                isTooFarFromNode = SculkHordeCompat.isOutOfBounds(player) || BlockAlgorithms.getBlockDistanceXZ(player.blockPosition(), ((ModSavedData.NodeEntry)entry.get()).getPosition()) > (float)DISTANCE_REQUIRED_FROM_NODE;
                            } while(isTooFarFromNode);
                        } while(isHitCooldownNotOver);
                    } while(hasGoodRelationshipWithHorde);
                } while(hasNotDestroyedEnoughNodes);
            } while(!target.isEmpty() && profile.getRelationshipToTheHorde() >= worstReputationSoFar);

            target = profile.getPlayer();
            worstReputationSoFar = profile.getRelationshipToTheHorde();
        }
    }
}
