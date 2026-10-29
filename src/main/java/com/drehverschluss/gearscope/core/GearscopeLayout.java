package com.drehverschluss.gearscope.core;

/**
 * Pixel layout constants for the Gearscope screen (480x270 GUI pixels), taken from Gearscope-Konzept.md
 * section 3.2. This is the single source of truth for slot and placeholder positions in Phase 1; Phase 4
 * will move these values into a resourcepack layout.json instead.
 */
public final class GearscopeLayout {
    private GearscopeLayout() {
    }

    public static final int SCREEN_WIDTH = 480;
    public static final int SCREEN_HEIGHT = 270;

    // Left/right sidebar placeholder areas (6 slots tall, only the left one is used in Phase 1 for
    // armor + offhand; the rest is reserved for Accessories in Phase 2).
    public static final int SIDEBAR_SLOT_SIZE = 18;
    public static final int SIDEBAR_SLOT_COUNT = 6;
    public static final int SIDEBAR_TOP_Y = 24;
    public static final int SIDEBAR_LEFT_X = 27;
    public static final int SIDEBAR_RIGHT_X = 256;
    private static final int SIDEBAR_HEIGHT = SIDEBAR_SLOT_COUNT * SIDEBAR_SLOT_SIZE;
    public static final int SIDEBAR_LEFT_X1 = SIDEBAR_LEFT_X - 8;
    public static final int SIDEBAR_LEFT_Y1 = SIDEBAR_TOP_Y - 8;
    public static final int SIDEBAR_LEFT_X2 = SIDEBAR_LEFT_X + SIDEBAR_SLOT_SIZE + 8;
    public static final int SIDEBAR_LEFT_Y2 = SIDEBAR_TOP_Y + SIDEBAR_HEIGHT + 8;
    public static final int SIDEBAR_RIGHT_X1 = SIDEBAR_RIGHT_X - 8;
    public static final int SIDEBAR_RIGHT_Y1 = SIDEBAR_TOP_Y - 8;
    public static final int SIDEBAR_RIGHT_X2 = SIDEBAR_RIGHT_X + SIDEBAR_SLOT_SIZE + 8;
    public static final int SIDEBAR_RIGHT_Y2 = SIDEBAR_TOP_Y + SIDEBAR_HEIGHT + 8;

    // Niche (stage) the player figure stands in.
    public static final int NICHE_X1 = 55;
    public static final int NICHE_Y1 = 13;
    public static final int NICHE_X2 = 247;
    public static final int NICHE_Y2 = 143;

    // Lower panel (inventory, hotbar, crafting).
    public static final int LOWER_PANEL_X1 = 22;
    public static final int LOWER_PANEL_Y1 = 157;
    public static final int LOWER_PANEL_X2 = 282;
    public static final int LOWER_PANEL_Y2 = 246;

    // Attribute scroll area (empty placeholder in Phase 1, filled in Phase 3).
    public static final int SCROLL_X1 = 315;
    public static final int SCROLL_Y1 = 27;
    public static final int SCROLL_X2 = 449;
    public static final int SCROLL_Y2 = 233;

    // Armor (head, chest, legs, feet, top to bottom) and offhand, left sidebar.
    public static final int ARMOR_X = 27;
    public static final int[] ARMOR_Y = {24, 42, 60, 78};
    public static final int OFFHAND_X = 27;
    public static final int OFFHAND_Y = 96;

    // Inventory (3x9) and hotbar.
    public static final int INV_X = 77;
    public static final int INV_SLOT_SIZE = 18;
    public static final int[] INV_ROW_Y = {163, 181, 199};
    public static final int HOTBAR_Y = 221;

    // 2x2 crafting grid and its result slot.
    public static final int[] CRAFT_X = {243, 261};
    public static final int[] CRAFT_Y = {171, 189};
    public static final int RESULT_X = 252;
    public static final int RESULT_Y = 213;

    // Player figure in the niche: a fixed-size box passed to
    // InventoryScreen.renderEntityInInventoryFollowsMouse. Center x=151, feet at approx. y=122.
    // Starting values only; fine-tuning happens in-game (Phase 4 adds screen scaling for small windows).
    public static final int PLAYER_RENDER_X1 = 116;
    public static final int PLAYER_RENDER_Y1 = 22;
    public static final int PLAYER_RENDER_X2 = 186;
    public static final int PLAYER_RENDER_Y2 = 122;
    public static final int PLAYER_RENDER_SCALE = 30;
    public static final float PLAYER_RENDER_Y_OFFSET = 0.0625F;
}
