package com.robotemployee.reu.foliant.entity;

import com.robotemployee.reu.foliant.FoliantRaid;
import com.robotemployee.reu.util.MobUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.UUID;

public abstract class FoliantRaidMob extends Monster {

    protected FoliantRaidMob(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    long tickCreated;
    public static final int TICKS_UNTIL_DIE_OF_OLD_AGE = 720000;

    protected FoliantRaid parentRaid;
    protected boolean needsRaidParent = true;

    // im spaced out okay
    protected final ArrayList<WeakReference<DevilEntity>> devilsProtectingMe = new ArrayList<>();
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
        applyPowerHealthBuff(power);
    }

    public static final UUID HEALTH_POWER_MODIFIER_UUID = UUID.fromString("4a2063fc-418d-40e2-adcb-e41011fca417");
    protected void applyPowerHealthBuff(float power) {
        if (power < 1) return;
        double multiplier = Math.min(6, 1 + (power * power) * 0.001);

        float healthFraction = getHealth() / getMaxHealth();

        getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(
                HEALTH_POWER_MODIFIER_UUID,
                "Health bonus from raid power",
                multiplier - 1,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));

        setHealth(healthFraction * getMaxHealth());
    }

    public static final UUID DAMAGE_POWER_MODIFIER_UUID = UUID.fromString("7470fc32-423b-4f09-a95e-64a53426bcde");
    protected void applyPowerDamageBuff(int power) {
        if (power < 1) return;
        double multiplier = Math.min(6, 1 + (power * power) * 0.001);

        AttributeInstance attackDamage = getAttribute(Attributes.ATTACK_DAMAGE);

        if (attackDamage == null) return;

        attackDamage.addPermanentModifier(new AttributeModifier(
                DAMAGE_POWER_MODIFIER_UUID,
                "Damage bonus from raid power",
                multiplier - 1,
                AttributeModifier.Operation.MULTIPLY_BASE
        ));
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
        cleanDevilsProtectingMe();
        devilsProtectingMe.add(new WeakReference<>(devil));
    }

    private void removeDevilProtectingMe(@NotNull DevilEntity devil) {
        cleanDevilsProtectingMe();
        devilsProtectingMe.removeIf(reference -> reference.refersTo(devil));
    }

    private void cleanDevilsProtectingMe() {
        devilsProtectingMe.removeIf(reference -> {
            DevilEntity devil = reference.get();
            if (devil == null) return true;
            if (!MobUtils.entityIsValidForTargeting(devil)) return true;
            return false;
        });
    }
    public ArrayList<WeakReference<DevilEntity>> getDevilsProtectingMe() {
        cleanDevilsProtectingMe();
        return devilsProtectingMe;
    }
    public boolean isBeingProtected() {
        cleanDevilsProtectingMe();
        return devilsProtectingMe.size() > 0;
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
    int ticksWantedParentRaidButAlone = 0;
    public boolean shouldIDieRightNow() {
        if (needsRaidParent && !isInRaid()) ticksWantedParentRaidButAlone++;

        return needsRaidParent && (!isInRaid() || getParentRaid().isPoop());
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData groupData, @Nullable CompoundTag tag) {
        // this is horrible logic, the raid should tell its spawned entities that they need a raid parent
        // but whatever
        if (spawnType == MobSpawnType.SPAWN_EGG ||
                spawnType == MobSpawnType.COMMAND ||
                spawnType == MobSpawnType.DISPENSER ||
                spawnType == MobSpawnType.SPAWNER) {
            needsRaidParent = false;
        }
        return super.finalizeSpawn(level, difficulty, spawnType, groupData, tag);
    }

    @Override
    public void tick() {
        super.tick();

        if (shouldIDieRightNow()) kill();
    }
}
