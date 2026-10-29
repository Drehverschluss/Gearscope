package com.drehverschluss.gearscope.client;

import com.drehverschluss.gearscope.Gearscope;
import com.drehverschluss.gearscope.core.AccessoryPlacement;
import com.drehverschluss.gearscope.core.AccessorySlotPlacer;
import com.drehverschluss.gearscope.core.AccessorySlotSpec;
import com.drehverschluss.gearscope.core.GearscopeLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client placement: each slot takes the next free place configured for its slot type in slot_places.json
 * (every place is used at most once); everything else goes into the tab of its Accessories group, filling
 * pages of 2x4 slots.
 */
public final class GearscopeSlotPlacer implements AccessorySlotPlacer {
    private final Set<String> warnedUnknownPlaces = ConcurrentHashMap.newKeySet();

    @Override
    public List<AccessoryPlacement> place(List<AccessorySlotSpec> specs) {
        GearscopeLayout layout = GearscopeLayout.current();
        Map<String, List<String>> slotPlaces = SlotPlacesLoader.slotPlaces();
        Set<String> takenPlaces = new HashSet<>();
        Map<String, Integer> tabCounts = new HashMap<>();
        List<AccessoryPlacement> placements = new ArrayList<>();

        for (AccessorySlotSpec spec : specs) {
            List<String> candidates = slotPlaces.getOrDefault(spec.slotName(), List.of());
            for (int i = 0; i < spec.size(); i++) {
                GearscopeLayout.Place place = null;
                for (String candidate : candidates) {
                    if (takenPlaces.contains(candidate)) {
                        continue;
                    }
                    place = layout.place(candidate);
                    if (place == null) {
                        if (this.warnedUnknownPlaces.add(candidate)) {
                            Gearscope.LOGGER.warn("slot_places.json uses place '{}' (slot type '{}') which layout.json does not define",
                                    candidate, spec.slotName());
                        }
                        continue;
                    }
                    takenPlaces.add(candidate);
                    break;
                }
                if (place != null) {
                    placements.add(new AccessoryPlacement(place.x(), place.y(), place.name(), null, 0));
                } else {
                    int position = tabCounts.merge(spec.groupName(), 1, Integer::sum) - 1;
                    int onPage = position % layout.tabSlotsPerPage;
                    int column = onPage % layout.tabSlotX.length;
                    int row = onPage / layout.tabSlotX.length;
                    placements.add(new AccessoryPlacement(layout.tabSlotX[column],
                            layout.tabSlotY + row * layout.tabSlotStep, null, spec.groupName(), position));
                }
            }
        }
        return placements;
    }
}
