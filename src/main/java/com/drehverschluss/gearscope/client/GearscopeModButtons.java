package com.drehverschluss.gearscope.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Buttons of other mods on the Gearscope screen, in a row next to the cosmetic button (layout.json
 * "mod_buttons"). A compatibility adds one entry; it is only shown while {@link ModButton#visible()} is true.
 */
public final class GearscopeModButtons {
    private static final List<ModButton> BUTTONS = new CopyOnWriteArrayList<>();

    private GearscopeModButtons() {
    }

    public interface ModButton {
        /** Whether the button is shown right now (e.g. the other mod's own "show button" setting). */
        boolean visible();

        /** Draws the icon inside the button area (frame and hover are drawn by Gearscope). */
        void renderIcon(GuiGraphics guiGraphics, int x, int y, int size);

        Component tooltip();

        /** Called on a left click; Gearscope plays the click sound. */
        void onClick(GearscopeScreen screen);
    }

    public static void register(ModButton button) {
        BUTTONS.add(button);
    }

    /** The buttons to show, in registration order. */
    static List<ModButton> visible() {
        List<ModButton> visible = new ArrayList<>();
        for (ModButton button : BUTTONS) {
            if (button.visible()) {
                visible.add(button);
            }
        }
        return visible;
    }
}
