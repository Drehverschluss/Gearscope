package com.drehverschluss.gearscope.core;

/**
 * An accessory slot whose item can be shown or hidden on the character (Accessories' per-slot render toggle).
 * Implemented by the accessory compat's slots, so the screen never touches an accessory mod's classes.
 */
public interface RenderToggleSlot {
    /** Client side: whether the item in this slot is currently rendered on the character. */
    boolean gearscope$rendersOnCharacter();

    /** Client side: asks the server to flip the render state, the same way the accessory mod's own screen does. */
    void gearscope$toggleRendering();
}
