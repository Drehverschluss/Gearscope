package com.drehverschluss.gearscope.compat.apothic;

import com.drehverschluss.gearscope.client.attributes.AttributeSupports;

/**
 * Client entry point of the Apothic Attributes integration. Only called when Apothic is loaded, so no other
 * part of Gearscope ever loads an Apothic class.
 */
public final class ApothicCompat {
    public static final String MOD_ID = "apothic_attributes";

    private ApothicCompat() {
    }

    public static void initClient() {
        AttributeSupports.set(new ApothicAttributeSupport());
    }
}
