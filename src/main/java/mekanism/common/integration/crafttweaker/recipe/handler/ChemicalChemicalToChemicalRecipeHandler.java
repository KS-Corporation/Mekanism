package mekanism.common.integration.crafttweaker.recipe.handler;

import com.blamejared.crafttweaker.api.managers.IRecipeManager;
import com.blamejared.crafttweaker.api.recipes.IRecipeHandler;
import mekanism.api.recipes.ChemicalInfuserRecipe;
import mekanism.api.recipes.PigmentMixingRecipe;
import mekanism.api.recipes.chemical.ChemicalChemicalToChemicalRecipe;
import net.minecraft.item.crafting.IRecipe;

public abstract class ChemicalChemicalToChemicalRecipeHandler<RECIPE extends ChemicalChemicalToChemicalRecipe<?, ?, ?>> extends MekanismRecipeHandler<RECIPE> {

    @Override
    public String dumpToCommandString(IRecipeManager manager, RECIPE recipe) {
        return buildCommandString(manager, recipe, recipe.getLeftInput(), recipe.getRightInput(), recipe.getOutputDefinition());
    }

    @Override
    public <U extends IRecipe<?>> boolean doesConflict(IRecipeManager manager, RECIPE recipe, U other) {
        //Only support if the other is a chemical chemical to chemical recipe and don't bother checking the reverse as the recipe type's generics
        // ensures that it is of the same type
        if (recipeIsInstance(other)) {
            ChemicalChemicalToChemicalRecipe<?, ?, ?> otherRecipe = (ChemicalChemicalToChemicalRecipe<?, ?, ?>) other;
            return (chemicalIngredientConflicts(recipe.getLeftInput(), otherRecipe.getLeftInput()) &&
                    chemicalIngredientConflicts(recipe.getRightInput(), otherRecipe.getRightInput())) ||
                   (chemicalIngredientConflicts(recipe.getLeftInput(), otherRecipe.getRightInput()) &&
                    chemicalIngredientConflicts(recipe.getRightInput(), otherRecipe.getLeftInput()));
        }
        return false;
    }

    /**
     * @return if the other recipe the correct class type.
     */
    protected abstract boolean recipeIsInstance(IRecipe<?> other);

    @IRecipeHandler.For(ChemicalInfuserRecipe.class)
    public static class ChemicalInfuserRecipeHandler extends ChemicalChemicalToChemicalRecipeHandler<ChemicalInfuserRecipe> {

        @Override
        protected boolean recipeIsInstance(IRecipe<?> other) {
            return other instanceof ChemicalInfuserRecipe;
        }
    }

    @IRecipeHandler.For(PigmentMixingRecipe.class)
    public static class PigmentMixingRecipeHandler extends ChemicalChemicalToChemicalRecipeHandler<PigmentMixingRecipe> {

        @Override
        protected boolean recipeIsInstance(IRecipe<?> other) {
            return other instanceof PigmentMixingRecipe;
        }
    }
}