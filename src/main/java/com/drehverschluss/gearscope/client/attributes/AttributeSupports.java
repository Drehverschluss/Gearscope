package com.drehverschluss.gearscope.client.attributes;

/**
 * Holds the active {@link AttributeSupport}; the Apothic compat replaces the fallback when Apothic is loaded.
 */
public final class AttributeSupports {
    private static volatile AttributeSupport current = SimpleAttributeSupport.INSTANCE;

    private AttributeSupports() {
    }

    public static AttributeSupport get() {
        return current;
    }

    public static void set(AttributeSupport support) {
        current = support;
    }
}
