package prisons.solar.npclib.core.engine;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.interaction.ProximityHandler;
import prisons.solar.npclib.api.interaction.ProximityTrigger;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.viewer.Viewer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks proximity triggers and fires enter/exit/stay events.
 */
public class ProximityTracker {

    private final Map<UUID, ProximityTrigger> triggers = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> viewersInRange = new ConcurrentHashMap<>();

    /**
     * Registers a proximity trigger for an NPC.
     *
     * @param trigger the trigger
     */
    public void register(@NotNull ProximityTrigger trigger) {
        triggers.put(trigger.npc().getId(), trigger);
        viewersInRange.putIfAbsent(trigger.npc().getId(), ConcurrentHashMap.newKeySet());
    }

    /**
     * Registers a proximity trigger.
     *
     * @param npc     the NPC
     * @param radius  the trigger radius
     * @param handler the handler
     */
    public void register(@NotNull NPC<?> npc, double radius, @NotNull ProximityHandler handler) {
        register(ProximityTrigger.of(npc, radius, handler));
    }

    /**
     * Unregisters a proximity trigger.
     *
     * @param npc the NPC
     */
    public void unregister(@NotNull NPC<?> npc) {
        triggers.remove(npc.getId());
        viewersInRange.remove(npc.getId());
    }

    /**
     * Gets the trigger for an NPC.
     *
     * @param npc the NPC
     * @return the trigger, or null
     */
    public ProximityTrigger getTrigger(@NotNull NPC<?> npc) {
        return triggers.get(npc.getId());
    }

    /**
     * Checks if an NPC has a proximity trigger.
     *
     * @param npc the NPC
     * @return true if has trigger
     */
    public boolean hasTrigger(@NotNull NPC<?> npc) {
        return triggers.containsKey(npc.getId());
    }

    /**
     * Updates proximity for a single viewer against all NPCs.
     * Call this each tick or at configured intervals.
     *
     * @param viewer the viewer to check
     */
    public void tick(@NotNull Viewer viewer) {
        for (ProximityTrigger trigger : triggers.values()) {
            tickTrigger(trigger, viewer);
        }
    }

    /**
     * Updates proximity for all viewers against all NPCs.
     *
     * @param viewers all online viewers
     */
    public void tickAll(@NotNull Collection<Viewer> viewers) {
        for (Viewer viewer : viewers) {
            tick(viewer);
        }
    }

    private void tickTrigger(@NotNull ProximityTrigger trigger, @NotNull Viewer viewer) {
        NPC<?> npc = trigger.npc();
        Set<UUID> inRange = viewersInRange.get(npc.getId());
        if (inRange == null) {
            return;
        }

        // Check same world
        if (!viewer.position().worldId().equals(npc.getPosition().worldId())) {
            // Different world - treat as exit if was in range
            if (inRange.remove(viewer.id())) {
                try {
                    trigger.handler().onExit(npc, viewer);
                } catch (Exception e) {
                    // Swallow handler errors to prevent propagation
                }
            }
            return;
        }

        double distanceSq = viewer.position().distanceSquared(npc.getPosition());
        double radiusSq = trigger.radius() * trigger.radius();
        boolean wasInRange = inRange.contains(viewer.id());
        boolean nowInRange = distanceSq <= radiusSq;

        if (nowInRange && !wasInRange) {
            // Entered range
            inRange.add(viewer.id());
            try {
                trigger.handler().onEnter(npc, viewer);
            } catch (Exception e) {
                // Swallow handler errors to prevent propagation
            }
        } else if (!nowInRange && wasInRange) {
            // Exited range
            inRange.remove(viewer.id());
            try {
                trigger.handler().onExit(npc, viewer);
            } catch (Exception e) {
                // Swallow handler errors to prevent propagation
            }
        } else if (nowInRange) {
            // Staying in range
            try {
                trigger.handler().onStay(npc, viewer);
            } catch (Exception e) {
                // Swallow handler errors to prevent propagation
            }
        }
    }

    /**
     * Called when a viewer disconnects.
     * Removes them from all tracking sets.
     *
     * @param viewer the disconnected viewer
     */
    public void onViewerDisconnect(@NotNull Viewer viewer) {
        for (Map.Entry<UUID, Set<UUID>> entry : viewersInRange.entrySet()) {
            if (entry.getValue().remove(viewer.id())) {
                ProximityTrigger trigger = triggers.get(entry.getKey());
                if (trigger != null) {
                    try {
                        trigger.handler().onExit(trigger.npc(), viewer);
                    } catch (Exception e) {
                        // Ignore - viewer is disconnecting anyway
                    }
                }
            }
        }
    }

    /**
     * Gets all viewers currently in range of an NPC.
     *
     * @param npc the NPC
     * @return set of viewer UUIDs in range
     */
    @NotNull
    public Set<UUID> viewersInRange(@NotNull NPC<?> npc) {
        Set<UUID> inRange = viewersInRange.get(npc.getId());
        return inRange != null ? Collections.unmodifiableSet(inRange) : Collections.emptySet();
    }

    /**
     * Gets the count of active triggers.
     *
     * @return trigger count
     */
    public int triggerCount() {
        return triggers.size();
    }

    /**
     * Clears all triggers.
     */
    public void clear() {
        triggers.clear();
        viewersInRange.clear();
    }
}
