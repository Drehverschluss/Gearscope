package com.drehverschluss.gearscope.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

/**
 * The player's active effects as the vanilla inventory shows them (same filter, order, name and duration), plus
 * icon drawing and tooltips shared by all effect display modes. Everything is read live from the client player.
 */
public final class ActiveEffects {
    private static final int TOOLTIP_WIDTH = 200;

    private ActiveEffects() {
    }

    /** Visible effects in vanilla inventory order (MobEffectInstance's natural order). */
    public static List<MobEffectInstance> visible(Player player) {
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            if (ClientHooks.shouldRenderEffect(effect)) {
                effects.add(effect);
            }
        }
        effects.sort(null);
        return effects;
    }

    /** Effect name, plus the level as roman numeral from II to X like the vanilla inventory. */
    public static Component name(MobEffectInstance effect) {
        MutableComponent name = effect.getEffect().value().getDisplayName().copy();
        Component level = level(effect);
        if (level != null) {
            name.append(CommonComponents.SPACE).append(level);
        }
        return name;
    }

    /** The level as roman numeral (II to X), or null for level I and levels the vanilla inventory does not show. */
    public static Component level(MobEffectInstance effect) {
        int amplifier = effect.getAmplifier();
        return amplifier >= 1 && amplifier <= 9 ? Component.translatable("enchantment.level." + (amplifier + 1)) : null;
    }

    /** Remaining time like the vanilla inventory ("1:30", "∞" for infinite effects). */
    public static Component duration(MobEffectInstance effect) {
        Minecraft minecraft = Minecraft.getInstance();
        float tickRate = minecraft.level != null ? minecraft.level.tickRateManager().tickrate() : 20.0F;
        return MobEffectUtil.formatDuration(effect, 1.0F, tickRate);
    }

    /**
     * Draws the effect icon at the given size. Effects with their own icon rendering for the HUD (NeoForge's
     * IClientMobEffectExtensions#renderGuiIcon) use it here as well; their inventory variant only works in
     * vanilla's effect screens. Otherwise the vanilla effect sprite is drawn.
     */
    public static void drawIcon(GuiGraphics guiGraphics, MobEffectInstance effect, int x, int y, int size) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        float scale = size / 18.0F;
        pose.scale(scale, scale, 1.0F);
        // The HUD draws the 18x18 icon 3 px inside its 24x24 background.
        if (!IClientMobEffectExtensions.of(effect).renderGuiIcon(effect, minecraft.gui, guiGraphics, -3, -3, 0, 1.0F)) {
            TextureAtlasSprite sprite = minecraft.getMobEffectTextures().get(effect.getEffect());
            guiGraphics.blit(0, 0, 0, 18, 18, sprite);
        }
        pose.popPose();
    }

    /** Name with level, remaining time and, if a resource pack provides "&lt;effect key&gt;.desc", the description. */
    public static List<FormattedCharSequence> tooltip(MobEffectInstance effect, Font font) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(name(effect).getVisualOrderText());
        lines.add(duration(effect).copy().withStyle(ChatFormatting.GRAY).getVisualOrderText());
        String descriptionKey = effect.getEffect().value().getDescriptionId() + ".desc";
        if (I18n.exists(descriptionKey)) {
            lines.addAll(font.split(Component.translatable(descriptionKey).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC), TOOLTIP_WIDTH));
        }
        return lines;
    }

    /** One of the given colors by the effect's category: beneficial, harmful or neutral. */
    public static int categoryColor(MobEffectInstance effect, int beneficial, int harmful, int neutral) {
        return switch (effect.getEffect().value().getCategory()) {
            case BENEFICIAL -> beneficial;
            case HARMFUL -> harmful;
            case NEUTRAL -> neutral;
        };
    }
}
