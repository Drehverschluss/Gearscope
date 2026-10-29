package com.drehverschluss.gearscope.core;

/**
 * Identifies a single Accessories slot in the Gearscope menu: slot type, index within that type, and
 * whether it is the functional or the cosmetic slot.
 */
public record AccessorySlotMeta(String slotName, int index, boolean cosmetic, String groupName) {
}
