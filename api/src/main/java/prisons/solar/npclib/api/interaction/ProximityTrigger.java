package prisons.solar.npclib.api.interaction;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;

/**
 * Defines a proximity-based trigger zone around an NPC.
 */
public interface ProximityTrigger {

    /**
     * Gets the NPC this trigger is attached to.
     *
     * @return the NPC
     */
    @NotNull NPC<?> npc();

    /**
     * Gets the trigger radius in blocks.
     *
     * @return the radius
     */
    double radius();

    /**
     * Gets the handler for this trigger.
     *
     * @return the proximity handler
     */
    @NotNull ProximityHandler handler();

    /**
     * Creates a new proximity trigger.
     *
     * @param npc     the NPC
     * @param radius  the trigger radius
     * @param handler the handler
     * @return new trigger
     */
    static ProximityTrigger of(@NotNull NPC<?> npc, double radius, @NotNull ProximityHandler handler) {
        return new ProximityTrigger() {
            @Override
            public @NotNull NPC<?> npc() {
                return npc;
            }

            @Override
            public double radius() {
                return radius;
            }

            @Override
            public @NotNull ProximityHandler handler() {
                return handler;
            }
        };
    }
}
