package com.drehverschluss.gearscope.core;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

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

    // Attribute scroll area (text area of the scroll; background is still a placeholder until Phase 4).
    public static final int SCROLL_X1 = 315;
    public static final int SCROLL_Y1 = 27;
    public static final int SCROLL_X2 = 449;
    public static final int SCROLL_Y2 = 233;
    // Inside it: a title row (title + "hide unchanged" checkbox), then the scrolling list.
    public static final int ATTR_TITLE_Y = SCROLL_Y1;
    public static final int ATTR_CHECKBOX_SIZE = 8;
    public static final int ATTR_LIST_Y = SCROLL_Y1 + 13;
    public static final int ATTR_SCROLLBAR_WIDTH = 3;
    public static final int ATTR_HEADER_HEIGHT = 11;
    public static final int ATTR_ROW_HEIGHT = 10;
    public static final int ATTR_GROUP_GAP = 3;
    public static final int ATTR_ICON_SIZE = 9;

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

    /** A named fixed position for an accessory slot (top-left of the 16x16 item area). */
    public record Place(String name, int x, int y) {
    }

    // Named accessory places, in shift-click priority order: body places first, then the sidebars.
    // Which slot type goes where comes from assets/gearscope/gearscope/slot_places.json.
    public static final List<Place> PLACES = List.of(
            new Place("body_left_top", 105, 42),
            new Place("body_right_top", 179, 42),
            new Place("body_left_mid", 105, 64),
            new Place("body_right_mid", 179, 64),
            new Place("body_left_low", 105, 86),
            new Place("body_right_low", 179, 86),
            new Place("strip_left_6", 27, 114),
            new Place("strip_right_1", 256, 24),
            new Place("strip_right_2", 256, 42),
            new Place("strip_right_3", 256, 60),
            new Place("strip_right_4", 256, 78),
            new Place("strip_right_5", 256, 96),
            new Place("strip_right_6", 256, 114));

    private static final Map<String, Place> PLACES_BY_NAME =
            PLACES.stream().collect(Collectors.toUnmodifiableMap(Place::name, Function.identity()));

    @Nullable
    public static Place place(String name) {
        return PLACES_BY_NAME.get(name);
    }

    // Group tabs: vertical bookmarks left of the inventory.
    public static final int TAB_X = 25;
    public static final int TAB_Y = 169;
    public static final int TAB_STEP = 13;
    public static final int TAB_WIDTH = 11;
    public static final int TAB_HEIGHT = 12;
    public static final int TABS_VISIBLE = 5;
    // Arrows to scroll the tab column when there are more than TABS_VISIBLE tabs.
    public static final int TAB_SCROLL_UP_Y = 160;
    public static final int TAB_SCROLL_DOWN_Y = TAB_Y + TABS_VISIBLE * TAB_STEP;
    public static final int ARROW_WIDTH = 11;
    public static final int ARROW_HEIGHT = 7;

    // Slots of the selected tab: 2 columns x 4 rows per page.
    public static final int[] TAB_SLOT_X = {38, 56};
    public static final int TAB_SLOT_Y = 165;
    public static final int TAB_SLOT_STEP = 18;
    public static final int TAB_SLOT_ROWS = 4;
    public static final int TAB_SLOTS_PER_PAGE = TAB_SLOT_X.length * TAB_SLOT_ROWS;
    // Page arrows below the tab slots when a tab has more than TAB_SLOTS_PER_PAGE slots.
    public static final int PAGE_PREV_X = 38;
    public static final int PAGE_NEXT_X = 62;
    public static final int PAGE_ARROW_Y = TAB_SLOT_Y + TAB_SLOT_ROWS * TAB_SLOT_STEP + 1;

    // Cosmetic toggle, top left of the niche.
    public static final int COSMETIC_BUTTON_X = 62;
    public static final int COSMETIC_BUTTON_Y = 20;
    public static final int COSMETIC_BUTTON_SIZE = 12;
}
