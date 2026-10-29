package com.drehverschluss.gearscope.client;

import com.drehverschluss.gearscope.Gearscope;
import net.minecraft.resources.ResourceLocation;

/**
 * Textures and colors of the Gearscope screen (wood/leather/parchment style), all in one place. The textures
 * can be replaced by resource packs.
 */
public final class GearscopeStyle {
    // 18x18 pixel-art slot frames (semi-transparent), drawn 1:1 without filtering. The background texture
    // and its size are part of layout.json.
    public static final ResourceLocation SLOT = Gearscope.id("textures/gui/slot.png");
    public static final ResourceLocation SLOT_COSMETIC = Gearscope.id("textures/gui/slot_cosmetic.png");
    public static final int SLOT_TEXTURE_SIZE = 18;

    // Group tabs (bookmarks left of the inventory).
    public static final int TAB_FILL = 0xF0261A10;
    public static final int TAB_BORDER = 0xFF82643E;
    public static final int TAB_SELECTED_FILL = 0xFF5C3E22;
    public static final int TAB_SELECTED_BORDER = 0xFFEBC373;
    public static final int TAB_HOVER_FILL = 0xF03A2818;

    // Small gold arrows (tab/page scrolling, crafting).
    public static final int ARROW = 0xFFDCBE78;
    public static final int ARROW_HOVER = 0xFFF5DCA0;
    public static final int ARROW_DISABLED = 0xFF5A4A36;

    // Cosmetic toggle.
    public static final int COSMETIC_FILL = 0xFF28120C;
    public static final int COSMETIC_BORDER = 0xFFCDA55A;
    public static final int COSMETIC_ACTIVE_FILL = 0xFF3C2418;
    public static final int COSMETIC_ACTIVE_BORDER = 0xFFB48CDC;

    // Attribute list on the parchment.
    public static final int ATTR_TEXT = 0xFF3A2414;
    public static final int ATTR_HEADER = 0xFF6E1C18;
    public static final int ATTR_BETTER = 0xFF1E641E;
    public static final int ATTR_WORSE = 0xFFA01E1E;
    public static final int ATTR_ROW_HOVER = 0x18000000;
    public static final int ATTR_SCROLLBAR_TRACK = 0xFFD8C8A4;
    public static final int ATTR_SCROLLBAR_THUMB = 0xFF6E4A2A;
    public static final int ATTR_CHECKBOX = 0xFF6E1C18;

    private GearscopeStyle() {
    }
}
