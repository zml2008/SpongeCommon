/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.core.item.recipe.crafting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.common.interfaces.IMixinInventoryCrafting;

@Mixin(ShapedRecipes.class)
public abstract class MixinShapedRecipes implements IRecipe, ShapedCraftingRecipe {

    @Shadow @Final protected int recipeWidth;
    @Shadow @Final protected int recipeHeight;
    @Shadow @Final private NonNullList<Ingredient> recipeItems;

    @Override
    public org.spongepowered.api.item.recipe.crafting.Ingredient getIngredient(int x, int y) {
        if (x < 0 || x >= this.recipeWidth || y < 0 || y >= this.recipeHeight) {
            throw new IndexOutOfBoundsException("Invalid ingredient predicate location");
        }

        int recipeItemIndex = x + y * this.recipeWidth;
        return ((org.spongepowered.api.item.recipe.crafting.Ingredient)(Object) this.recipeItems.get(recipeItemIndex));
    }

    private int i;
    private int j;
    private InventoryCrafting craftingGrid;

    @Inject(method = "checkMatch", locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/crafting/Ingredient;apply(Lnet/minecraft/item/ItemStack;)Z"))
    private void beforeCheckMatch(InventoryCrafting craftingGrid, int p_77573_2_, int p_77573_3_, boolean p_77573_4_, CallbackInfoReturnable<Boolean> match,
            int i, int j, int k, int l, Ingredient ingredient) {
        this.i = i;
        this.j = j;
        this.craftingGrid = craftingGrid;
    }

    @Redirect(method = "checkMatch", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/crafting/Ingredient;apply(Lnet/minecraft/item/ItemStack;)Z"))
    private boolean onCheckMatch(Ingredient ingredient, ItemStack itemStack) {
        if (ingredient.apply(itemStack)) {
            ((IMixinInventoryCrafting) this.craftingGrid).matchedIngredient(this.i, this.j, ingredient);
            this.craftingGrid = null;
            return true;
        }
        ((IMixinInventoryCrafting) this.craftingGrid).resetIngredients();
        this.craftingGrid = null;
        return false;
    }

    @Override
    public int getWidth() {
        return this.recipeWidth;
    }

    @Override
    public int getHeight() {
        return this.recipeHeight;
    }

}
