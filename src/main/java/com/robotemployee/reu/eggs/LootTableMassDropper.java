package com.robotemployee.reu.eggs;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.mixin.base.loot.condition.*;
import com.robotemployee.reu.mixin.base.loot.entry.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;
import org.slf4j.Logger;

import java.util.*;

public class LootTableMassDropper {

    static Logger LOGGER = LogUtils.getLogger();

    public static HashMap<String, DynamicLootLambda> dynamicLootLambdas;

    public static LootTable getLootTable(ResourceLocation location, LootDataManager lootDataManager) {
        Optional<LootTable> tableOptional = lootDataManager.getElementOptional(LootDataType.TABLE, location);
        return tableOptional.orElse(null);
    }

    public static ObjectArrayList<ItemStack> simulateDrops(LootTable table, LootParams params, LootContext context, Level level, long multiplier) {
        ObjectArrayList<ItemStack> result = new ObjectArrayList<>();
        result.addAll(processLootTable(table, params, context, level, multiplier));

        // global loot tables stuff
        net.minecraftforge.common.ForgeHooks.modifyLoot(table.getLootTableId(), result, context);
        return result;
    }

    public static ObjectArrayList<ItemStack> processLootTable(LootTable table, LootParams params, LootContext context, Level level, long multiplier) {
        ObjectArrayList<ItemStack> result = new ObjectArrayList<ItemStack>();
        List<LootPool> pools = ((LootTableAccessor)table).getPools();
        for (LootPool pool : pools) {
            result.addAll(processLootPool(pool, context, params, level, multiplier));
        }
        return result;
    }

    public static ObjectArrayList<ItemStack> processLootPool(LootPool pool, LootContext context, LootParams params, Level level, long multiplier) {
        ObjectArrayList<ItemStack> results = new ObjectArrayList<ItemStack>();
        long newMultiplier = (long)(pool.getRolls().getFloat(context) * multiplier);
        LootPoolEntryContainer[] entries = ((LootPoolAccessor)pool).getEntries();
        for (LootPoolEntryContainer container : entries) {
            results.addAll(processLootPoolContainer(container, context, params, newMultiplier, level));
        }
        return results;
    }

    public final float COPE_ASSUMING_DROP_AMOUNT = 0.5f;
    // recursive
    public static ObjectArrayList<ItemStack> processLootPoolContainer(LootPoolEntryContainer container, LootContext context, LootParams params, long multiplier, Level level) {

        /*
        Empty - DONE , UNTESTED
        LootItem - DONE , UNTESTED
        LootReference - DONE , UNTESTED
        DynamicLoot - MEH GOOD ENOUGH , UNTESTED
        TagEntry - DONE , UNTESTED
        AlternativesEntry - DONE , UNTESTED
        SequentialEntry - DONE , UNTESTED
        EntryGroup
         */

        ObjectArrayList<ItemStack> result = new ObjectArrayList<ItemStack>();


        if (container instanceof LootItem) {
            Item item = ((LootItemAccessor)container).getItem();
            result.addAll(getItemsMultiplied(new ItemStack(item), multiplier));

        } else if (container instanceof LootTableReference) {
            ResourceLocation loc = ((LootTableReferenceAccessor)container).getResourceLocation();
            LootTable table = getLootTable(loc, level.getServer().getLootData());
            // if this is null, then i u uyhj jk;lfjklfdjkl;dfkl;jkla;sdfjklsdfjkl
            if (table != null) result.addAll(processLootTable(table, params, context, level, multiplier));

        } else if (container instanceof DynamicLoot) {
            if (dynamicLootLambdas.isEmpty()) dynamicLootLambdas = createDynamicLootLambdas();
            ResourceLocation loc = ((DynamicLootAccessor)container).getResourceLocation();
            // I LOVE RAIN WORLD I LOVE RAIN WORLD I LOVE RAIN WORLD I LOVE RAIN WORLD
            DynamicLootLambda daddyLongLegs = dynamicLootLambdas.get(loc.toString());
            if (daddyLongLegs != null) {
                result.addAll(daddyLongLegs.apply(context, params, multiplier, level));
            } else {
                LOGGER.info("Infused Egg encountered DynamicLoot with no custom defined behavior; skipping entry");
            }

        } else if (container instanceof TagEntry) {
            TagKey<Item> tag = ((TagEntryAccessor)container).getTag();
            ITag<Item> itemsInTag = ForgeRegistries.ITEMS.tags().getTag(tag);
            // if tags are null, then you have a big problem

            if (itemsInTag.size() > 0) {
                itemsInTag.stream().forEach(item -> {
                    result.addAll(getItemsMultiplied(new ItemStack(item), multiplier / itemsInTag.size()));
                });
            }

        } else if (container instanceof AlternativesEntry) {
            LootPoolEntryContainer[] children = ((CompositeEntryBaseAccessor)container).getChildren();

            for (LootPoolEntryContainer child : children) {
                LootItemCondition[] conditions = ((LootPoolEntryContainerAccessor)child).getConditions();

                double chance = getANDChanceFromConditions(conditions, context);
                if (chance > 0) {
                    result.addAll(processLootPoolContainer(child, context, params, (long)Math.ceil(multiplier * chance), level));
                } else break;
            }

        } else if (container instanceof SequentialEntry) {
            LootPoolEntryContainer[] children = ((CompositeEntryBaseAccessor)container).getChildren();

            for (LootPoolEntryContainer child : children) {
                LootItemCondition[] conditions = ((LootPoolEntryContainerAccessor)child).getConditions();

                double chance = getANDChanceFromConditions(conditions, context);
                if (chance > 0) {
                    result.addAll(processLootPoolContainer(child, context, params, (long)Math.ceil(multiplier * chance), level));
                }
            }
        } else if (container instanceof EntryGroup) {
            LootPoolEntryContainer[] children = ((CompositeEntryBaseAccessor)container).getChildren();
            LootItemCondition[] conditions = ((LootPoolEntryContainerAccessor)container).getConditions();

            double chance = getANDChanceFromConditions(conditions, context);
            if (chance > 0) {
                for (LootPoolEntryContainer child : children) {
                    result.addAll(processLootPoolContainer(child, context, params, (long)Math.ceil(multiplier * chance), level));
                }
            }
        }

        return result;
    }

    public static double getANDChanceFromConditions(LootItemCondition[] conditions, LootContext context) {
        double chance = 1.0;

        for (LootItemCondition condition : conditions) {
            double newChance = getChanceFromCondition(condition, context);
            chance *= newChance;
            if (!(chance > 0)) return 0;
        }

        return chance;
    }

    public static double getORChanceFromConditions(LootItemCondition[] conditions, LootContext context) {
        double antiChance = 1.0;
        // the chance for it to not return
        // because OR only needs one to activate,
        // we can find the chance by first getting
        // the chance for nothing to happen and inverting it.

        for(LootItemCondition condition : conditions) {
            double chance = getChanceFromCondition(condition, context);
            // if it's guaranteed
            if (!(chance < 1)) return 1;
            antiChance *= (1 - chance);
        }

        return 1 - antiChance;
    }

    // recursive
    public static double getChanceFromCondition(LootItemCondition condition, LootContext context) {


        if (condition instanceof InvertedLootItemCondition) {
            LootItemCondition term = ((InvertedLootItemConditionAccessor)condition).getTerm();
            return 1 - getChanceFromCondition(term, context);

        } else if (condition instanceof AnyOfCondition) {
            LootItemCondition[] terms = ((CompositeLootItemConditionAccessor)condition).getTerms();
            return getORChanceFromConditions(terms, context);

        } else if (condition instanceof AllOfCondition) {
            LootItemCondition[] terms = ((CompositeLootItemConditionAccessor)condition).getTerms();
            return (getANDChanceFromConditions(terms, context));
        } else if (condition instanceof LootItemRandomChanceCondition) {
            return ((LootItemRandomChanceConditionAccessor)condition).getProbability();

        } else if (condition instanceof LootItemRandomChanceWithLootingCondition) {
            float probability = ((LootItemRandomChanceWithLootingConditionAccessor)condition).getProbability();
            float lootingMultiplier = ((LootItemRandomChanceWithLootingConditionAccessor)condition).getLootingMultiplier();
            int looting = context.getLootingModifier();
            return Math.min(1, probability + lootingMultiplier * looting);

        } else if (condition instanceof BonusLevelTableCondition) {
            Enchantment enchantment = ((BonusLevelTableConditionAccessor)condition).getEnchantment();
            float[] values = ((BonusLevelTableConditionAccessor)condition).getValues();

            ItemStack itemStack = context.getParamOrNull(LootContextParams.TOOL);
            int i = itemStack != null ? itemStack.getEnchantmentLevel(enchantment) : 0;
            if (values.length == 0) return 0;
            return values[Math.min(i, values.length - 1)];

        } else if (condition instanceof ExplosionCondition) {
            Float f = context.getParamOrNull(LootContextParams.EXPLOSION_RADIUS);
            return f != null ? Math.min(1.0, 1.0 / f) : 1.0;

        } else if (condition instanceof ConditionReference) {
            ResourceLocation loc = ((ConditionReferenceAccessor)condition).getResourceLocation();
            LootItemCondition term = context.getResolver().getElement(LootDataType.PREDICATE, loc);

            if (term == null) {
                LOGGER.warn("Tried using unknown condition table called {}", loc);
                return 0;
            } else {
                LootContext.VisitedEntry<?> visitedentry = LootContext.createVisitedEntry(term);
                if (context.pushVisitedElement(visitedentry)) {
                    double chance;
                    try {
                        chance = getChanceFromCondition(term, context);
                    } finally {
                        context.popVisitedElement(visitedentry);
                    }

                    return chance;
                } else {
                    LOGGER.warn("Detected infinite loop in loot tables");
                    return 0;
                }
            }
        }
        return condition.test(context) ? 1 : 0;
    }



    // this is separate mostly for easier ability to mixin into it or something i guess ?
    // like at the tail get the current return value map thing, add to it, then set it
    public static HashMap<String, DynamicLootLambda> createDynamicLootLambdas() {
        HashMap<String, DynamicLootLambda> bigResult = new HashMap<>();

        // built-in compatibility
        bigResult.put("minecraft:fish", (context, oldParams, multiplier, level) -> {
            ObjectArrayList<ItemStack> result = new ObjectArrayList<>();
            Vec3 position = oldParams.getParameter(LootContextParams.ORIGIN);
            Entity owner = oldParams.getParameter(LootContextParams.THIS_ENTITY);
            ItemStack tool = new ItemStack(Items.FISHING_ROD);

            LootParams params = (new LootParams.Builder((ServerLevel)level)).withParameter(LootContextParams.ORIGIN, position).withParameter(LootContextParams.TOOL, tool).withParameter(LootContextParams.THIS_ENTITY, owner).withParameter(LootContextParams.KILLER_ENTITY, owner).withLuck(1.0f).create(LootContextParamSets.FISHING);
            LootTable target = getLootTable(new ResourceLocation("minecraft:gameplay/fishing/fish"), level.getServer().getLootData());
            result.addAll(processLootTable(target, params, context, level, multiplier));
            target = getLootTable(new ResourceLocation("minecraft:gameplay/fishing/junk"), level.getServer().getLootData());
            result.addAll(processLootTable(target, params, context, level, multiplier));
            target = getLootTable(new ResourceLocation("minecraft:gameplay/fishing/treasure"), level.getServer().getLootData());
            result.addAll(processLootTable(target, params, context, level, multiplier));

            return result;
        });

        bigResult.put("minecraft:barter", (context, oldParams, multiplier, level) -> {
            ObjectArrayList<ItemStack> result = new ObjectArrayList<>();
            Entity owner = oldParams.getParameter(LootContextParams.THIS_ENTITY);

            LootParams params = (new LootParams.Builder((ServerLevel)level)).withParameter(LootContextParams.THIS_ENTITY, owner).create(LootContextParamSets.PIGLIN_BARTER);
            LootTable target = getLootTable(new ResourceLocation("minecraft:gameplay/piglin_bartering"), level.getServer().getLootData());
            result.addAll(processLootTable(target, params, context, level, multiplier));

            return result;
        });

        return bigResult;
    }

    public static ArrayList<ItemStack> getItemsMultiplied(ItemStack stack, long multiplier) {
        ArrayList<ItemStack> result = new ArrayList<>();
        long amount = stack.getCount() * multiplier;
        // integer division
        int completeStacks = (int) (amount / stack.getMaxStackSize());
        if (completeStacks > 0) {
            for (int i = 0; i < completeStacks; i++) result.add(stack.copyWithCount(stack.getMaxStackSize()));
        }
        int leftover = (int) (amount - completeStacks * stack.getMaxStackSize());
        if (leftover > 0) {
            result.add(stack.copyWithCount(leftover));
        }
        return result;
    }

    @FunctionalInterface
    public interface DynamicLootLambda {
        ObjectArrayList<ItemStack> apply(LootContext context, LootParams params, long multiplier, Level level);
    }
}
