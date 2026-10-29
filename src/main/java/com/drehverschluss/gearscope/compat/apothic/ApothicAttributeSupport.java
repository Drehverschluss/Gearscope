package com.drehverschluss.gearscope.compat.apothic;

import com.drehverschluss.gearscope.client.attributes.AttributeSupport;
import com.drehverschluss.gearscope.client.attributes.SimpleAttributeSupport;
import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.client.AttributeModifierComponent;
import dev.shadowsoffire.apothic_attributes.client.AttributesGui;
import dev.shadowsoffire.apothic_attributes.client.ModifierSource;
import dev.shadowsoffire.apothic_attributes.client.ModifierSourceType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;

/**
 * Attribute support backed by Apothic Attributes: its hidden-attributes config, dynamic attributes, and the
 * same tooltip as its own attributes screen. The tooltip follows AttributesGui#renderTooltip from Apothic
 * Attributes 2.11.0 (MIT, Shadows of Fire) step by step, using its public building blocks; it only returns
 * the components instead of drawing them, so Gearscope can position the tooltip itself. See
 * THIRD_PARTY_NOTICES.md.
 */
final class ApothicAttributeSupport implements AttributeSupport {
    @Override
    public Set<ResourceLocation> hiddenAttributes() {
        // Apothic resolves "namespace:*" and "!negation" entries when loading its config.
        return ALConfig.hiddenAttributes;
    }

    @Override
    public boolean isDynamic(AttributeInstance instance) {
        return instance.getAttribute().is(ALObjects.Tags.DYNAMIC_BASE_ATTRIBUTES);
    }

    @Override
    public List<ClientTooltipComponent> tooltip(Player player, AttributeInstance inst, Font font, int maxWidth) {
        Attribute attr = inst.getAttribute().value();
        boolean isDynamic = this.isDynamic(inst);
        TooltipFlag flag = ApothicAttributes.getTooltipFlag();

        List<Component> list = new ArrayList<>();
        MutableComponent name = Component.translatable(attr.getDescriptionId()).withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withUnderlined(true));
        if (isDynamic) {
            name.append(CommonComponents.SPACE);
            name.append(Component.translatable("apothic_attributes.gui.dynamic").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withUnderlined(false)));
        }
        if (flag.isAdvanced()) {
            Style style = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withUnderlined(false);
            name.append(Component.literal(" [" + BuiltInRegistries.ATTRIBUTE.getKey(attr) + "]").withStyle(style));
        }
        list.add(name);

        String key = attr.getDescriptionId() + ".desc";
        if (I18n.exists(key)) {
            list.add(Component.translatable(key).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        } else if (flag.isAdvanced()) {
            list.add(Component.literal(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        int color = AttributesGui.getValueColor(inst, ChatFormatting.GRAY.getColor());
        Component valueComp = attr.toValueComponent(null, inst.getValue(), flag).withColor(color);
        Component baseComp = attr.toValueComponent(null, inst.getBaseValue(), flag).withStyle(ChatFormatting.GRAY);

        if (!isDynamic) {
            list.add(CommonComponents.EMPTY);
            list.add(Component.translatable("apothic_attributes.gui.current", valueComp).withStyle(ChatFormatting.GRAY));
            Component base = Component.translatable("apothic_attributes.gui.base", baseComp).withStyle(ChatFormatting.GRAY);
            if (attr instanceof RangedAttribute ranged) {
                Component min = Component.translatable("apothic_attributes.gui.min", attr.toValueComponent(null, ranged.getMinValue(), flag));
                Component max = Component.translatable("apothic_attributes.gui.max", attr.toValueComponent(null, ranged.getMaxValue(), flag));
                list.add(Component.translatable("%s \u2507 %s \u2507 %s", base, min, max).withStyle(ChatFormatting.GRAY));
            } else {
                list.add(base);
            }
        }

        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        for (Component line : list) {
            SimpleAttributeSupport.addWrapped(line, font, maxWidth, tooltip);
        }

        if (inst.getModifiers().stream().anyMatch(modifier -> modifier.amount() != 0)) {
            SimpleAttributeSupport.addWrapped(CommonComponents.EMPTY, font, maxWidth, tooltip);
            SimpleAttributeSupport.addWrapped(Component.translatable("apothic_attributes.gui.modifiers").withStyle(ChatFormatting.GOLD), font, maxWidth, tooltip);

            Map<ResourceLocation, ModifierSource<?>> modifiersToSources = new HashMap<>();
            for (ModifierSourceType<?> type : ModifierSourceType.getTypes()) {
                type.extract(player, (modifier, source) -> modifiersToSources.put(modifier.id(), source));
            }

            double[] numericValues = new double[3];
            for (Operation op : Operation.values()) {
                double baseValue = op == Operation.ADD_MULTIPLIED_TOTAL ? 1 : 0;
                List<AttributeModifier> modifiers = new ArrayList<>(inst.getModifiers().stream()
                        .filter(modifier -> modifier.operation() == op).toList());
                double opValue = modifiers.stream().mapToDouble(AttributeModifier::amount)
                        .reduce(baseValue, (result, amount) -> op == Operation.ADD_MULTIPLIED_TOTAL ? result * (1 + amount) : result + amount);

                modifiers.sort(ModifierSourceType.compareBySource(modifiersToSources));
                for (AttributeModifier modifier : modifiers) {
                    if (modifier.amount() != 0) {
                        Component comp = attr.toComponent(modifier, flag);
                        tooltip.add(new AttributeModifierComponent(modifiersToSources.get(modifier.id()), comp, font, maxWidth));
                    }
                }
                numericValues[op.ordinal()] = opValue;
            }

            SimpleAttributeSupport.addWrapped(CommonComponents.EMPTY, font, maxWidth, tooltip);
            SimpleAttributeSupport.addWrapped(Component.translatable("apothic_attributes.gui.formula").withStyle(ChatFormatting.GOLD), font, maxWidth, tooltip);
            Component base = isDynamic ? Component.translatable("apothic_attributes.gui.formula.base") : baseComp;
            Component value = isDynamic ? Component.translatable("apothic_attributes.gui.formula.value") : valueComp;
            SimpleAttributeSupport.addWrapped(AttributesGui.buildFormula(base, value, numericValues, attr), font, maxWidth, tooltip);
        } else if (isDynamic) {
            SimpleAttributeSupport.addWrapped(CommonComponents.EMPTY, font, maxWidth, tooltip);
            SimpleAttributeSupport.addWrapped(Component.translatable("apothic_attributes.gui.no_modifiers").withStyle(ChatFormatting.GOLD), font, maxWidth, tooltip);
        }
        return tooltip;
    }
}
