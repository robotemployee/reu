package com.robotemployee.reu.banana.entity;

import com.robotemployee.reu.banana.BananaRaid;
import com.robotemployee.reu.core.ModEntityDataSerializers;
import com.robotemployee.reu.util.MobUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class BananaRaidMob extends Monster {

    protected static final EntityDataAccessor<List<Integer>> DEVILS_PROTECTING_ME_IDS = SynchedEntityData.defineId(BananaRaidMob.class, ModEntityDataSerializers.INTEGER_LIST.get());
    protected BananaRaidMob(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.entityData.define(DEVILS_PROTECTING_ME_IDS, new ArrayList<>());
    }

    protected BananaRaid parentRaid;
    public BananaRaid getParentRaid() {
        return parentRaid;
    }

    public void init(BananaRaid parentRaid) {
        this.parentRaid = parentRaid;
    }

    // note that all of these weights are treated as multipliers
    boolean canAirlift() {
        return getAirliftWeight() > 0;
    }
    // when the raid is deciding what should be airlifted to where, this is the weight
    // bananas with higher weight will be airlifted first
    // additionally, this is specifically for when the raid is proactively looking for things to airlift
    // if an entity wants to specifically request an airlift, it can do so with requestAirliftTo()
    public abstract float getAirliftWeight();

    // Devil Protection
    protected ArrayList<DevilEntity> devilsProtectingMe = new ArrayList<>();
    public void startProtectionFrom(@NotNull DevilEntity devil) {
        addDevilProtectingMe(devil);
    }
    // VERY IMPORTANT TO CALL WHENEVER YOU ARE NO LONGER BEING PROTECTED
    // todo make this get called when the Protection thingy stops
    public void stopProtectionFrom(@NotNull DevilEntity devil) {
        removeDevilProtectingMe(devil);
    }

    private void addDevilProtectingMe(@NotNull DevilEntity devil) {
        devilsProtectingMe.add(devil);
        List<Integer> ids = getDevilsProtectingMeIds();
        ids.add(devil.getId());
        saveDevilsProtectingMe(ids);
    }

    private void removeDevilProtectingMe(@NotNull DevilEntity devil) {
        List<Integer> ids = getDevilsProtectingMeIds();
        ids.remove(devil.getId());
        saveDevilsProtectingMe(ids);
        devilsProtectingMe.remove(devil);
    }

    private void saveDevilsProtectingMe(List<Integer> ids) {
        getEntityData().set(DEVILS_PROTECTING_ME_IDS, ids);
    }

    public List<Integer> getDevilsProtectingMeIds() {
        return getEntityData().get(DEVILS_PROTECTING_ME_IDS);
    }

    public List<DevilEntity> getDevilsProtectingMe() {
        if (level().isClientSide()) {
            List<Integer> ids = getDevilsProtectingMeIds();
            List<DevilEntity> devils = ids.stream().map(id -> (DevilEntity)level().getEntity(id)).toList();
            return devils;
        }
        return devilsProtectingMe;
    }
    public boolean isBeingProtected() {
        return getDevilsProtectingMe().size() > 0;
    }

    public void cleanDevilsProtectingMe() {
        for (DevilEntity devil : getDevilsProtectingMe()) {
            if (!MobUtils.entityIsValidForTargeting(devil)) removeDevilProtectingMe(devil);
        }
    }

    public boolean canDevilProtect() {
        return !isBeingProtected() && getDevilProtectionWeight() > 0;
    }
    public float getDevilProtectionWeight() {
        return 1f;
    }


    // Recycling
    public abstract float getRecycleWeight();

    public abstract BananaRaid.EnemyTypes getBananaType();

    public void requestAirliftTo(BlockPos destination) {
        getParentRaid().requestAirlift(this.getUUID(), destination);
    }
}
