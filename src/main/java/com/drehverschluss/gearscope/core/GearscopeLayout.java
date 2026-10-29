package com.drehverschluss.gearscope.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * All positions of the Gearscope screen in GUI pixels, relative to its top-left corner. The client loads them
 * from assets/gearscope/gearscope/layout.json (resource packs can override it, see docs/layout.md); every
 * value missing there falls back to the built-in default below. The server only ever uses the defaults, slot
 * positions do not matter there.
 */
public final class GearscopeLayout {
    /** A named fixed position for an accessory slot (top-left of the 16x16 item area). */
    public record Place(String name, int x, int y) {
    }

    /** Mutable values used to build a layout; starts with the defaults. */
    public static final class Builder {
        public int screenWidth = 480;
        public int screenHeight = 270;

        public ResourceLocation backgroundTexture = ResourceLocation.fromNamespaceAndPath("gearscope", "textures/gui/background.png");
        public int backgroundTextureWidth = 1440;
        public int backgroundTextureHeight = 810;

        public int armorX = 27;
        public int[] armorY = {24, 42, 60, 78};
        public int offhandX = 27;
        public int offhandY = 96;

        public int inventoryX = 77;
        public int[] inventoryRowY = {163, 181, 199};
        public int slotStep = 18;
        public int hotbarY = 221;

        public int[] craftX = {243, 261};
        public int[] craftY = {171, 189};
        public int resultX = 252;
        public int resultY = 213;
        public int craftArrowY = 207;

        // Shift-click priority follows this order: body places first, then the sidebars.
        public final LinkedHashMap<String, int[]> places = new LinkedHashMap<>();

        public int tabX = 25;
        public int tabY = 169;
        public int tabStep = 13;
        public int tabWidth = 11;
        public int tabHeight = 12;
        public int tabsVisible = 5;
        public int tabScrollUpY = 160;
        public int arrowWidth = 11;
        public int arrowHeight = 7;
        public int[] tabSlotX = {38, 56};
        public int tabSlotY = 165;
        public int tabSlotStep = 18;
        public int tabSlotRows = 4;
        public int pagePrevX = 38;
        public int pageNextX = 62;

        public int cosmeticButtonX = 62;
        public int cosmeticButtonY = 20;
        public int cosmeticButtonSize = 12;

        // Recipe selection button of Polymorph (only shown when it is installed and a recipe is ambiguous).
        public int recipeButtonX = 239;
        public int recipeButtonY = 215;
        public int recipeButtonSize = 12;

        // Player figure: horizontal center, where its feet stand, render scale and the clipping box around it.
        public int playerCenterX = 151;
        public int playerFeetY = 126;
        public int playerScale = 40;
        public int playerBoxWidth = 80;
        public int playerBoxHeight = 100;
        public float playerYOffset = 0.0625F;

        public int scrollX1 = 315;
        public int scrollY1 = 27;
        public int scrollX2 = 449;
        public int scrollY2 = 233;
        public int attrListY = 40;
        public int attrCheckboxSize = 8;
        public int attrScrollbarWidth = 3;
        public int attrHeaderHeight = 11;
        public int attrRowHeight = 10;
        public int attrGroupGap = 3;
        public int attrIconSize = 9;

        public Builder() {
            // Body places row by row (top, mid, waist, low), each from the outer left to the outer right.
            this.places.put("body_left_outer_top", new int[] {81, 52});
            this.places.put("body_left_top", new int[] {103, 52});
            this.places.put("body_right_top", new int[] {183, 52});
            this.places.put("body_right_outer_top", new int[] {205, 52});
            this.places.put("body_left_outer_mid", new int[] {81, 72});
            this.places.put("body_left_mid", new int[] {103, 72});
            this.places.put("body_right_mid", new int[] {183, 72});
            this.places.put("body_right_outer_mid", new int[] {205, 72});
            this.places.put("body_left_outer_waist", new int[] {81, 92});
            this.places.put("body_left_waist", new int[] {103, 92});
            this.places.put("body_right_waist", new int[] {183, 92});
            this.places.put("body_right_outer_waist", new int[] {205, 92});
            this.places.put("body_left_outer_low", new int[] {81, 112});
            this.places.put("body_left_low", new int[] {103, 112});
            this.places.put("body_right_low", new int[] {183, 112});
            this.places.put("body_right_outer_low", new int[] {205, 112});
            this.places.put("strip_left_6", new int[] {27, 114});
            this.places.put("strip_right_1", new int[] {256, 24});
            this.places.put("strip_right_2", new int[] {256, 42});
            this.places.put("strip_right_3", new int[] {256, 60});
            this.places.put("strip_right_4", new int[] {256, 78});
            this.places.put("strip_right_5", new int[] {256, 96});
            this.places.put("strip_right_6", new int[] {256, 114});
        }

        public GearscopeLayout build() {
            return new GearscopeLayout(this);
        }
    }

    public static final GearscopeLayout DEFAULT = new Builder().build();

    private static volatile GearscopeLayout current = DEFAULT;

    /** The active layout (the client's layout.json, otherwise the defaults). */
    public static GearscopeLayout current() {
        return current;
    }

    public static void setCurrent(GearscopeLayout layout) {
        current = layout;
    }

    public final int screenWidth;
    public final int screenHeight;
    public final ResourceLocation backgroundTexture;
    public final int backgroundTextureWidth;
    public final int backgroundTextureHeight;
    public final int armorX;
    public final int[] armorY;
    public final int offhandX;
    public final int offhandY;
    public final int inventoryX;
    public final int[] inventoryRowY;
    public final int slotStep;
    public final int hotbarY;
    public final int[] craftX;
    public final int[] craftY;
    public final int resultX;
    public final int resultY;
    public final int craftArrowY;
    public final List<Place> places;
    private final Map<String, Place> placesByName;
    public final int tabX;
    public final int tabY;
    public final int tabStep;
    public final int tabWidth;
    public final int tabHeight;
    public final int tabsVisible;
    public final int tabScrollUpY;
    public final int tabScrollDownY;
    public final int arrowWidth;
    public final int arrowHeight;
    public final int[] tabSlotX;
    public final int tabSlotY;
    public final int tabSlotStep;
    public final int tabSlotRows;
    public final int tabSlotsPerPage;
    public final int pagePrevX;
    public final int pageNextX;
    public final int pageArrowY;
    public final int cosmeticButtonX;
    public final int cosmeticButtonY;
    public final int cosmeticButtonSize;
    public final int recipeButtonX;
    public final int recipeButtonY;
    public final int recipeButtonSize;
    public final int playerCenterX;
    public final int playerFeetY;
    public final int playerScale;
    public final int playerBoxWidth;
    public final int playerBoxHeight;
    public final float playerYOffset;
    public final int scrollX1;
    public final int scrollY1;
    public final int scrollX2;
    public final int scrollY2;
    public final int attrTitleY;
    public final int attrListY;
    public final int attrCheckboxSize;
    public final int attrScrollbarWidth;
    public final int attrHeaderHeight;
    public final int attrRowHeight;
    public final int attrGroupGap;
    public final int attrIconSize;

    private GearscopeLayout(Builder b) {
        this.screenWidth = b.screenWidth;
        this.screenHeight = b.screenHeight;
        this.backgroundTexture = b.backgroundTexture;
        this.backgroundTextureWidth = b.backgroundTextureWidth;
        this.backgroundTextureHeight = b.backgroundTextureHeight;
        this.armorX = b.armorX;
        this.armorY = b.armorY.clone();
        this.offhandX = b.offhandX;
        this.offhandY = b.offhandY;
        this.inventoryX = b.inventoryX;
        this.inventoryRowY = b.inventoryRowY.clone();
        this.slotStep = b.slotStep;
        this.hotbarY = b.hotbarY;
        this.craftX = b.craftX.clone();
        this.craftY = b.craftY.clone();
        this.resultX = b.resultX;
        this.resultY = b.resultY;
        this.craftArrowY = b.craftArrowY;

        List<Place> placeList = new ArrayList<>();
        Map<String, Place> byName = new LinkedHashMap<>();
        b.places.forEach((name, position) -> {
            Place place = new Place(name, position[0], position[1]);
            placeList.add(place);
            byName.put(name, place);
        });
        this.places = List.copyOf(placeList);
        this.placesByName = Map.copyOf(byName);

        this.tabX = b.tabX;
        this.tabY = b.tabY;
        this.tabStep = b.tabStep;
        this.tabWidth = b.tabWidth;
        this.tabHeight = b.tabHeight;
        this.tabsVisible = b.tabsVisible;
        this.tabScrollUpY = b.tabScrollUpY;
        this.tabScrollDownY = b.tabY + b.tabsVisible * b.tabStep;
        this.arrowWidth = b.arrowWidth;
        this.arrowHeight = b.arrowHeight;
        this.tabSlotX = b.tabSlotX.clone();
        this.tabSlotY = b.tabSlotY;
        this.tabSlotStep = b.tabSlotStep;
        this.tabSlotRows = b.tabSlotRows;
        this.tabSlotsPerPage = b.tabSlotX.length * b.tabSlotRows;
        this.pagePrevX = b.pagePrevX;
        this.pageNextX = b.pageNextX;
        this.pageArrowY = b.tabSlotY + b.tabSlotRows * b.tabSlotStep + 1;
        this.cosmeticButtonX = b.cosmeticButtonX;
        this.cosmeticButtonY = b.cosmeticButtonY;
        this.cosmeticButtonSize = b.cosmeticButtonSize;
        this.recipeButtonX = b.recipeButtonX;
        this.recipeButtonY = b.recipeButtonY;
        this.recipeButtonSize = b.recipeButtonSize;
        this.playerCenterX = b.playerCenterX;
        this.playerFeetY = b.playerFeetY;
        this.playerScale = b.playerScale;
        this.playerBoxWidth = b.playerBoxWidth;
        this.playerBoxHeight = b.playerBoxHeight;
        this.playerYOffset = b.playerYOffset;
        this.scrollX1 = b.scrollX1;
        this.scrollY1 = b.scrollY1;
        this.scrollX2 = b.scrollX2;
        this.scrollY2 = b.scrollY2;
        this.attrTitleY = b.scrollY1;
        this.attrListY = b.attrListY;
        this.attrCheckboxSize = b.attrCheckboxSize;
        this.attrScrollbarWidth = b.attrScrollbarWidth;
        this.attrHeaderHeight = b.attrHeaderHeight;
        this.attrRowHeight = b.attrRowHeight;
        this.attrGroupGap = b.attrGroupGap;
        this.attrIconSize = b.attrIconSize;
    }

    @Nullable
    public Place place(String name) {
        return this.placesByName.get(name);
    }

    /**
     * Vertical center of the player render box. InventoryScreen draws the entity relative to the box center
     * with its feet at center + scale * (0.9 + yOffset) for a standing player (half of 1.8 blocks), so the
     * center follows from where the feet should stand.
     */
    public float playerCenterY() {
        return this.playerFeetY - this.playerScale * (0.9F + this.playerYOffset);
    }

    public int playerBoxX1() {
        return this.playerCenterX - this.playerBoxWidth / 2;
    }

    public int playerBoxY1() {
        return Math.round(this.playerCenterY() - this.playerBoxHeight / 2.0F);
    }
}
