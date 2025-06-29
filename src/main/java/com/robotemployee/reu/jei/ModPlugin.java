package com.robotemployee.reu.jei;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.recipes.InfusedEggRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;

@JeiPlugin
public class ModPlugin implements IModPlugin {

    public static final RecipeType<InfusedEggRecipe> INFUSED_EGG_TYPE = new RecipeType<>(new ResourceLocation(RobotEmployeeUtils.MODID, "egg_infusion"), InfusedEggRecipe.class);

    Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation resourceLocation = new ResourceLocation(RobotEmployeeUtils.MODID, "jei_plugin");
    @Override
    @NotNull
    public ResourceLocation getPluginUid() {
        return resourceLocation;
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        ArrayList<InfusedEggRecipe> recipes = new ArrayList<>();
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            if (type.getCategory() != MobCategory.MISC && type.canSummon()) {
                recipes.add(new InfusedEggRecipe(type));
            }
        }

        LOGGER.info("Registering " + recipes.size() + " fake egg recipes");
        registration.addRecipes(INFUSED_EGG_TYPE, recipes);
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        LOGGER.info("Registering the egg infusion JEI category");
        registration.addRecipeCategories(new EggInfusionJEICategory(registration.getJeiHelpers().getGuiHelper()));
    }
}
