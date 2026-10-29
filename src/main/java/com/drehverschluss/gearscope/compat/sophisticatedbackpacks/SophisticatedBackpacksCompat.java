package com.drehverschluss.gearscope.compat.sophisticatedbackpacks;

import com.drehverschluss.gearscope.client.GearscopeScreen;
import com.drehverschluss.gearscope.menu.GearscopeMenu;
import com.drehverschluss.gearscope.network.OpenGearscopePayload;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.client.KeybindHandler;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.IContextAwareContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;

/**
 * Sophisticated Backpacks integration, client side only.
 * <ul>
 * <li>SB's own "open backpack" key already works on Gearscope's inventory, hotbar, chest and offhand slots
 * (it only looks at slots of the player {@link Inventory}). This adds the same for Accessories slots
 * (SB's "accessories" inventory handler) and opens a backpack on right-click with an empty cursor. Both
 * send SB's own {@link BackpackOpenPayload} with the values SB itself would use.</li>
 * <li>SB only returns to the vanilla inventory when a backpack screen is closed. A backpack opened from
 * Gearscope reopens Gearscope instead when its screen is closed (not when SB switches to its settings or a
 * nested backpack, which are SB screens as well).</li>
 * </ul>
 */
public final class SophisticatedBackpacksCompat {
    public static final String MOD_ID = "sophisticatedbackpacks";
    private static final String ACCESSORIES_HANDLER = "accessories";

    // Set while an SB screen opened from Gearscope (directly or through SB sub screens) is shown.
    private static boolean returnToGearscope;

    private SophisticatedBackpacksCompat() {
    }

    public static void initClient() {
        NeoForge.EVENT_BUS.addListener(SophisticatedBackpacksCompat::onMouseButtonPressed);
        NeoForge.EVENT_BUS.addListener(SophisticatedBackpacksCompat::onKeyPressed);
        NeoForge.EVENT_BUS.addListener(SophisticatedBackpacksCompat::onScreenOpening);
        NeoForge.EVENT_BUS.addListener(SophisticatedBackpacksCompat::onClientTick);
        NeoForge.EVENT_BUS.addListener(SophisticatedBackpacksCompat::onLoggingOut);
    }

    // --- Opening a backpack from Gearscope ---

    private record OpenTarget(int slotIndex, String identifier, String handlerName) {
    }

    private static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof GearscopeScreen screen)) {
            return;
        }
        boolean plainRightClick = event.getButton() == InputConstants.MOUSE_BUTTON_RIGHT
                && !Screen.hasShiftDown() && screen.getMenu().getCarried().isEmpty();
        // SB's key bound to a mouse button: SB itself already handled player inventory slots.
        boolean openKey = KeybindHandler.BACKPACK_OPEN_KEYBIND.isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(event.getButton()));
        if ((plainRightClick || openKey) && tryOpen(screen, plainRightClick)) {
            event.setCanceled(true);
        }
    }

    private static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof GearscopeScreen screen
                && KeybindHandler.BACKPACK_OPEN_KEYBIND.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))
                && tryOpen(screen, false)) {
            event.setCanceled(true);
        }
    }

    /**
     * Opens the backpack under the mouse. Player inventory slots are only handled for right-click; SB's key
     * handler (higher priority) already covers them for its key.
     */
    private static boolean tryOpen(GearscopeScreen screen, boolean includeInventorySlots) {
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null || !(slot.getItem().getItem() instanceof BackpackItem)) {
            return false;
        }
        Optional<OpenTarget> target = slot.container instanceof Inventory
                ? (includeInventorySlots ? inventoryTarget(slot) : Optional.empty())
                : accessoryTarget(screen.getMenu(), slot);
        target.ifPresent(t -> PacketDistributor.sendToServer(new BackpackOpenPayload(t.slotIndex(), t.identifier(), t.handlerName())));
        return target.isPresent();
    }

    /** Same mapping as SB's KeybindHandler: main inventory/hotbar, chest armor slot or offhand. */
    private static Optional<OpenTarget> inventoryTarget(Slot slot) {
        int index = slot.getSlotIndex();
        String handlerName;
        if (index == 38) {
            handlerName = "armor";
        } else if (index == Inventory.SLOT_OFFHAND) {
            handlerName = "offhand";
        } else if (index >= 0 && index < Inventory.INVENTORY_SIZE) {
            handlerName = "main";
        } else {
            return Optional.empty();
        }
        return Optional.of(new OpenTarget(index, "", handlerName));
    }

    /** SB's "accessories" handler: identifier is the Accessories slot name, index the index in it. */
    private static Optional<OpenTarget> accessoryTarget(GearscopeMenu menu, Slot slot) {
        for (GearscopeMenu.AccessoryEntry entry : menu.accessoryEntries()) {
            if (entry.slot() == slot) {
                // SB only looks at the functional stacks, never at cosmetic ones.
                return entry.meta().cosmetic() ? Optional.empty()
                        : Optional.of(new OpenTarget(entry.meta().index(), entry.meta().slotName(), ACCESSORIES_HANDLER));
            }
        }
        return Optional.empty();
    }

    // --- Returning to Gearscope ---

    private static void onScreenOpening(ScreenEvent.Opening event) {
        boolean fromGearscopeOrSb = event.getCurrentScreen() instanceof GearscopeScreen || returnToGearscope;
        returnToGearscope = fromGearscopeOrSb && isSbScreen(event.getNewScreen());
    }

    private static boolean isSbScreen(@Nullable Screen screen) {
        return screen instanceof AbstractContainerScreen<?> containerScreen
                && containerScreen.getMenu() instanceof IContextAwareContainer;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (!returnToGearscope) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            returnToGearscope = false;
        } else if (minecraft.screen == null) {
            // The backpack screen was closed (ESC, inventory key, or the server closed it).
            returnToGearscope = false;
            PacketDistributor.sendToServer(new OpenGearscopePayload());
        } else if (!isSbScreen(minecraft.screen)) {
            returnToGearscope = false;
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        returnToGearscope = false;
    }
}
