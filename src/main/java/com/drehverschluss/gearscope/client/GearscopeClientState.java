package com.drehverschluss.gearscope.client;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Screen state that survives closing, reopening and server-side reopens of the Gearscope screen. Kept in
 * memory only.
 */
final class GearscopeClientState {
    @Nullable
    static String selectedTab;
    static int tabScroll;
    static boolean cosmeticMode;
    static final Map<String, Integer> TAB_PAGES = new HashMap<>();

    private GearscopeClientState() {
    }
}
