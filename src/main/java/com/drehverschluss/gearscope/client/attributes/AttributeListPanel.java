package com.drehverschluss.gearscope.client.attributes;

import com.drehverschluss.gearscope.client.GearscopeClientConfig;
import com.drehverschluss.gearscope.core.GearscopeLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.BooleanAttribute;

/**
 * The grouped attribute list on the scroll. Which attributes are shown (and in which group) is rebuilt
 * every few ticks; values are read from the attribute instances every frame.
 */
public final class AttributeListPanel {
    // Dark colors for the light parchment, all in one place.
    public static final int COLOR_TEXT = 0xFF3A2414;
    public static final int COLOR_HEADER = 0xFF6E1C18;
    public static final int COLOR_BETTER = 0xFF1E641E;
    public static final int COLOR_WORSE = 0xFFA01E1E;
    private static final int COLOR_ROW_HOVER = 0x22000000;
    private static final int COLOR_SCROLLBAR_TRACK = 0x33000000;
    private static final int COLOR_SCROLLBAR_THUMB = 0xFF6E5A40;

    private static final int REBUILD_INTERVAL_TICKS = 10;
    private static final int TOOLTIP_MAX_WIDTH = 220;
    private static final String ELLIPSIS = "\u2026";

    // Scrolling snaps to row starts: the list always starts with a whole row, never half of one. Kept across
    // screen instances so reopening (also server-side reopens) keeps the position.
    private static int scrollRow;

    private sealed interface Row permits HeaderRow, AttributeRow {
    }

    private record HeaderRow(String groupId, Component name, boolean collapsed) implements Row {
    }

    /**
     * @param name     translated name without a leading icon glyph from icon resource packs
     * @param nameIcon that glyph (with its formatting), drawn in the icon column if there is no icon texture
     * @param icon     attribute_icon texture, if a resource pack provides one
     */
    private record AttributeRow(AttributeInstance instance, FormattedText name, @Nullable FormattedText nameIcon,
            @Nullable ResourceLocation icon) implements Row {
    }

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Font font = this.minecraft.font;
    private final Map<ResourceLocation, Optional<ResourceLocation>> iconCache = new HashMap<>();
    private List<Row> rows = List.of();
    // Top of every row relative to the list start (group gaps included), same order as rows.
    private int[] rowTops = new int[0];
    private int contentHeight;
    private int maxScrollRow;
    private int tickCounter;
    private boolean draggingScrollbar;

    public AttributeListPanel() {
        this.rebuild();
    }

    public void tick() {
        if (++this.tickCounter % REBUILD_INTERVAL_TICKS == 0) {
            this.rebuild();
        }
    }

    // --- Building the list ---

    private void rebuild() {
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }
        AttributeSupport support = AttributeSupports.get();
        Set<ResourceLocation> hidden = new LinkedHashSet<>(support.hiddenAttributes());
        hidden.addAll(resolveHidden(GearscopeClientConfig.HIDDEN_ATTRIBUTES.get()));
        boolean hideUnchanged = GearscopeClientConfig.HIDE_UNCHANGED.get();
        AttributeGroups groups = AttributeGroupsLoader.groups();

        Map<String, List<AttributeInstance>> byGroup = new HashMap<>();
        BuiltInRegistries.ATTRIBUTE.holders().forEach(holder -> {
            AttributeInstance instance = player.getAttribute(holder);
            if (instance == null) {
                return;
            }
            ResourceLocation id = holder.key().location();
            // Same rule as Apothic's "Hide Unchanged": the value equals the base value.
            if (hidden.contains(id) || (hideUnchanged && instance.getValue() == instance.getBaseValue())) {
                return;
            }
            byGroup.computeIfAbsent(groups.groupOf(id), key -> new ArrayList<>()).add(instance);
        });

        List<String> groupIds = new ArrayList<>(byGroup.keySet());
        groupIds.sort(Comparator.comparingInt(groups::order).thenComparing(Comparator.naturalOrder()));

        List<Row> newRows = new ArrayList<>();
        List<Integer> tops = new ArrayList<>();
        int height = 0;
        for (String groupId : groupIds) {
            List<AttributeInstance> instances = byGroup.get(groupId);
            // Exactly listed attributes in list order, then wildcard/unassigned ones by translated name
            // (without icon glyphs and formatting codes, so icon packs do not change the order).
            Map<AttributeInstance, AttributeNameIcons.Split> names = new HashMap<>();
            for (AttributeInstance instance : instances) {
                names.put(instance, AttributeNameIcons.split(Component.translatable(instance.getAttribute().value().getDescriptionId())));
            }
            instances.sort(Comparator
                    .comparingInt((AttributeInstance instance) -> {
                        OptionalInt position = groups.exactPosition(id(instance.getAttribute()));
                        return position.isPresent() ? position.getAsInt() : Integer.MAX_VALUE;
                    })
                    .thenComparing(instance -> ChatFormatting.stripFormatting(names.get(instance).name().getString())));

            boolean collapsed = GearscopeClientConfig.isCollapsed(groupId);
            if (!newRows.isEmpty()) {
                height += GearscopeLayout.ATTR_GROUP_GAP;
            }
            newRows.add(new HeaderRow(groupId, groupName(groupId), collapsed));
            tops.add(height);
            height += GearscopeLayout.ATTR_HEADER_HEIGHT;
            if (!collapsed) {
                for (AttributeInstance instance : instances) {
                    AttributeNameIcons.Split name = names.get(instance);
                    newRows.add(new AttributeRow(instance, name.name(), name.icon(), this.icon(instance.getAttribute())));
                    tops.add(height);
                    height += GearscopeLayout.ATTR_ROW_HEIGHT;
                }
            }
        }
        this.rows = newRows;
        this.rowTops = tops.stream().mapToInt(Integer::intValue).toArray();
        this.contentHeight = height;
        // Last row index that can be at the top: the first one from which the rest fits into the list.
        int maxRow = 0;
        while (maxRow < this.rowTops.length && height - this.rowTops[maxRow] > this.listHeight()) {
            maxRow++;
        }
        this.maxScrollRow = Math.min(maxRow, Math.max(0, this.rowTops.length - 1));
        scrollRow = Mth.clamp(scrollRow, 0, this.maxScrollRow);
    }

    /** Pixel offset of the current scroll position (top of the first visible row). */
    private int scrollPixels() {
        return this.rowTops.length == 0 ? 0 : this.rowTops[scrollRow];
    }

    private static ResourceLocation id(Holder<Attribute> attribute) {
        return attribute.unwrapKey().map(key -> key.location()).orElseGet(() -> BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()));
    }

    private static Component groupName(String groupId) {
        String key = "gearscope.group." + groupId;
        return I18n.exists(key) ? Component.translatable(key) : Component.literal(groupId);
    }

    /**
     * Icon texture at assets/&lt;namespace&gt;/textures/attribute_icon/&lt;path&gt;.png, the path the attribute icon
     * resource packs use; null if no pack provides one.
     */
    @Nullable
    private ResourceLocation icon(Holder<Attribute> attribute) {
        ResourceLocation id = id(attribute);
        return this.iconCache.computeIfAbsent(id, key -> {
            ResourceLocation texture = key.withPath(path -> "textures/attribute_icon/" + path + ".png");
            return this.minecraft.getResourceManager().getResource(texture).isPresent() ? Optional.of(texture) : Optional.empty();
        }).orElse(null);
    }

    /**
     * Resolves the config list: "ns:path" hides one attribute, "ns:*" a whole namespace, "!ns:path" shows one
     * again. Processed in order, like Apothic's "Hidden Attributes".
     */
    private static Set<ResourceLocation> resolveHidden(List<? extends String> entries) {
        Set<ResourceLocation> hidden = new LinkedHashSet<>();
        for (String entry : entries) {
            try {
                if (entry.endsWith(":*")) {
                    String namespace = entry.substring(0, entry.length() - 2);
                    for (ResourceLocation id : BuiltInRegistries.ATTRIBUTE.keySet()) {
                        if (id.getNamespace().equals(namespace)) {
                            hidden.add(id);
                        }
                    }
                } else if (entry.startsWith("!")) {
                    hidden.remove(ResourceLocation.parse(entry.substring(1)));
                } else {
                    hidden.add(ResourceLocation.parse(entry));
                }
            } catch (ResourceLocationException ignored) {
                // Invalid entries are skipped.
            }
        }
        return hidden;
    }

    // --- Rendering ---

    public void render(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }
        int x1 = left + GearscopeLayout.SCROLL_X1;
        int x2 = left + GearscopeLayout.SCROLL_X2;
        int titleY = top + GearscopeLayout.ATTR_TITLE_Y;
        int listTop = top + GearscopeLayout.ATTR_LIST_Y;
        int listBottom = top + GearscopeLayout.SCROLL_Y2;

        guiGraphics.drawString(this.font, Component.translatable("gearscope.attributes.title"), x1, titleY + 2, COLOR_HEADER, false);
        this.renderCheckbox(guiGraphics, x2 - GearscopeLayout.ATTR_CHECKBOX_SIZE, titleY + 2, GearscopeClientConfig.HIDE_UNCHANGED.get());

        int contentRight = this.contentRight(x2);
        Row hovered = this.rowAt(left, top, mouseX, mouseY);
        guiGraphics.enableScissor(x1, listTop, x2, listBottom);
        int y = listTop - this.scrollPixels();
        boolean first = true;
        for (Row row : this.rows) {
            if (row instanceof HeaderRow && !first) {
                y += GearscopeLayout.ATTR_GROUP_GAP;
            }
            first = false;
            int height = rowHeight(row);
            if (y + height > listTop && y < listBottom) {
                if (row instanceof HeaderRow header) {
                    this.renderHeader(guiGraphics, header, x1, contentRight, y);
                } else if (row instanceof AttributeRow attribute) {
                    if (row == hovered) {
                        guiGraphics.fill(x1, y, contentRight, y + height, COLOR_ROW_HOVER);
                    }
                    this.renderAttribute(guiGraphics, attribute, x1, contentRight, y);
                }
            }
            y += height;
        }
        guiGraphics.disableScissor();

        if (this.isScrollbarVisible()) {
            int trackX = x2 - GearscopeLayout.ATTR_SCROLLBAR_WIDTH;
            guiGraphics.fill(trackX, listTop, x2, listBottom, COLOR_SCROLLBAR_TRACK);
            int thumbHeight = this.thumbHeight();
            int thumbY = listTop + (int) ((long) (this.listHeight() - thumbHeight) * scrollRow / Math.max(1, this.maxScrollRow));
            guiGraphics.fill(trackX, thumbY, x2, thumbY + thumbHeight, COLOR_SCROLLBAR_THUMB);
        }
    }

    private void renderCheckbox(GuiGraphics guiGraphics, int x, int y, boolean checked) {
        int size = GearscopeLayout.ATTR_CHECKBOX_SIZE;
        guiGraphics.renderOutline(x, y, size, size, COLOR_TEXT);
        if (checked) {
            // Small check mark.
            guiGraphics.fill(x + 2, y + 4, x + 3, y + 6, COLOR_TEXT);
            guiGraphics.fill(x + 3, y + 5, x + 4, y + 7, COLOR_TEXT);
            guiGraphics.fill(x + 4, y + 4, x + 5, y + 6, COLOR_TEXT);
            guiGraphics.fill(x + 5, y + 2, x + 6, y + 5, COLOR_TEXT);
        }
    }

    private void renderHeader(GuiGraphics guiGraphics, HeaderRow header, int x1, int contentRight, int y) {
        // Triangle: pointing right when collapsed, down when open.
        int tx = x1 + 1;
        int ty = y + 3;
        for (int i = 0; i < 3; i++) {
            if (header.collapsed()) {
                guiGraphics.fill(tx + i, ty + i, tx + i + 1, ty + 5 - i, COLOR_HEADER);
            } else {
                guiGraphics.fill(tx + i, ty + 1 + i, tx + 5 - i, ty + 2 + i, COLOR_HEADER);
            }
        }
        int textX = x1 + 8;
        guiGraphics.drawString(this.font, header.name(), textX, y + 2, COLOR_HEADER, false);
        int lineX = textX + this.font.width(header.name()) + 3;
        if (lineX < contentRight) {
            guiGraphics.fill(lineX, y + 6, contentRight, y + 7, COLOR_HEADER);
        }
    }

    private void renderAttribute(GuiGraphics guiGraphics, AttributeRow row, int x1, int contentRight, int y) {
        AttributeInstance instance = row.instance();
        Attribute attribute = instance.getAttribute().value();
        // Icon column: the attribute_icon texture, otherwise the glyph an icon pack put into the name.
        if (row.icon() != null) {
            int size = GearscopeLayout.ATTR_ICON_SIZE;
            guiGraphics.blit(row.icon(), x1, y, size, size, 0, 0, 1, 1, 1, 1);
        } else if (row.nameIcon() != null) {
            guiGraphics.drawString(this.font, Language.getInstance().getVisualOrder(row.nameIcon()), x1, y + 1, COLOR_TEXT, false);
        }

        Component value = AttributeSupports.get().isDynamic(instance)
                ? Component.literal("\uFFFD")
                : attribute.toValueComponent(null, instance.getValue(), TooltipFlag.NORMAL);
        int valueWidth = this.font.width(value);
        guiGraphics.drawString(this.font, value, contentRight - valueWidth, y + 1,
                valueColor(attribute, instance.getValue(), instance.getBaseValue()), false);

        // Every name starts at the same x; too long names end with an ellipsis.
        int nameX = x1 + GearscopeLayout.ATTR_ICON_SIZE + 2;
        int maxNameWidth = contentRight - valueWidth - 4 - nameX;
        FormattedText name = row.name();
        if (this.font.width(name) > maxNameWidth) {
            FormattedText cut = this.font.substrByWidth(name, Math.max(0, maxNameWidth - this.font.width(ELLIPSIS)));
            name = FormattedText.composite(cut, FormattedText.of(ELLIPSIS));
        }
        guiGraphics.drawString(this.font, Language.getInstance().getVisualOrder(name), nameX, y + 1, COLOR_TEXT, false);
    }

    /**
     * Better/worse than the base value, using the attribute's sentiment like Apothic's getValueColor (so e.g.
     * "damage taken" going up is red).
     */
    public static int valueColor(Attribute attribute, double value, double base) {
        if (value == base) {
            return COLOR_TEXT;
        }
        ChatFormatting style;
        if (attribute instanceof RangedAttribute) {
            style = attribute.getStyle(value > base);
        } else if (attribute instanceof BooleanAttribute) {
            style = attribute.getStyle(value > 0);
        } else {
            return COLOR_TEXT;
        }
        return switch (style) {
            case BLUE -> COLOR_BETTER;
            case RED -> COLOR_WORSE;
            default -> COLOR_TEXT;
        };
    }

    public void renderTooltip(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) {
            return;
        }
        if (this.isOverCheckbox(left, top, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.translatable("gearscope.attributes.hide_unchanged"), mouseX, mouseY);
            return;
        }
        if (this.rowAt(left, top, mouseX, mouseY) instanceof AttributeRow row) {
            List<ClientTooltipComponent> tooltip = AttributeSupports.get().tooltip(player, row.instance(), this.font, TOOLTIP_MAX_WIDTH);
            if (!tooltip.isEmpty()) {
                guiGraphics.renderTooltipInternal(this.font, tooltip, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE);
            }
        }
    }

    // --- Layout helpers ---

    private static int rowHeight(Row row) {
        return row instanceof HeaderRow ? GearscopeLayout.ATTR_HEADER_HEIGHT : GearscopeLayout.ATTR_ROW_HEIGHT;
    }

    private int listHeight() {
        return GearscopeLayout.SCROLL_Y2 - GearscopeLayout.ATTR_LIST_Y;
    }

    private boolean isScrollbarVisible() {
        return this.contentHeight > this.listHeight();
    }

    private int thumbHeight() {
        int listHeight = this.listHeight();
        return Mth.clamp(listHeight * listHeight / Math.max(1, this.contentHeight), 8, listHeight);
    }

    private int contentRight(int x2) {
        return this.isScrollbarVisible() ? x2 - GearscopeLayout.ATTR_SCROLLBAR_WIDTH - 2 : x2;
    }

    public boolean isMouseOver(int left, int top, double mouseX, double mouseY) {
        return mouseX >= left + GearscopeLayout.SCROLL_X1 && mouseX < left + GearscopeLayout.SCROLL_X2
                && mouseY >= top + GearscopeLayout.SCROLL_Y1 && mouseY < top + GearscopeLayout.SCROLL_Y2;
    }

    private boolean isOverList(int left, int top, double mouseX, double mouseY) {
        return mouseX >= left + GearscopeLayout.SCROLL_X1 && mouseX < left + GearscopeLayout.SCROLL_X2
                && mouseY >= top + GearscopeLayout.ATTR_LIST_Y && mouseY < top + GearscopeLayout.SCROLL_Y2;
    }

    private boolean isOverCheckbox(int left, int top, double mouseX, double mouseY) {
        int size = GearscopeLayout.ATTR_CHECKBOX_SIZE;
        int x = left + GearscopeLayout.SCROLL_X2 - size;
        int y = top + GearscopeLayout.ATTR_TITLE_Y + 2;
        return mouseX >= x - 1 && mouseX < x + size + 1 && mouseY >= y - 1 && mouseY < y + size + 1;
    }

    private boolean isOverScrollbar(int left, int top, double mouseX, double mouseY) {
        return this.isScrollbarVisible() && this.isOverList(left, top, mouseX, mouseY)
                && mouseX >= left + GearscopeLayout.SCROLL_X2 - GearscopeLayout.ATTR_SCROLLBAR_WIDTH - 1;
    }

    @Nullable
    private Row rowAt(int left, int top, double mouseX, double mouseY) {
        if (!this.isOverList(left, top, mouseX, mouseY) || mouseX >= this.contentRight(left + GearscopeLayout.SCROLL_X2)) {
            return null;
        }
        int y = top + GearscopeLayout.ATTR_LIST_Y - this.scrollPixels();
        boolean first = true;
        for (Row row : this.rows) {
            if (row instanceof HeaderRow && !first) {
                y += GearscopeLayout.ATTR_GROUP_GAP;
            }
            first = false;
            int height = rowHeight(row);
            if (mouseY >= y && mouseY < y + height) {
                return row;
            }
            y += height;
        }
        return null;
    }

    // --- Input ---

    public boolean mouseClicked(int left, int top, double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(left, top, mouseX, mouseY)) {
            return false;
        }
        if (button == 0) {
            if (this.isOverCheckbox(left, top, mouseX, mouseY)) {
                GearscopeClientConfig.setHideUnchanged(!GearscopeClientConfig.HIDE_UNCHANGED.get());
                this.rebuild();
            } else if (this.isOverScrollbar(left, top, mouseX, mouseY)) {
                this.draggingScrollbar = true;
                this.scrollToMouse(top, mouseY);
            } else if (this.rowAt(left, top, mouseX, mouseY) instanceof HeaderRow header) {
                GearscopeClientConfig.setCollapsed(header.groupId(), !header.collapsed());
                this.rebuild();
            }
        }
        // Clicks on the scroll never reach the slots behind it.
        return true;
    }

    public boolean mouseDragged(int top, double mouseY) {
        if (!this.draggingScrollbar) {
            return false;
        }
        this.scrollToMouse(top, mouseY);
        return true;
    }

    public boolean mouseReleased() {
        boolean wasDragging = this.draggingScrollbar;
        this.draggingScrollbar = false;
        return wasDragging;
    }

    public boolean mouseScrolled(int left, int top, double mouseX, double mouseY, double scrollY) {
        if (!this.isMouseOver(left, top, mouseX, mouseY)) {
            return false;
        }
        // One row (or group header) per wheel step.
        scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY), 0, this.maxScrollRow);
        return true;
    }

    private void scrollToMouse(int top, double mouseY) {
        int thumbHeight = this.thumbHeight();
        double trackStart = top + GearscopeLayout.ATTR_LIST_Y + thumbHeight / 2.0;
        double trackLength = Math.max(1, this.listHeight() - thumbHeight);
        double fraction = Mth.clamp((mouseY - trackStart) / trackLength, 0, 1);
        scrollRow = (int) Math.round(fraction * this.maxScrollRow);
    }
}
