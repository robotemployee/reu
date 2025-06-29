package com.robotemployee.reu.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.recipes.InfusedEggRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;

public class EggInfusionJEICategory implements IRecipeCategory<InfusedEggRecipe> {

    private final IDrawable icon;

    public EggInfusionJEICategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(RobotEmployeeUtils.INFUSED_EGG_ITEM.get()));
    }

    @Override
    @NotNull
    public RecipeType<InfusedEggRecipe> getRecipeType() {
        return ModPlugin.INFUSED_EGG_TYPE;
    }

    @Override
    @NotNull
    public Component getTitle() {
        return Component.translatable("gui.jei.category.reu.egg_infusion");
    }

    @Override
    @Nullable
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayoutBuilder builder, @NotNull InfusedEggRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 100, 0)
                .addItemStack(recipe.getInfusedEggItem());
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 0)
                .addItemStack(recipe.getFoodItem());
        builder.addSlot(RecipeIngredientRole.INPUT, 40, 0)
                .addItemStack(new ItemStack(Items.SNIFFER_EGG));
    }

    @Override
    public int getHeight() {
        return 70;
    }

    @Override
    public int getWidth() {
        return 160;
    }

    @Override
    public void draw(@NotNull InfusedEggRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // death
        EntityType<?> type = recipe.getEntityType();
        Minecraft instance = Minecraft.getInstance();
        Level level = instance.level;
        if (level == null) return;
        Entity entity = type.create(level);
        if (entity == null) return;
        entity.tickCount = 1;

        Vector2i position = new Vector2i(80, 30);
        float scale = 20f;

        // from -1 to 1
        // for making the mob follow your mouse a little
        float mouseXRelShrunk = 2 * (float)mouseX / guiGraphics.guiWidth() - 1;
        float mouseYRelShrunk = 2 * (float)mouseY / guiGraphics.guiHeight() - 1;

        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();
        poseStack.translate(position.x, position.y, 50);
        poseStack.scale(scale, -scale, scale);
        //poseStack.mulPose(Axis.YP.rotationDegrees(-mouseXRelShrunk * 15));
        //poseStack.mulPose(Axis.XP.rotationDegrees(-mouseYRelShrunk * 15));
        //poseStack.mulPose(Axis.XP.rotationDegrees(15));

        EntityRenderDispatcher dispatcher = instance.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        try {
            // entity, x, y, z, yaw, partialTicks, poseStack, multiBufferSource, packedLight
            dispatcher.render(entity, 0, 0, 0, 0, 0, poseStack, instance.renderBuffers().bufferSource(), 15728880);
        } catch (Exception ignored) {}
        dispatcher.setRenderShadow(true);

        poseStack.popPose();
    }
}
