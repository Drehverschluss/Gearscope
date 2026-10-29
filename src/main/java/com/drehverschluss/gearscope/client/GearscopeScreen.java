package com.drehverschluss.gearscope.client;

import com.drehverschluss.gearscope.Gearscope;
import com.drehverschluss.gearscope.client.attributes.AttributeListPanel;
import com.drehverschluss.gearscope.core.AccessoryPlacement;
import com.drehverschluss.gearscope.core.AccessorySlotSpec;
import com.drehverschluss.gearscope.core.GearscopeLayout;
import com.drehverschluss.gearscope.core.GearscopeServices;
import com.drehverschluss.gearscope.core.ToggleableSlot;
import com.drehverschluss.gearscope.menu.GearscopeMenu;
import com.drehverschluss.gearscope.network.AccessoryQuickMoveOrderPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Gearscope screen: placeholder panels instead of a background texture (added in Phase 4), simple frames
 * around every visible slot, the real player figure, and the Accessories slots (body places, sidebars,
 * group tabs, cosmetic toggle). Visibility of accessory slots is decided here, on the client only.
 */
public class GearscopeScreen extends AbstractContainerScreen<GearscopeMenu> {
    private static final ResourceLocation COSMETIC_ICON =
            ResourceLocation.fromNamespaceAndPath("accessories", "textures/gui/theme/cosmetic_rainbow_icon.png");
    private static final ResourceLocation TAB_UNKNOWN_ICON = Gearscope.id("textures/gui/tab_unknown.png");

    private enum Arrow { UP, DOWN, LEFT, RIGHT }

    private static boolean warnedAboutSmallWindow = false;

    // Tabs (Accessories groups) that have at least one slot without a fixed place, in display order.
    private final List<String> tabs = new ArrayList<>();
    private final Map<String, Integer> tabSlotCounts = new HashMap<>();
    private final Map<String, ResourceLocation> tabIcons = new HashMap<>();
    private final boolean hasAccessories;
    private final AttributeListPanel attributePanel = new AttributeListPanel();

    private float xMouse;
    private float yMouse;

    public GearscopeScreen(GearscopeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GearscopeLayout.SCREEN_WIDTH;
        this.imageHeight = GearscopeLayout.SCREEN_HEIGHT;
        this.hasAccessories = !menu.accessoryEntries().isEmpty();

        for (GearscopeMenu.AccessoryEntry entry : menu.accessoryEntries()) {
            AccessoryPlacement placement = entry.placement();
            if (!entry.meta().cosmetic() && placement.isInTab()) {
                if (!this.tabSlotCounts.containsKey(placement.tab())) {
                    this.tabs.add(placement.tab());
                }
                this.tabSlotCounts.merge(placement.tab(), 1, Integer::sum);
            }
            if (entry.slot() instanceof ToggleableSlot toggleable) {
                toggleable.gearscope$setVisibility(() -> this.isAccessoryVisible(entry));
            }
        }
        // Cosmetic armor replaces the vanilla armor slots at the same positions while cosmetic mode is on.
        if (!menu.cosmeticArmorSlots().isEmpty()) {
            for (Slot slot : menu.armorSlots()) {
                if (slot instanceof ToggleableSlot toggleable) {
                    toggleable.gearscope$setVisibility(() -> !GearscopeClientState.cosmeticMode);
                }
            }
            for (Slot slot : menu.cosmeticArmorSlots()) {
                if (slot instanceof ToggleableSlot toggleable) {
                    toggleable.gearscope$setVisibility(() -> GearscopeClientState.cosmeticMode);
                }
            }
        }
        GearscopeClientState.tabScroll = Mth.clamp(GearscopeClientState.tabScroll, 0, this.maxTabScroll());
        this.sendAccessoryQuickMoveOrder();
    }

    @Override
    protected void init() {
        super.init();
        if (!warnedAboutSmallWindow && (this.width < this.imageWidth || this.height < this.imageHeight)) {
            warnedAboutSmallWindow = true;
            Gearscope.LOGGER.warn(
                    "Gearscope screen ({}x{}) does not fit the window ({}x{} GUI pixels); scaling for small windows is added in Phase 4.",
                    this.imageWidth, this.imageHeight, this.width, this.height);
        }
    }

    // --- Accessory slot visibility ---

    private boolean isAccessoryVisible(GearscopeMenu.AccessoryEntry entry) {
        if (entry.meta().cosmetic() != GearscopeClientState.cosmeticMode) {
            return false;
        }
        AccessoryPlacement placement = entry.placement();
        if (placement.isPlaced()) {
            return true;
        }
        if (placement.isInTab()) {
            return placement.tab().equals(this.currentTab())
                    && placement.tabPosition() / GearscopeLayout.TAB_SLOTS_PER_PAGE == this.currentPage(placement.tab());
        }
        return false;
    }

    @Nullable
    private String currentTab() {
        String selected = GearscopeClientState.selectedTab;
        if (selected != null && this.tabSlotCounts.containsKey(selected)) {
            return selected;
        }
        return this.tabs.isEmpty() ? null : this.tabs.get(0);
    }

    private int pageCount(String tab) {
        int count = this.tabSlotCounts.getOrDefault(tab, 0);
        return Math.max(1, Mth.positiveCeilDiv(count, GearscopeLayout.TAB_SLOTS_PER_PAGE));
    }

    private int currentPage(String tab) {
        return Mth.clamp(GearscopeClientState.TAB_PAGES.getOrDefault(tab, 0), 0, this.pageCount(tab) - 1);
    }

    private int maxTabScroll() {
        return Math.max(0, this.tabs.size() - GearscopeLayout.TABS_VISIBLE);
    }

    /**
     * Shift-click into accessories tries body places first, then the sidebars, then the tabs. Only the
     * client knows the layout, so it tells the server the order.
     */
    private void sendAccessoryQuickMoveOrder() {
        int[] order = this.menu.accessoryEntries().stream()
                .filter(entry -> !entry.meta().cosmetic())
                .sorted(Comparator.comparingLong(this::quickMovePriority))
                .mapToInt(entry -> entry.slot().index)
                .toArray();
        if (order.length == 0) {
            return;
        }
        this.menu.setAccessoryQuickMoveOrder(order);
        PacketDistributor.sendToServer(new AccessoryQuickMoveOrderPayload(this.menu.containerId, order));
    }

    private long quickMovePriority(GearscopeMenu.AccessoryEntry entry) {
        AccessoryPlacement placement = entry.placement();
        if (placement.isPlaced()) {
            return GearscopeLayout.PLACES.indexOf(GearscopeLayout.place(placement.place()));
        }
        if (placement.isInTab()) {
            return 1_000_000L + this.tabs.indexOf(placement.tab()) * 100_000L + placement.tabPosition();
        }
        return Long.MAX_VALUE;
    }

    // --- Rendering ---

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title and "Inventory" labels are intentionally not drawn.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        this.renderWidgetTooltips(guiGraphics, mouseX, mouseY);
        if (this.hoveredSlot == null && this.menu.getCarried().isEmpty()) {
            this.attributePanel.renderTooltip(guiGraphics, this.leftPos, this.topPos, mouseX, mouseY);
        }
        this.xMouse = mouseX;
        this.yMouse = mouseY;
    }

    @Override
    protected void containerTick() {
        this.attributePanel.tick();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        // Painted background at 3x resolution, scaled down to 480x270; linear filtering suits a painting.
        this.minecraft.getTextureManager().getTexture(GearscopeStyle.BACKGROUND).setFilter(true, false);
        guiGraphics.blit(GearscopeStyle.BACKGROUND, left, top, this.imageWidth, this.imageHeight, 0, 0,
                GearscopeStyle.BACKGROUND_TEXTURE_WIDTH, GearscopeStyle.BACKGROUND_TEXTURE_HEIGHT,
                GearscopeStyle.BACKGROUND_TEXTURE_WIDTH, GearscopeStyle.BACKGROUND_TEXTURE_HEIGHT);
        this.attributePanel.render(guiGraphics, left, top, mouseX, mouseY);
        this.renderCraftingArrow(guiGraphics);

        // Player figure; Accessories draws equipped (or cosmetic) accessories on it through its own render layer.
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics,
                left + GearscopeLayout.PLAYER_RENDER_X1, top + GearscopeLayout.PLAYER_RENDER_Y1,
                left + GearscopeLayout.PLAYER_RENDER_X2, top + GearscopeLayout.PLAYER_RENDER_Y2,
                GearscopeLayout.PLAYER_RENDER_SCALE, GearscopeLayout.PLAYER_RENDER_Y_OFFSET,
                this.xMouse, this.yMouse, this.minecraft.player);

        // Frame under every visible slot (none for empty places); violet for cosmetic slots.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int frameSize = GearscopeStyle.SLOT_TEXTURE_SIZE;
        for (Slot slot : this.menu.slots) {
            if (slot.isActive()) {
                ResourceLocation frame = slot instanceof ToggleableSlot toggleable && toggleable.gearscope$isCosmetic()
                        ? GearscopeStyle.SLOT_COSMETIC : GearscopeStyle.SLOT;
                guiGraphics.blit(frame, left + slot.x - 1, top + slot.y - 1, 0, 0, frameSize, frameSize, frameSize, frameSize);
            }
        }
        RenderSystem.disableBlend();

        if (this.hasAccessories) {
            this.renderCosmeticButton(guiGraphics, mouseX, mouseY);
            this.renderTabs(guiGraphics, mouseX, mouseY);
        }
    }

    /** Small gold arrow from the 2x2 grid down to the result slot. */
    private void renderCraftingArrow(GuiGraphics guiGraphics) {
        int centerX = this.leftPos + GearscopeLayout.RESULT_X + 8;
        int y = this.topPos + GearscopeLayout.CRAFT_ARROW_Y;
        for (int row = 0; row < 4; row++) {
            guiGraphics.fill(centerX - 3 + row, y + row, centerX + 4 - row, y + row + 1, GearscopeStyle.ARROW);
        }
    }

    private void renderCosmeticButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos + GearscopeLayout.COSMETIC_BUTTON_X;
        int y = this.topPos + GearscopeLayout.COSMETIC_BUTTON_Y;
        int size = GearscopeLayout.COSMETIC_BUTTON_SIZE;
        boolean active = GearscopeClientState.cosmeticMode;
        guiGraphics.fill(x, y, x + size, y + size, active ? GearscopeStyle.COSMETIC_ACTIVE_FILL : GearscopeStyle.COSMETIC_FILL);
        guiGraphics.renderOutline(x, y, size, size, active ? GearscopeStyle.COSMETIC_ACTIVE_BORDER : GearscopeStyle.COSMETIC_BORDER);
        guiGraphics.blit(COSMETIC_ICON, x + 1, y + 1, size - 2, size - 2, 0, 0, 32, 32, 32, 32);
    }

    private void renderTabs(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String currentTab = this.currentTab();
        if (currentTab == null) {
            return;
        }
        int scroll = GearscopeClientState.tabScroll;
        int visible = Math.min(GearscopeLayout.TABS_VISIBLE, this.tabs.size() - scroll);
        for (int i = 0; i < visible; i++) {
            String tab = this.tabs.get(scroll + i);
            int relY = GearscopeLayout.TAB_Y + i * GearscopeLayout.TAB_STEP;
            int x = this.leftPos + GearscopeLayout.TAB_X;
            int y = this.topPos + relY;
            boolean selected = tab.equals(currentTab);
            boolean hovered = this.isOver(mouseX, mouseY, GearscopeLayout.TAB_X, relY, GearscopeLayout.TAB_WIDTH, GearscopeLayout.TAB_HEIGHT);
            int fill = selected ? GearscopeStyle.TAB_SELECTED_FILL : hovered ? GearscopeStyle.TAB_HOVER_FILL : GearscopeStyle.TAB_FILL;
            guiGraphics.fill(x, y, x + GearscopeLayout.TAB_WIDTH, y + GearscopeLayout.TAB_HEIGHT, fill);
            guiGraphics.renderOutline(x, y, GearscopeLayout.TAB_WIDTH, GearscopeLayout.TAB_HEIGHT,
                    selected ? GearscopeStyle.TAB_SELECTED_BORDER : GearscopeStyle.TAB_BORDER);
            // Group icons are plain textures (assets/<ns>/textures/<icon>.png), drawn whole into 8x8
            // regardless of their actual size.
            guiGraphics.blit(this.tabIconTexture(tab), x + 1, y + 2, 8, 8, 0, 0, 1, 1, 1, 1);
        }

        if (this.tabs.size() > GearscopeLayout.TABS_VISIBLE) {
            this.renderArrow(guiGraphics, mouseX, mouseY, GearscopeLayout.TAB_X, GearscopeLayout.TAB_SCROLL_UP_Y, Arrow.UP, scroll > 0);
            this.renderArrow(guiGraphics, mouseX, mouseY, GearscopeLayout.TAB_X, GearscopeLayout.TAB_SCROLL_DOWN_Y, Arrow.DOWN, scroll < this.maxTabScroll());
        }

        int pages = this.pageCount(currentTab);
        if (pages > 1) {
            int page = this.currentPage(currentTab);
            this.renderArrow(guiGraphics, mouseX, mouseY, GearscopeLayout.PAGE_PREV_X, GearscopeLayout.PAGE_ARROW_Y, Arrow.LEFT, page > 0);
            this.renderArrow(guiGraphics, mouseX, mouseY, GearscopeLayout.PAGE_NEXT_X, GearscopeLayout.PAGE_ARROW_Y, Arrow.RIGHT, page < pages - 1);
        }
    }

    private void renderArrow(GuiGraphics guiGraphics, int mouseX, int mouseY, int relX, int relY, Arrow arrow, boolean enabled) {
        int x = this.leftPos + relX;
        int y = this.topPos + relY;
        int width = GearscopeLayout.ARROW_WIDTH;
        int height = GearscopeLayout.ARROW_HEIGHT;
        boolean hovered = enabled && this.isOver(mouseX, mouseY, relX, relY, width, height);
        int color = !enabled ? GearscopeStyle.ARROW_DISABLED : hovered ? GearscopeStyle.ARROW_HOVER : GearscopeStyle.ARROW;
        int centerX = x + width / 2;
        int centerY = y + height / 2;
        // Small 3-pixel triangles.
        for (int i = 0; i < 3; i++) {
            switch (arrow) {
                case UP -> guiGraphics.fill(centerX - i, y + 2 + i, centerX + i + 1, y + 3 + i, color);
                case DOWN -> guiGraphics.fill(centerX - (2 - i), y + 2 + i, centerX + (2 - i) + 1, y + 3 + i, color);
                case LEFT -> guiGraphics.fill(centerX - 1 + i, centerY - i, centerX + i, centerY + i + 1, color);
                case RIGHT -> guiGraphics.fill(centerX - 1 + i, centerY - (2 - i), centerX + i, centerY + (2 - i) + 1, color);
            }
        }
    }

    private void renderWidgetTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!this.menu.getCarried().isEmpty()) {
            return;
        }
        if (this.hoveredSlot instanceof ToggleableSlot toggleable && !this.hoveredSlot.hasItem()) {
            List<Component> tooltip = toggleable.gearscope$emptySlotTooltip();
            if (!tooltip.isEmpty()) {
                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                return;
            }
        }
        if (!this.hasAccessories) {
            return;
        }
        Component tooltip = this.widgetTooltip(mouseX, mouseY);
        if (tooltip != null) {
            guiGraphics.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Nullable
    private Component widgetTooltip(int mouseX, int mouseY) {
        int size = GearscopeLayout.COSMETIC_BUTTON_SIZE;
        if (this.isOver(mouseX, mouseY, GearscopeLayout.COSMETIC_BUTTON_X, GearscopeLayout.COSMETIC_BUTTON_Y, size, size)) {
            return Component.translatable("gearscope.cosmetic_slots");
        }
        String tab = this.tabAt(mouseX, mouseY);
        if (tab != null) {
            return tabName(tab);
        }
        String currentTab = this.currentTab();
        if (currentTab != null && this.pageCount(currentTab) > 1
                && (this.isOverArrow(mouseX, mouseY, GearscopeLayout.PAGE_PREV_X, GearscopeLayout.PAGE_ARROW_Y)
                || this.isOverArrow(mouseX, mouseY, GearscopeLayout.PAGE_NEXT_X, GearscopeLayout.PAGE_ARROW_Y))) {
            return Component.translatable("gearscope.page", this.currentPage(currentTab) + 1, this.pageCount(currentTab));
        }
        return null;
    }

    private Component tabName(String tab) {
        if (AccessorySlotSpec.UNSORTED_GROUP.equals(tab)) {
            return Component.translatable("gearscope.tab.other");
        }
        return Component.translatable(GearscopeServices.slotSource().groupTranslationKey(this.minecraft.level, tab));
    }

    /** The group's icon texture, or Gearscope's fallback for "Other" and for icons whose texture is missing. */
    private ResourceLocation tabIconTexture(String tab) {
        return this.tabIcons.computeIfAbsent(tab, key -> {
            if (AccessorySlotSpec.UNSORTED_GROUP.equals(key)) {
                return TAB_UNKNOWN_ICON;
            }
            ResourceLocation icon = GearscopeServices.slotSource().groupIcon(this.minecraft.level, key);
            if (icon == null) {
                return TAB_UNKNOWN_ICON;
            }
            ResourceLocation texture = icon.withPath(path -> "textures/" + path + ".png");
            return this.minecraft.getResourceManager().getResource(texture).isPresent() ? texture : TAB_UNKNOWN_ICON;
        });
    }

    // --- Input ---

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.hasAccessories && this.handleWidgetClick((int) mouseX, (int) mouseY)) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        if (this.attributePanel.mouseClicked(this.leftPos, this.topPos, mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.attributePanel.mouseDragged(this.topPos, mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.attributePanel.mouseReleased()) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Over the scroll the wheel only scrolls the attribute list.
        if (this.attributePanel.mouseScrolled(this.leftPos, this.topPos, mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean handleWidgetClick(int mouseX, int mouseY) {
        int size = GearscopeLayout.COSMETIC_BUTTON_SIZE;
        if (this.isOver(mouseX, mouseY, GearscopeLayout.COSMETIC_BUTTON_X, GearscopeLayout.COSMETIC_BUTTON_Y, size, size)) {
            GearscopeClientState.cosmeticMode = !GearscopeClientState.cosmeticMode;
            return true;
        }
        String tab = this.tabAt(mouseX, mouseY);
        if (tab != null) {
            GearscopeClientState.selectedTab = tab;
            return true;
        }
        if (this.tabs.size() > GearscopeLayout.TABS_VISIBLE) {
            if (this.isOverArrow(mouseX, mouseY, GearscopeLayout.TAB_X, GearscopeLayout.TAB_SCROLL_UP_Y) && GearscopeClientState.tabScroll > 0) {
                GearscopeClientState.tabScroll--;
                return true;
            }
            if (this.isOverArrow(mouseX, mouseY, GearscopeLayout.TAB_X, GearscopeLayout.TAB_SCROLL_DOWN_Y) && GearscopeClientState.tabScroll < this.maxTabScroll()) {
                GearscopeClientState.tabScroll++;
                return true;
            }
        }
        String currentTab = this.currentTab();
        if (currentTab != null && this.pageCount(currentTab) > 1) {
            int page = this.currentPage(currentTab);
            if (this.isOverArrow(mouseX, mouseY, GearscopeLayout.PAGE_PREV_X, GearscopeLayout.PAGE_ARROW_Y) && page > 0) {
                GearscopeClientState.TAB_PAGES.put(currentTab, page - 1);
                return true;
            }
            if (this.isOverArrow(mouseX, mouseY, GearscopeLayout.PAGE_NEXT_X, GearscopeLayout.PAGE_ARROW_Y) && page < this.pageCount(currentTab) - 1) {
                GearscopeClientState.TAB_PAGES.put(currentTab, page + 1);
                return true;
            }
        }
        return false;
    }

    @Nullable
    private String tabAt(int mouseX, int mouseY) {
        int scroll = GearscopeClientState.tabScroll;
        int visible = Math.min(GearscopeLayout.TABS_VISIBLE, this.tabs.size() - scroll);
        for (int i = 0; i < visible; i++) {
            int relY = GearscopeLayout.TAB_Y + i * GearscopeLayout.TAB_STEP;
            if (this.isOver(mouseX, mouseY, GearscopeLayout.TAB_X, relY, GearscopeLayout.TAB_WIDTH, GearscopeLayout.TAB_HEIGHT)) {
                return this.tabs.get(scroll + i);
            }
        }
        return null;
    }

    private boolean isOverArrow(int mouseX, int mouseY, int relX, int relY) {
        return this.isOver(mouseX, mouseY, relX, relY, GearscopeLayout.ARROW_WIDTH, GearscopeLayout.ARROW_HEIGHT);
    }

    private boolean isOver(double mouseX, double mouseY, int relX, int relY, int width, int height) {
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;
        return x >= relX && x < relX + width && y >= relY && y < relY + height;
    }
}
