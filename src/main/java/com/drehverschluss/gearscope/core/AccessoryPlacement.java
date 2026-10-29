package com.drehverschluss.gearscope.core;

import javax.annotation.Nullable;

/**
 * Where an Accessories slot is drawn on the Gearscope screen. Either at a named place (body or sidebar),
 * or as entry {@code tabPosition} of the group tab {@code tab}. Functional and cosmetic slots of the same
 * slot share one placement.
 */
public record AccessoryPlacement(int x, int y, @Nullable String place, @Nullable String tab, int tabPosition) {
    public static final AccessoryPlacement UNPLACED = new AccessoryPlacement(0, 0, null, null, 0);

    public boolean isPlaced() {
        return this.place != null;
    }

    public boolean isInTab() {
        return this.tab != null;
    }
}
