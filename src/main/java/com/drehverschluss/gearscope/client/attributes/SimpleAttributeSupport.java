package com.drehverschluss.gearscope.client.attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;

/**
 * Fallback without Apothic Attributes: nothing hidden, no dynamic attributes, and a plain tooltip with name,
 * description, current and base value and the list of modifiers.
 */
public final class SimpleAttributeSupport implements AttributeSupport {
    public static final SimpleAttributeSupport INSTANCE = new SimpleAttributeSupport();

    private SimpleAttributeSupport() {
    }

    @Override
    public Set<ResourceLocation> hiddenAttributes() {
        return Set.of();
    }

    @Override
    public boolean isDynamic(AttributeInstance instance) {
        return false;
    }

    @Override
    public List<ClientTooltipComponent> tooltip(Player player, AttributeInstance instance, Font font, int maxWidth) {
        Attribute attribute = instance.getAttribute().value();
        TooltipFlag flag = Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
        List<Component> lines = new ArrayList<>();

        lines.add(Component.translatable(attribute.getDescriptionId())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withUnderlined(true)));
        String descriptionKey = attribute.getDescriptionId() + ".desc";
        if (I18n.exists(descriptionKey)) {
            lines.add(Component.translatable(descriptionKey).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        }

        lines.add(CommonComponents.EMPTY);
        lines.add(Component.translatable("gearscope.attributes.current",
                attribute.toValueComponent(null, instance.getValue(), flag)).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gearscope.attributes.base",
                attribute.toValueComponent(null, instance.getBaseValue(), flag)).withStyle(ChatFormatting.GRAY));

        List<AttributeModifier> modifiers = instance.getModifiers().stream().filter(modifier -> modifier.amount() != 0).toList();
        if (!modifiers.isEmpty()) {
            lines.add(CommonComponents.EMPTY);
            lines.add(Component.translatable("gearscope.attributes.modifiers").withStyle(ChatFormatting.GOLD));
            for (AttributeModifier modifier : modifiers) {
                lines.add(attribute.toComponent(modifier, flag));
            }
        }

        List<ClientTooltipComponent> result = new ArrayList<>();
        for (Component line : lines) {
            addWrapped(line, font, maxWidth, result);
        }
        return result;
    }

    /** Adds a component, wrapped to {@code maxWidth}; empty lines stay as a single blank line. */
    public static void addWrapped(Component line, Font font, int maxWidth, List<ClientTooltipComponent> result) {
        if (line == CommonComponents.EMPTY) {
            result.add(ClientTooltipComponent.create(line.getVisualOrderText()));
            return;
        }
        for (FormattedText part : font.getSplitter().splitLines(line, maxWidth, line.getStyle())) {
            result.add(ClientTooltipComponent.create(Language.getInstance().getVisualOrder(part)));
        }
    }
}
