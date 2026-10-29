package com.drehverschluss.gearscope.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Computes screen positions for the Accessories slots of a menu. Positions only matter on the client; the
 * client installs a real implementation at startup, everything else uses {@link #NONE}.
 */
@FunctionalInterface
public interface AccessorySlotPlacer {
    AccessorySlotPlacer NONE = specs -> {
        int total = 0;
        for (AccessorySlotSpec spec : specs) {
            total += spec.size();
        }
        return new ArrayList<>(Collections.nCopies(total, AccessoryPlacement.UNPLACED));
    };

    /**
     * @return one placement per slot index of every spec, in spec order (spec 0 index 0, spec 0 index 1, ...).
     */
    List<AccessoryPlacement> place(List<AccessorySlotSpec> specs);
}
