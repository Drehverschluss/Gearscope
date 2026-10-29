package com.drehverschluss.gearscope.compat.polymorph;

import com.drehverschluss.gearscope.client.GearscopeScreen;
import com.drehverschluss.gearscope.core.GearscopeLayout;
import com.illusivesoulworks.polymorph.api.client.widgets.PlayerRecipesWidget;
import net.minecraft.world.inventory.Slot;

/**
 * Polymorph's player crafting widget, with the selection button placed by Gearscope's layout instead of
 * Polymorph's default (22 px above the result slot, which would cover the 2x2 grid).
 */
class GearscopeRecipesWidget extends PlayerRecipesWidget {
    private final GearscopeLayout layout;

    GearscopeRecipesWidget(GearscopeScreen screen, Slot resultSlot) {
        super(screen, resultSlot);
        this.layout = screen.layout();
    }

    @Override
    public void initChildWidgets() {
        super.initChildWidgets();
        this.openButton.setWidth(this.layout.recipeButtonSize);
        this.openButton.setHeight(this.layout.recipeButtonSize);
    }

    // Both relative to the screen's left/top; Polymorph adds the screen position itself.
    @Override
    public int getXPos() {
        return this.layout.recipeButtonX;
    }

    @Override
    public int getYPos() {
        return this.layout.recipeButtonY;
    }
}
