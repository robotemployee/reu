package com.robotemployee.reu.recipes;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public class InfusedEggRecipe {
    private final ItemStack egg = new ItemStack(RobotEmployeeUtils.INFUSED_EGG_ITEM.get());;

    private final ItemStack foodItem;
    private final EntityType<?> entityType;

    public InfusedEggRecipe(EntityType<?> entityType) {
        // TODO: make this be a different food item for each entity
        // make it data-driven :pleading:
        foodItem = new ItemStack(Items.BEETROOT_SEEDS);
        this.entityType = entityType;
        CompoundTag tag = new CompoundTag();


        this.egg.setTag(tag);
    }

    public ItemStack getInfusedEggItem() {
        return egg;
    }

    public EntityType<?> getEntityType() {
        return entityType;
    }

    public ItemStack getFoodItem() {
        return foodItem;
    }
}
