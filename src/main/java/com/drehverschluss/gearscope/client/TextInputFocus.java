package com.drehverschluss.gearscope.client;

import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

/**
 * Whether a screen currently takes text input, so a key that closes screens must be left to that input.
 * Vanilla only checks this per screen (anvil, creative search); for any screen this looks at the focus chain
 * and, like JEI, at every active and focused {@link EditBox} among the children. Text widgets of other mods that
 * are no EditBox (e.g. Sophisticated Core's TextBox) are recognised by their class name.
 */
final class TextInputFocus {
    private static final Pattern TEXT_WIDGET_NAME = Pattern.compile("(?i)(text|edit|search)(box|field|input|area)");
    private static final int MAX_DEPTH = 16;

    private TextInputFocus() {
    }

    static boolean isTextInputFocused(Screen screen) {
        GuiEventListener focused = screen.getFocused();
        for (int depth = 0; focused != null && depth < MAX_DEPTH; depth++) {
            if (focused instanceof EditBox || focused instanceof MultiLineEditBox
                    || TEXT_WIDGET_NAME.matcher(focused.getClass().getSimpleName()).find()) {
                return true;
            }
            focused = focused instanceof ContainerEventHandler container ? container.getFocused() : null;
        }
        return hasFocusedEditBox(screen.children(), 0);
    }

    private static boolean hasFocusedEditBox(List<? extends GuiEventListener> children, int depth) {
        if (depth >= MAX_DEPTH) {
            return false;
        }
        for (GuiEventListener child : children) {
            if (child instanceof EditBox editBox && editBox.isActive() && editBox.isFocused()) {
                return true;
            }
            if (child instanceof ContainerEventHandler container && hasFocusedEditBox(container.children(), depth + 1)) {
                return true;
            }
        }
        return false;
    }
}
