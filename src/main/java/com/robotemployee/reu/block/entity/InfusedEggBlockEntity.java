package com.robotemployee.reu.block.entity;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.ModBlockEntities;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.eggs.LootTableMassDropper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

public class InfusedEggBlockEntity extends BlockEntity {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String CONTAINED_KEY = "contained";
    public static final String COUNT_KEY = "count";
    public static final String TYPE_KEY = "type";
    public static final String OWNER_KEY = "owner";

    protected String occupant;

    protected long population = 1;

    protected UUID owner;

    public InfusedEggBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.INFUSED_EGG_BLOCK_ENTITY.get(), pos, blockState);
    }

    public void setOccupant(String newOccupant) {
        occupant = newOccupant;
    }
    public void setOccupant(Entity entity) {
        setOccupant(entity.getType());
    }

    public void setOccupant(@NotNull EntityType<?> entityType) {
        ResourceLocation loc = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        LOGGER.info("occupant is being set to " + entityType + " Exists:" + (loc == null) + " Loc:" + loc + " Input:" + entityType);
        occupant = loc == null ? null : loc.toString();
    }

    public String getOccupantString() {
        if (!hasOccupant()) {
            LOGGER.error("Trying to get the occupant of an Infused Egg when the occupant is null");
            return "minecraft:pig";
        }
        return occupant;
    }

    public EntityType<?> getOccupant() {
        EntityType<?> occupantType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(getOccupantString()));
        LOGGER.info("Recalling occupant type: " + ForgeRegistries.ENTITY_TYPES.getKey(occupantType));
        return occupantType;
    }

    public boolean hasOccupant() {
        return occupant != null;
    }

    public long getPopulation() {
        return population;
    }

    public void setOwner(Player player) {
        setOwner(player.getUUID());
    }
    public void setOwner(UUID uuid) {
        owner = uuid;
    }
    public UUID getOwnerUUID() {
        return owner;
    }

    public void setup(Player owner, EntityType<?> occupant) {
        setOwner(owner);
        setOccupant(occupant);
    }

    public Player getOwner(Level level) {
        return level.getPlayerByUUID(owner);
    }

    @Nullable
    public Entity createEntity(ServerLevel level) {
        EntityType<?> newbornType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(occupant));
        if (newbornType == null) return null;
        return newbornType.create(level);
    }

    public void dropOnRemove(@NotNull BlockState oldState, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        //LOGGER.info("Block was removed!");
        //LOGGER.info(oldState.getBlock().getDescriptionId() + " -> " + newState.getBlock().getDescriptionId());
        if (oldState.getBlock() == newState.getBlock() || level.isClientSide()) return;
        //LOGGER.info("Passed first check");
        //LOGGER.info(String.valueOf(level.getBlockEntity(pos) != null));
        if (!(level.getBlockEntity(pos) instanceof InfusedEggBlockEntity infusedEggBlockEntity)) return;
        //LOGGER.info("and it knows it's a block entity!");
        Container dropsContainer = infusedEggBlockEntity.getDropsContainer((ServerLevel)level);
        if (dropsContainer == null) return;
        //LOGGER.info("Size: " + dropsContainer.getContainerSize());
        Containers.dropContents(level, pos, dropsContainer);
    }

    @Nullable
    public Container getDropsContainer(ServerLevel level) {
        Collection<ItemStack> drops = getDrops(level);
        if (drops == null) return null;
        SimpleContainer container = new SimpleContainer(drops.size());
        for (ItemStack drop : drops) container.addItem(drop);
        return container;
    }

    @Nullable
    public Collection<ItemStack> getDrops(ServerLevel level) {
        // why the hell should we load the entity when we're just gonna get its type anyway
        //Entity entity = EntityType.loadEntityRecursive(getOccupantTag(), level, (entity1) -> entity1);
        EntityType<?> entityType = getOccupant();
        // this should never happen but whatever
        if (entityType == null) return null;

        Player player = getOwner(level);
        ObjectArrayList<ItemStack> result = new ObjectArrayList<>();
        LootParams params = (new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atBottomCenterOf(getBlockPos()).add(0, 0.1, 0))).withParameter(LootContextParams.THIS_ENTITY, player).withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player)).create(LootContextParamSets.ENTITY);
        LootContext context = new LootContext.Builder(params).create(null);

        LootTable table = LootTableMassDropper.getLootTable(entityType.getDefaultLootTable(), level.getServer().getLootData());
        // and so it begins


        result.addAll(LootTableMassDropper.simulateDrops(table, params, context, level, getPopulation()));


        return result;
    }

    private CompoundTag createTag() {
        return createTag(getPopulation(), getOccupantString(), getOwnerUUID());
    }

    public static CompoundTag createTag(long population, String occupant, UUID owner) {
        CompoundTag tag = new CompoundTag();
        tag.putLong(COUNT_KEY, population);
        tag.putString(TYPE_KEY, occupant);
        tag.putUUID(OWNER_KEY, owner);
        return tag;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag bigTag) {
        super.saveAdditional(bigTag);
        bigTag.put(CONTAINED_KEY, createTag());
    }

    @Override
    public void load(@NotNull CompoundTag bigTag) {
        super.load(bigTag);
        CompoundTag tag = bigTag.getCompound(CONTAINED_KEY);
        population = tag.getLong(COUNT_KEY);
        occupant = tag.getString(TYPE_KEY);
        owner = tag.getUUID(OWNER_KEY);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, InfusedEggBlockEntity egg) {
        if (level.isClientSide() || level.getGameTime() % 60 > 0) return;

        egg.grow();
        LOGGER.info("Ticking egg at " + pos);
    }




    public void grow() {
        grow(1);
    }
    public void grow(int amount) {
        population += amount;
        population = (long)Math.floor(population * 1.02);
    }
}
