package com.drehverschluss.gearscope.compat.jei;

import com.drehverschluss.gearscope.Gearscope;
import com.drehverschluss.gearscope.client.GearscopeScreen;
import com.drehverschluss.gearscope.core.GearscopeLayout;
import java.util.Collection;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;

/**
 * JEI integration (only loaded by JEI itself, through the {@link JeiPlugin} annotation). JEI already keeps
 * its overlay clear of the screen area given by leftPos/topPos/imageWidth/imageHeight, which covers all of
 * Gearscope, so no extra exclusion areas are reported.
 */
@JeiPlugin
public class GearscopeJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = Gearscope.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Clicking the small arrow between grid and result shows the crafting recipes. Read from the
        // screen's layout each time, so a reloaded layout.json moves the area too.
        registration.addGuiContainerHandler(GearscopeScreen.class, new IGuiContainerHandler<GearscopeScreen>() {
            @Override
            public Collection<IGuiClickableArea> getGuiClickableAreas(GearscopeScreen screen, double guiMouseX, double guiMouseY) {
                GearscopeLayout layout = screen.layout();
                return List.of(IGuiClickableArea.createBasic(layout.resultX + 3, layout.craftArrowY - 1, 11, 6,
                        RecipeTypes.CRAFTING));
            }
        });
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new GearscopeCraftingTransferHandler(registration.getTransferHelper()),
                RecipeTypes.CRAFTING);
    }
}
