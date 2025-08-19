package com.robotemployee.reu.mixin.create;

import com.robotemployee.reu.registry.ModItems;
import com.robotemployee.reu.item.FryingPanItem;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RecipeApplier.class)
public class RecipeApplierMixin {
    @Inject(method = "applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;)Ljava/util/List;", at = @At("TAIL"), cancellable = true, remap = false)
    private static void applyRecipeOn(Level level, ItemStack stackIn, Recipe<?> recipe, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!(recipe instanceof PressingRecipe)) return;
        List<ItemStack> results = cir.getReturnValue();
        if (results.isEmpty()) return;
        ItemStack output = cir.getReturnValue().get(0);
        if (output.getItem() != ModItems.FRYING_PAN.get()) return;
        FryingPanItem.saveTo(output, stackIn);
    }
}
