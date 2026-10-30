package com.drehverschluss.gearscope.compat.quark;

import com.drehverschluss.gearscope.core.GearscopeServices;
import com.drehverschluss.gearscope.menu.GearscopeMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.quark.base.handler.SimilarBlockTypeHandler;
import org.violetmoon.quark.content.management.inventory.HeldShulkerBoxContainer;
import org.violetmoon.quark.content.management.module.ExpandedItemInteractionsModule;

/**
 * Quark integration. Quark's Expanded Item Interactions open a shulker box in place only in the vanilla inventory
 * (its check requires InventoryMenu). For Gearscope's inventory and hotbar slots this does the same with Quark's
 * own container and the same conditions (module enabled, shulker box interaction and allowOpeningShulkerBoxes on,
 * the click Quark uses, empty cursor, a single shulker box that may be picked up). Quark's other interactions
 * (putting items into shulker boxes, swapping armor) already work in Gearscope and are left alone.
 */
public final class QuarkCompat {
    public static final String MOD_ID = "quark";

    private QuarkCompat() {
    }

    /** Both sides. */
    public static void init() {
        // Quark's interactions use the right click, or the left click with its invertClicks option.
        GearscopeServices.setOpenItemButton(() -> ExpandedItemInteractionsModule.invertClicks ? 0 : 1);
        GearscopeServices.addSlotClickHandler(QuarkCompat::openShulkerBox);
    }

    private static boolean openShulkerBox(AbstractContainerMenu menu, Slot slot, int button, Player player) {
        if (!(menu instanceof GearscopeMenu)
                || slot.index < GearscopeMenu.INV_SLOT_START || slot.index >= GearscopeMenu.HOTBAR_SLOT_END
                || slot.container != player.getInventory()
                || button != GearscopeServices.openItemButton().getAsInt()
                || !Quark.ZETA.modules.isEnabled(ExpandedItemInteractionsModule.class)
                || !ExpandedItemInteractionsModule.enableShulkerBoxInteraction
                || !ExpandedItemInteractionsModule.allowOpeningShulkerBoxes
                || !menu.getCarried().isEmpty()) {
            return false;
        }
        ItemStack stack = slot.getItem();
        if (stack.getCount() != 1 || !SimilarBlockTypeHandler.isShulkerBox(stack) || !slot.mayPickup(player)) {
            return false;
        }
        // Same as Quark's ExpandedItemInteractionsModule: the server opens its held shulker box menu for this
        // inventory slot, the client plays the sound.
        int inventorySlot = slot.getSlotIndex();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new HeldShulkerBoxContainer(serverPlayer, inventorySlot), buffer -> buffer.writeInt(inventorySlot));
        } else {
            player.playSound(SoundEvents.SHULKER_BOX_OPEN, 1.0F, 1.0F);
        }
        return true;
    }
}
