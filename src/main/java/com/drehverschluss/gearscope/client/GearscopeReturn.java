package com.drehverschluss.gearscope.client;

import com.drehverschluss.gearscope.network.OpenGearscopePayload;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Reopens Gearscope when a screen that was opened from it is closed (a backpack, a shulker box, the party
 * screen ...). Such screens either belong to a registered family ({@link #registerChildScreens}, e.g. all
 * Sophisticated Backpacks screens, so switching between their sub screens keeps the return) or are announced
 * right before they are opened ({@link #expect}, for vanilla screen types that can also be opened elsewhere).
 * Gearscope is reopened only when the last of them closes to the game; switching to any other screen cancels it.
 */
public final class GearscopeReturn {
    private static final long EXPECT_TIMEOUT_MS = 3000;
    private static final List<Predicate<Screen>> CHILD_SCREENS = new CopyOnWriteArrayList<>();

    @Nullable
    private static Predicate<Screen> expected;
    private static long expectedUntil;
    // Set when the server closed Gearscope on its own (e.g. right before opening another menu with the usual close
    // packet): the next screen still counts as opened from Gearscope.
    private static long gearscopeClosedUntil;
    // The announced screen family that is currently open, in addition to the registered ones.
    @Nullable
    private static Predicate<Screen> activeExpected;
    private static boolean returnToGearscope;

    private GearscopeReturn() {
    }

    /** Screens that, when opened from Gearscope (or from another such screen), return to Gearscope on close. */
    public static void registerChildScreens(Predicate<Screen> screens) {
        CHILD_SCREENS.add(screens);
    }

    /**
     * Announces that the next screen matching {@code screens}, opened within a few seconds, comes from Gearscope
     * (used when Gearscope itself is closed first, or the screen type is a vanilla one).
     */
    public static void expect(Predicate<Screen> screens) {
        expected = screens;
        expectedUntil = System.currentTimeMillis() + EXPECT_TIMEOUT_MS;
    }

    /** Called when the Gearscope screen goes away without the player closing it (see GearscopeScreen#removed). */
    static void gearscopeClosedBySomethingElse() {
        gearscopeClosedUntil = System.currentTimeMillis() + EXPECT_TIMEOUT_MS;
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        Screen next = event.getNewScreen();
        // Some child screens close into the vanilla inventory themselves (e.g. Quark's held shulker box): while a
        // return is pending, Gearscope takes the inventory's place (reopened on the next tick).
        if (returnToGearscope && next instanceof InventoryScreen && !isChild(event.getCurrentScreen())) {
            event.setCanceled(true);
            return;
        }
        if (expected != null && System.currentTimeMillis() < expectedUntil && expected.test(next)) {
            activeExpected = expected;
            expected = null;
            gearscopeClosedUntil = 0;
            returnToGearscope = true;
            return;
        }
        boolean closedJustBefore = event.getCurrentScreen() == null && System.currentTimeMillis() < gearscopeClosedUntil;
        gearscopeClosedUntil = 0;
        boolean fromGearscope = event.getCurrentScreen() instanceof GearscopeScreen || returnToGearscope || closedJustBefore;
        returnToGearscope = fromGearscope && isChild(next);
        if (!returnToGearscope) {
            activeExpected = null;
        }
    }

    private static boolean isChild(@Nullable Screen screen) {
        if (screen == null) {
            return false;
        }
        if (activeExpected != null && activeExpected.test(screen)) {
            return true;
        }
        for (Predicate<Screen> family : CHILD_SCREENS) {
            if (family.test(screen)) {
                return true;
            }
        }
        return false;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!returnToGearscope) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            reset();
        } else if (minecraft.screen == null) {
            // The child screen was closed (Esc, inventory key, or the server closed it).
            reset();
            PacketDistributor.sendToServer(new OpenGearscopePayload());
        } else if (!isChild(minecraft.screen)) {
            reset();
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
        expected = null;
    }

    private static void reset() {
        returnToGearscope = false;
        activeExpected = null;
    }
}
