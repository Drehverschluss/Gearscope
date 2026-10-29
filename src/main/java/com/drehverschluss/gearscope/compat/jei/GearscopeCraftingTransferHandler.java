package com.drehverschluss.gearscope.compat.jei;

import com.drehverschluss.gearscope.menu.GearscopeMenu;
import com.drehverschluss.gearscope.menu.GearscopeMenuTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Moves crafting recipes into Gearscope's 2x2 grid, the same way JEI does it for the vanilla player
 * inventory: JEI lays out every crafting recipe on a 3x3 grid, so only recipes whose ingredients all sit in
 * its top-left 2x2 part (3x3 indices 0, 1, 3, 4) can be moved, and only those four views are passed on to
 * a basic transfer handler for grid slots 1-4 and inventory/hotbar slots 10-45.
 */
class GearscopeCraftingTransferHandler implements IRecipeTransferHandler<GearscopeMenu, RecipeHolder<CraftingRecipe>> {
    private static final int[] GRID_2X2_IN_3X3 = {0, 1, 3, 4};

    private final IRecipeTransferHandlerHelper helper;
    private final IRecipeTransferHandler<GearscopeMenu, RecipeHolder<CraftingRecipe>> basicHandler;

    GearscopeCraftingTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
        this.basicHandler = helper.createUnregisteredRecipeTransferHandler(helper.createBasicRecipeTransferInfo(
                GearscopeMenu.class, GearscopeMenuTypes.CHARACTER.get(), RecipeTypes.CRAFTING,
                GearscopeMenu.CRAFT_SLOT_START, GearscopeMenu.CRAFT_SLOT_COUNT,
                GearscopeMenu.INV_SLOT_START, GearscopeMenu.INV_SLOT_COUNT + GearscopeMenu.HOTBAR_SLOT_COUNT));
    }

    @Override
    public Class<? extends GearscopeMenu> getContainerClass() {
        return this.basicHandler.getContainerClass();
    }

    @Override
    public Optional<MenuType<GearscopeMenu>> getMenuType() {
        return this.basicHandler.getMenuType();
    }

    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    // JEI's own player inventory handler goes through this (deprecated) variant as well; the context
    // variant's default implementation delegates here.
    @SuppressWarnings("removal")
    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(GearscopeMenu container, RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        if (!this.helper.recipeTransferHasServerSupport()) {
            return this.helper.createUserErrorWithTooltip(Component.translatable("jei.tooltip.error.recipe.transfer.no.server"));
        }
        List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        for (int i = 0; i < inputs.size(); i++) {
            if (!isIn2x2(i) && !inputs.get(i).isEmpty()) {
                return this.helper.createUserErrorWithTooltip(
                        Component.translatable("jei.tooltip.error.recipe.transfer.too.large.player.inventory"));
            }
        }
        List<IRecipeSlotView> gridInputs = new ArrayList<>(GRID_2X2_IN_3X3.length);
        for (int index : GRID_2X2_IN_3X3) {
            if (index >= inputs.size()) {
                return this.helper.createInternalError();
            }
            gridInputs.add(inputs.get(index));
        }
        return this.basicHandler.transferRecipe(container, recipe, this.helper.createRecipeSlotsView(gridInputs),
                player, maxTransfer, doTransfer);
    }

    private static boolean isIn2x2(int index) {
        for (int gridIndex : GRID_2X2_IN_3X3) {
            if (gridIndex == index) {
                return true;
            }
        }
        return false;
    }
}
