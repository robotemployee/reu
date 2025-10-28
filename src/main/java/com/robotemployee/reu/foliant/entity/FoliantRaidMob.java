package com.robotemployee.reu.foliant.entity;

import com.robotemployee.reu.foliant.FoliantRaid;
import com.robotemployee.reu.core.ModEntityDataSerializers;
import com.robotemployee.reu.util.MobUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public abstract class FoliantRaidMob extends Monster {

    protected static final EntityDataAccessor<List<Integer>> DEVILS_PROTECTING_ME_IDS = SynchedEntityData.defineId(FoliantRaidMob.class, ModEntityDataSerializers.INTEGER_LIST.get());
    protected FoliantRaidMob(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
        this.entityData.define(DEVILS_PROTECTING_ME_IDS, new ArrayList<>());
    }

    long tickCreated;
    public static final int TICKS_UNTIL_DIE_OF_OLD_AGE = 720000;

    protected FoliantRaid parentRaid;
    @Nullable
    public FoliantRaid getParentRaid() {
        return parentRaid;
    }

    public void setParentRaid(FoliantRaid raid) {
        parentRaid = raid;
    }

    public boolean isInRaid() {
        return getParentRaid() != null;
    }

    public void init(@NotNull FoliantRaid parentRaid) {
        setParentRaid(parentRaid);
        if (isInRaid()) {
            applyPowerBuffs(getParentRaid().getPowerFloat());
            getParentRaid().incrementPopulation(getEnemyType());
        }
        tickCreated = level().getGameTime();
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (isInRaid()) removeFromRaid();
    }

    @Override
    public boolean removeWhenFarAway(double p_21542_) {
        return !this.hasCustomName() && !this.isPersistenceRequired();
    }

    protected boolean canDieOfOldAge() {
        return true;
    }

    // with overriding removeWhenFarAway and FoliantRaid's chunkloading, it is best to automatically remove us after a long time
    protected boolean shouldDieOfOldAge() {
        return canDieOfOldAge() && level().getGameTime() - tickCreated > TICKS_UNTIL_DIE_OF_OLD_AGE;
    }

    public void removeFromRaid() {
        if (!isInRaid()) return;
        getParentRaid().decrementPopulation(getEnemyType());
        setParentRaid(null);
    }

    protected void applyPowerBuffs(float power) {
        applyHealthBuff(power);
    }

    public static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("4a2063fc-418d-40e2-adcb-e41011fca417");
    protected void applyHealthBuff(float power) {
        if (power < 1) return;
        double multiplier = Math.min(6, 1 + (power * power) * 0.001);

        float healthFraction = getHealth() / getMaxHealth();

        getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(
                HEALTH_MODIFIER_UUID,
                "Health bonus from raid power",
                multiplier - 1,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));

        setHealth(healthFraction * getMaxHealth());
    }

    // Devil Protection
    public void startProtectionFrom(@NotNull DevilEntity devil) {
        addDevilProtectingMe(devil);
    }
    // VERY IMPORTANT TO CALL WHENEVER YOU ARE NO LONGER BEING PROTECTED
    // todo make this get called when the Protection thingy stops
    public void stopProtectionFrom(@NotNull DevilEntity devil) {
        removeDevilProtectingMe(devil);
    }

    private void addDevilProtectingMe(@NotNull DevilEntity devil) {
        List<Integer> ids = getDevilsProtectingMeIds();
        ids.add(devil.getId());
        saveDevilsProtectingMe(ids);
    }

    private void removeDevilProtectingMe(@NotNull DevilEntity devil) {
        List<Integer> ids = getDevilsProtectingMeIds();
        ids.remove((Object)devil.getId());
        saveDevilsProtectingMe(ids);
    }

    private void saveDevilsProtectingMe(List<Integer> ids) {
        getEntityData().set(DEVILS_PROTECTING_ME_IDS, ids);
    }

    public List<Integer> getDevilsProtectingMeIds() {
        return getEntityData().get(DEVILS_PROTECTING_ME_IDS);
    }

    public Stream<DevilEntity> getDevilsProtectingMe() {
        List<Integer> ids = getDevilsProtectingMeIds();
        return ids.stream().map(id -> (DevilEntity)level().getEntity(id));
    }
    public boolean isBeingProtected() {
        return getDevilsProtectingMeIds().size() > 0;
    }

    public void cleanDevilsProtectingMe() {
        getDevilsProtectingMe().forEach(devil -> {
            if (!MobUtils.entityIsValidForTargeting(devil)) removeDevilProtectingMe(devil);
        });
    }

    public boolean canDevilProtect() {
        return !isBeingProtected() && getDevilProtectionWeight() > 0;
    }
    public float getDevilProtectionWeight() {
        return 1f;
    }

    public abstract FoliantRaid.EnemyType getEnemyType();

    public boolean canRecycle() {
        return true;
    }

    // This is for spawning in
    // This value is also considered negatively - the higher it is, the less of them are spawned before the director is satisfied
    public abstract float getPresenceImportance();

    // presence importance * recycle importance factor = perceived value for recycling from asteirtos
    protected float getRecycleImportanceFactor() {
        return 1;
    }

    public final float getRecycleImpedance() {
        return getPresenceImportance() * getRecycleImportanceFactor();
    }

    // This is for determining how effectively a creature is in fulfilling their role -
    // if a creature is very badly wounded, it should report a lower fulfillment rating.
    // 0 to 1.
    public float getFulfillment() {
        return (getHealth() / (2 * getMaxHealth())) + 0.5f;
    }

    // override this if you want something to be able to survive without a raid
    public boolean shouldIDieRightNow() {
        return (!isInRaid() || getParentRaid().isPoop());
    }

    @Override
    public void tick() {
        super.tick();

        if (shouldIDieRightNow()) kill();
    }
}
