package com.drehverschluss.gearscope.client;

import com.drehverschluss.gearscope.core.GearscopeLayout;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Effect display TOP: a row of tiles centered above the Gearscope frame. Rows wrap at the frame width and grow
 * upwards (the first row sits directly above the frame). The tiles use the slot frame texture as nine-patch.
 */
final class EffectTiles {
    private static final int SLICE = 2;
    private static final int SLOT_TEXTURE = GearscopeStyle.SLOT_TEXTURE_SIZE;

    private final GearscopeLayout layout;

    EffectTiles(GearscopeLayout layout) {
        this.layout = layout;
    }

    private int perRow(int frameWidth) {
        return Math.max(1, (frameWidth + this.layout.effectTileGap) / (this.layout.effectTileWidth + this.layout.effectTileGap));
    }

    private int rows(int count, int frameWidth) {
        return (count + this.perRow(frameWidth) - 1) / this.perRow(frameWidth);
    }

    /** Whether {@code count} tiles fit into the space above the frame (whose top edge is at {@code frameTop}). */
    boolean fits(int count, int frameWidth, int frameTop) {
        if (count == 0) {
            return true;
        }
        int rows = this.rows(count, frameWidth);
        int needed = this.layout.effectFrameGap + rows * this.layout.effectTileHeight + (rows - 1) * this.layout.effectTileGap;
        return needed <= frameTop;
    }

    /** Screen rectangle of every tile, in effect order. */
    List<Rect2i> tileRects(int count, int frameLeft, int frameTop, int frameWidth) {
        List<Rect2i> rects = new ArrayList<>(count);
        int perRow = this.perRow(frameWidth);
        int width = this.layout.effectTileWidth;
        int height = this.layout.effectTileHeight;
        int gap = this.layout.effectTileGap;
        for (int i = 0; i < count; i++) {
            int row = i / perRow;
            int inRow = Math.min(perRow, count - row * perRow);
            int rowWidth = inRow * width + (inRow - 1) * gap;
            int x = frameLeft + (frameWidth - rowWidth) / 2 + (i % perRow) * (width + gap);
            int y = frameTop - this.layout.effectFrameGap - height - row * (height + gap);
            rects.add(new Rect2i(x, y, width, height));
        }
        return rects;
    }

    /** Bounding box of each tile row, for JEI's exclusion areas. */
    List<Rect2i> rowAreas(int count, int frameLeft, int frameTop, int frameWidth) {
        List<Rect2i> tiles = this.tileRects(count, frameLeft, frameTop, frameWidth);
        List<Rect2i> rows = new ArrayList<>();
        int perRow = this.perRow(frameWidth);
        for (int start = 0; start < tiles.size(); start += perRow) {
            Rect2i first = tiles.get(start);
            Rect2i last = tiles.get(Math.min(tiles.size(), start + perRow) - 1);
            rows.add(new Rect2i(first.getX(), first.getY(), last.getX() + last.getWidth() - first.getX(), first.getHeight()));
        }
        return rows;
    }

    void render(GuiGraphics guiGraphics, Font font, List<MobEffectInstance> effects, List<Rect2i> rects) {
        for (int i = 0; i < effects.size(); i++) {
            MobEffectInstance effect = effects.get(i);
            Rect2i rect = rects.get(i);
            int x = rect.getX();
            int y = rect.getY();
            int width = rect.getWidth();
            int height = rect.getHeight();
            drawFrame(guiGraphics, x, y, width, height);
            if (effect.isAmbient()) {
                guiGraphics.renderOutline(x, y, width, height, GearscopeStyle.EFFECT_AMBIENT);
            }

            int iconSize = this.layout.effectTileIconSize;
            ActiveEffects.drawIcon(guiGraphics, effect, x + (width - iconSize) / 2, y + 2, iconSize);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
            Component level = ActiveEffects.level(effect);
            if (level != null) {
                // Small level numeral in the top right corner.
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x + width - 2 - font.width(level) * 0.5F, y + 2, 0.0F);
                guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
                guiGraphics.drawString(font, level, 0, 0, GearscopeStyle.EFFECT_TILE_NEUTRAL, true);
                guiGraphics.pose().popPose();
            }
            Component duration = ActiveEffects.duration(effect);
            int color = ActiveEffects.categoryColor(effect, GearscopeStyle.EFFECT_TILE_BENEFICIAL,
                    GearscopeStyle.EFFECT_TILE_HARMFUL, GearscopeStyle.EFFECT_TILE_NEUTRAL);
            int textWidth = font.width(duration);
            float scale = Math.min(1.0F, (width - 3) / (float) Math.max(1, textWidth));
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(x + (width - textWidth * scale) / 2.0F, y + height - 2 - 8 * scale, 0.0F);
            guiGraphics.pose().scale(scale, scale, 1.0F);
            guiGraphics.drawString(font, duration, 0, 0, color, true);
            guiGraphics.pose().popPose();
            guiGraphics.pose().popPose();
        }
    }

    @Nullable
    static MobEffectInstance effectAt(List<MobEffectInstance> effects, List<Rect2i> rects, double mouseX, double mouseY) {
        for (int i = 0; i < rects.size() && i < effects.size(); i++) {
            if (rects.get(i).contains((int) Math.floor(mouseX), (int) Math.floor(mouseY))) {
                return effects.get(i);
            }
        }
        return null;
    }

    /** The 18x18 slot frame stretched to any size: 2 px corners and edges, stretched middle. */
    private static void drawFrame(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int middle = SLOT_TEXTURE - 2 * SLICE;
        int[] xs = {x, x + SLICE, x + width - SLICE};
        int[] ws = {SLICE, width - 2 * SLICE, SLICE};
        int[] ys = {y, y + SLICE, y + height - SLICE};
        int[] hs = {SLICE, height - 2 * SLICE, SLICE};
        int[] uv = {0, SLICE, SLOT_TEXTURE - SLICE};
        int[] uvSize = {SLICE, middle, SLICE};
        RenderSystem.enableBlend();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                guiGraphics.blit(GearscopeStyle.SLOT, xs[column], ys[row], ws[column], hs[row], uv[column], uv[row],
                        uvSize[column], uvSize[row], SLOT_TEXTURE, SLOT_TEXTURE);
            }
        }
        RenderSystem.disableBlend();
    }
}
