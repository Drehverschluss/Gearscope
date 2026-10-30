package com.drehverschluss.gearscope.compat.betterparty;

import com.drehverschluss.gearscope.client.GearscopeModButtons;
import com.drehverschluss.gearscope.client.GearscopeReturn;
import com.drehverschluss.gearscope.client.GearscopeScreen;
import dev.betterparty.client.PartyHudSettings;
import dev.betterparty.client.PartyScreen;
import dev.betterparty.network.PartyActionPayload;
import dev.betterparty.network.PartyNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Better Party integration, client side only. Better Party adds its party button to the vanilla inventory only;
 * this adds the same button to Gearscope's mod button row. It does what Better Party's own button does (request a
 * sync, open the party screen), uses Better Party's own textures and tooltip, and follows its "show inventory
 * button" and icon settings. Closing the party screen (and its sub screens) returns to Gearscope.
 */
public final class BetterPartyCompat {
    public static final String MOD_ID = "better_party";

    // Better Party's button textures and the cut-out of the motif it draws (PartyInventoryButton, 1.1.7).
    private static final ResourceLocation ADVENTURERS_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/party_button_adventurers.png");
    private static final ResourceLocation GREEN_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/party_button_green.png");
    private static final int TEXTURE_SIZE = 1254;
    private static final int MOTIF_X = 270;
    private static final int MOTIF_Y = 300;
    private static final int MOTIF_WIDTH = 714;
    private static final int MOTIF_HEIGHT = 624;
    private static final String SCREEN_PACKAGE = "dev.betterparty.client.";

    private BetterPartyCompat() {
    }

    public static void initClient() {
        GearscopeModButtons.register(new PartyButton());
        // The party screen and every screen it opens (settings, invitations, confirmations) live in this package.
        GearscopeReturn.registerChildScreens(BetterPartyCompat::isPartyScreen);
    }

    private static boolean isPartyScreen(Screen screen) {
        return screen.getClass().getName().startsWith(SCREEN_PACKAGE);
    }

    private static final class PartyButton implements GearscopeModButtons.ModButton {
        @Override
        public boolean visible() {
            return PartyHudSettings.showInventoryButton();
        }

        @Override
        public void renderIcon(GuiGraphics guiGraphics, int x, int y, int size) {
            ResourceLocation texture = PartyHudSettings.inventoryButtonIcon() == PartyHudSettings.InventoryButtonIcon.ADVENTURERS
                    ? ADVENTURERS_ICON : GREEN_ICON;
            // Keep the motif's aspect ratio inside the square button.
            int height = Math.max(1, Math.round(size * (float) MOTIF_HEIGHT / MOTIF_WIDTH));
            guiGraphics.blit(texture, x, y + (size - height) / 2, size, height, MOTIF_X, MOTIF_Y, MOTIF_WIDTH, MOTIF_HEIGHT,
                    TEXTURE_SIZE, TEXTURE_SIZE);
        }

        @Override
        public Component tooltip() {
            return Component.translatable("better_party.inventory.tooltip");
        }

        @Override
        public void onClick(GearscopeScreen screen) {
            Minecraft minecraft = Minecraft.getInstance();
            // Close the Gearscope menu properly first (crafting grid and cursor go back to the inventory), then do
            // exactly what Better Party's inventory button does.
            GearscopeReturn.expect(BetterPartyCompat::isPartyScreen);
            if (minecraft.player != null) {
                minecraft.player.closeContainer();
            }
            PartyNetwork.sendToServer(new PartyActionPayload(PartyActionPayload.Action.REQUEST_SYNC, ""));
            minecraft.setScreen(new PartyScreen());
        }
    }
}
