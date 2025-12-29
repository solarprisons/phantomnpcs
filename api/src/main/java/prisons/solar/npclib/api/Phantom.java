package prisons.solar.npclib.api;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.combat.CombatHandler;
import prisons.solar.npclib.api.combat.DamageCalculator;
import prisons.solar.npclib.api.event.EventBus;
import prisons.solar.npclib.api.npc.NPCRegistry;
import prisons.solar.npclib.api.service.ServiceRegistry;
import prisons.solar.npclib.api.viewer.ViewerTracker;

/**
 * Main entry point for the Phantom NPC library.
 * Each instance is independent - multiple plugins can have their own Phantom instance.
 */
public interface Phantom {

    default void setCombatHandler(CombatHandler handler){
        services().register(CombatHandler.class, handler, 0);
    }

    default void setDamageCalculator(DamageCalculator calc){
        services().register(DamageCalculator.class, calc, 0);
    }



    /**
     * Gets the NPC registry for creating and managing NPCs.
     *
     * @return the NPC registry
     */
    @NotNull NPCRegistry npcs();

    /**
     * Gets the viewer tracker for managing NPC visibility.
     *
     * @return the viewer tracker
     */
    @NotNull ViewerTracker viewers();

    /**
     * Gets the event bus for subscribing to NPC events.
     *
     * @return the event bus
     */
    @NotNull EventBus events();

    /**
     * Gets the service registry for extensions.
     *
     * @return the service registry
     */
    @NotNull ServiceRegistry services();

    /**
     * Enables the Phantom instance.
     * This starts tracking players and processing ticks.
     */
    void enable();

    /**
     * Disables the Phantom instance.
     * This despawns all NPCs and releases resources.
     */
    void disable();

    /**
     * Returns whether this instance is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Invalidates pathfinding cache around a block position.
     * Call this when blocks are placed/broken to update NPC pathfinding.
     *
     * @param worldId world UUID
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param z block Z coordinate
     */
    void invalidatePathfindingCache(@NotNull String worldId, int x, int y, int z);

    /**
     * Creates a new builder for constructing a Phantom instance.
     *
     * @return new builder
     */
    static Builder builder() {
        throw new UnsupportedOperationException("Use PhantomBuilder from the core module");
    }

    /**
     * Builder for creating Phantom instances.
     */
    interface Builder {

        /**
         * Sets the platform adapter.
         *
         * @param platform the platform adapter
         * @return this builder
         */
        @NotNull Builder platform(@NotNull Object platform);

        /**
         * Sets the protocol adapter.
         *
         * @param protocol the protocol adapter
         * @return this builder
         */
        @NotNull Builder protocol(@NotNull Object protocol);

        /**
         * Sets the default view distance for NPCs.
         *
         * @param distance the view distance in blocks
         * @return this builder
         */
        @NotNull Builder viewDistance(double distance);

        /**
         * Sets the tick rate for visibility updates.
         *
         * @param ticks ticks between visibility checks
         * @return this builder
         */
        @NotNull Builder visibilityTickRate(int ticks);

        /**
         * Builds the Phantom instance.
         *
         * @return the constructed Phantom instance
         */
        @NotNull Phantom build();
    }
}
