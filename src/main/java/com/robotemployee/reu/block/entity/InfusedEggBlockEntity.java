package com.robotemployee.reu.block.entity;

import com.google.common.collect.Lists;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

public class InfusedEggBlockEntity extends BlockEntity {

    public final String CONTAINED_KEY = "contained";
    public final String COUNT_KEY = "count";
    public final String TYPE_KEY = "type";

    public InfusedEggBlockEntity(BlockPos pos, BlockState blockState) {
        super(RobotEmployeeUtils.INFUSED_EGG_BLOCK_ENTITY.get(), pos, blockState);
    }

    // entity type id
    protected String occupant;

    protected long population = 1;

    protected UUID owner;

    public void setOccupant(String newOccupant) {
        occupant = newOccupant;
    }
    public void setOccupant(Entity entity) {
        setOccupant(entity.getType());
    }

    public void setOccupant(EntityType<?> entityType) {
        ResourceLocation loc = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        occupant = loc == null ? null : loc.toString();
    }

    public String getOccupant() {
        return occupant;
    }

    public long getPopulation() {
        return population;
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
    }
    public void setOwner(UUID uuid) {
        owner = uuid;
    }
    public UUID getOwnerUUID() {
        return owner;
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

    // so this has to implement a custom loot pool parser


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
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(getOccupant()));
        // this should never happen but whatever
        if (entityType == null) return null;
        Optional<LootTable> tableOptional = level.getServer().getLootData().getElementOptional(LootDataType.TABLE, entityType.getDefaultLootTable());
        if (tableOptional.isEmpty()) return null;
        LootTable table = tableOptional.get();

        Player player = getOwner(level);
        ObjectArrayList<ItemStack> returnedItems = new ObjectArrayList<>();
        LootParams params = (new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atBottomCenterOf(getBlockPos()).add(0, 0.1, 0))).withParameter(LootContextParams.THIS_ENTITY, player).withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(player)).create(LootContextParamSets.ENTITY);
        LootContext context = new LootContext.Builder(params).create(null);
        try {
            Field poolsField = LootTable.class.getDeclaredField("pools");
            poolsField.setAccessible(true);
            Field entriesField = LootPool.class.getDeclaredField("entries");
            entriesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<LootPool> pools = (List<LootPool>)poolsField.get(table);
            for (LootPool pool : pools) {
                long multiplier = (long)(pool.getRolls().getFloat(context) * population);
                LootPoolEntryContainer[] entries = (LootPoolEntryContainer[])entriesField.get(pool);
                Consumer<ItemStack> consumer = returnedItems::add;
                addItems(consumer, context, entries, multiplier);
            }
            // global loot tables stuff
            net.minecraftforge.common.ForgeHooks.modifyLoot(table.getLootTableId(), returnedItems, context);
            return returnedItems;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;


    }

    public void addItems(Consumer<ItemStack> consumer, LootContext context, LootPoolEntryContainer[] entries, long multiplier) {
        Consumer<ItemStack> singleWrapper = (stack) -> {
            long amount = stack.getCount() * multiplier;
            // integer division
            int completeStacks = (int) (amount / stack.getMaxStackSize());
            if (completeStacks > 0) {
                for (int i = 0; i < completeStacks; i++) consumer.accept(stack.copyWithCount(stack.getMaxStackSize()));
            }
            int leftover = (int) (amount - completeStacks * stack.getMaxStackSize());
            if (leftover > 0) {
                consumer.accept(stack.copyWithCount(leftover));
            }
        };
        //RandomSource randomsource = context.getRandom();
        List<LootPoolEntry> list = Lists.newArrayList();
        MutableInt mutableint = new MutableInt();

        for(LootPoolEntryContainer container : entries) {
            container.expand(context, (entry) -> {
                int k = entry.getWeight(context.getLuck());
                if (k > 0) {
                    list.add(entry);
                    mutableint.add(k);
                }

            });
        }


        int i = list.size();
        if (mutableint.intValue() != 0 && i != 0) {
            if (i == 1) {
                list.get(0).createItemStack(singleWrapper, context);
            } else {

                for (LootPoolEntry entry : list) {
                    float chance = (float)entry.getWeight(context.getLuck()) / mutableint.intValue();
                    Consumer<ItemStack> chanceWrapper = (stack) -> {
                        int amount = (int)(stack.getCount() * chance * multiplier);
                        // integer division
                        int completeStacks = amount / stack.getMaxStackSize();
                        if (completeStacks > 0) {
                            for (int g = 0; g < completeStacks; g++) consumer.accept(stack.copyWithCount(stack.getMaxStackSize()));
                        }
                        int leftover = amount - completeStacks * stack.getMaxStackSize();
                        if (leftover > 0) {
                            consumer.accept(stack.copyWithCount(leftover));
                        }
                    };
                    entry.createItemStack(chanceWrapper, context);
                }
                // Code from the original function
                /*
                int j = randomsource.nextInt(mutableint.intValue());

                for(LootPoolEntry entry : list) {
                    j -= entry.getWeight(context.getLuck());
                    if (j < 0) {
                        entry.createItemStack(chanceWrapper, context);
                        return;
                    }
                }
                 */

            }
        }
    }


    @Override
    protected void saveAdditional(@NotNull CompoundTag bigTag) {
        super.saveAdditional(bigTag);
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE_KEY, getOccupant());
        tag.putLong(COUNT_KEY, population);
        bigTag.put(CONTAINED_KEY, tag);
    }

    @Override
    public void load(@NotNull CompoundTag bigTag) {
        super.load(bigTag);
        CompoundTag tag = bigTag.getCompound(CONTAINED_KEY);
        population = tag.getLong(COUNT_KEY);
        occupant = tag.getString(TYPE_KEY);
    }

    public void grow() {
        grow(1);
    }
    public void grow(int amount) {
        population += amount;
        population = (long)Math.floor(population * 1.02);
    }
}
