package com.drehverschluss.gearscope.client.attributes;

import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

/**
 * What the attribute list needs from an attribute mod. With Apothic Attributes installed the compat in
 * compat/apothic replaces {@link SimpleAttributeSupport}.
 */
public interface AttributeSupport {
    /** Attributes the attribute mod hides (already resolved, no wildcards). */
    Set<ResourceLocation> hiddenAttributes();

    /** Attributes whose base value is dynamic and therefore shown without a number. */
    boolean isDynamic(AttributeInstance instance);

    /** The hover tooltip for one attribute. */
    List<ClientTooltipComponent> tooltip(Player player, AttributeInstance instance, Font font, int maxWidth);
}
