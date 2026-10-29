package com.drehverschluss.gearscope.client;

import com.drehverschluss.gearscope.Gearscope;
import com.drehverschluss.gearscope.core.GearscopeLayout;
import com.drehverschluss.gearscope.menu.GearscopeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Phase 1 screen: placeholder panels instead of a background texture (added in Phase 4), simple frames
 * around every slot, and the real player figure. No Accessories slots or attribute list yet.
 */
public class GearscopeScreen extends AbstractContainerScreen<GearscopeMenu> {
    private static final int PLACEHOLDER_COLOR = 0xA0101018;
    private static final int SLOT_FRAME_COLOR = 0xFF3C3C3C;

    private static boolean warnedAboutSmallWindow = false;

    private float xMouse;
    private float yMouse;

    public GearscopeScreen(GearscopeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GearscopeLayout.SCREEN_WIDTH;
        this.imageHeight = GearscopeLayout.SCREEN_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        if (!warnedAboutSmallWindow && (this.width < this.imageWidth || this.height < this.imageHeight)) {
            warnedAboutSmallWindow = true;
            Gearscope.LOGGER.warn(
                    "Gearscope screen ({}x{}) does not fit the window ({}x{} GUI pixels); scaling for small windows is added in Phase 4.",
                    this.imageWidth, this.imageHeight, this.width, this.height);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title and "Inventory" labels are intentionally not drawn.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.xMouse = mouseX;
        this.yMouse = mouseY;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        // Placeholder panels for the areas from Gearscope-Konzept.md 3.2; replaced by real art in Phase 4.
        fillPlaceholder(guiGraphics, left, top, GearscopeLayout.SIDEBAR_LEFT_X1, GearscopeLayout.SIDEBAR_LEFT_Y1,
                GearscopeLayout.SIDEBAR_LEFT_X2, GearscopeLayout.SIDEBAR_LEFT_Y2);
        fillPlaceholder(guiGraphics, left, top, GearscopeLayout.SIDEBAR_RIGHT_X1, GearscopeLayout.SIDEBAR_RIGHT_Y1,
                GearscopeLayout.SIDEBAR_RIGHT_X2, GearscopeLayout.SIDEBAR_RIGHT_Y2);
        fillPlaceholder(guiGraphics, left, top, GearscopeLayout.NICHE_X1, GearscopeLayout.NICHE_Y1,
                GearscopeLayout.NICHE_X2, GearscopeLayout.NICHE_Y2);
        fillPlaceholder(guiGraphics, left, top, GearscopeLayout.LOWER_PANEL_X1, GearscopeLayout.LOWER_PANEL_Y1,
                GearscopeLayout.LOWER_PANEL_X2, GearscopeLayout.LOWER_PANEL_Y2);
        fillPlaceholder(guiGraphics, left, top, GearscopeLayout.SCROLL_X1, GearscopeLayout.SCROLL_Y1,
                GearscopeLayout.SCROLL_X2, GearscopeLayout.SCROLL_Y2);

        // Simple frame around every slot so the layout can be judged without real slot art.
        for (Slot slot : this.menu.slots) {
            guiGraphics.renderOutline(left + slot.x - 1, top + slot.y - 1, 18, 18, SLOT_FRAME_COLOR);
        }

        // Player figure, rendered above the niche placeholder, follows the mouse like the vanilla inventory.
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics,
                left + GearscopeLayout.PLAYER_RENDER_X1, top + GearscopeLayout.PLAYER_RENDER_Y1,
                left + GearscopeLayout.PLAYER_RENDER_X2, top + GearscopeLayout.PLAYER_RENDER_Y2,
                GearscopeLayout.PLAYER_RENDER_SCALE, GearscopeLayout.PLAYER_RENDER_Y_OFFSET,
                this.xMouse, this.yMouse, this.minecraft.player);
    }

    private static void fillPlaceholder(GuiGraphics guiGraphics, int left, int top, int x1, int y1, int x2, int y2) {
        guiGraphics.fill(left + x1, top + y1, left + x2, top + y2, PLACEHOLDER_COLOR);
    }
}
