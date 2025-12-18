package prisons.solar.npclib.core.engine;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.ai.GoalSelector;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.NPCRegistry;
import prisons.solar.npclib.api.npc.NPCState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Core tick engine for NPC AI and behavior processing.
 * Manages scheduled updates for goals, pathfinding, and animations.
 */
public class NPCEngine {

    private final NPCRegistry registry;
    private final Map<UUID, GoalSelector> goalSelectors = new ConcurrentHashMap<>();
    private final Map<UUID, Consumer<NPC<?>>> tickCallbacks = new ConcurrentHashMap<>();

    private volatile boolean running = false;
    private long tickCount = 0;

    public NPCEngine(@NotNull NPCRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registers a goal selector for an NPC.
     *
     * @param npc      the NPC
     * @param selector the goal selector
     */
    public void registerGoalSelector(@NotNull NPC<?> npc, @NotNull GoalSelector selector) {
        goalSelectors.put(npc.id(), selector);
    }

    /**
     * Unregisters the goal selector for an NPC.
     *
     * @param npc the NPC
     */
    public void unregisterGoalSelector(@NotNull NPC<?> npc) {
        goalSelectors.remove(npc.id());
    }

    /**
     * Gets the goal selector for an NPC.
     *
     * @param npc the NPC
     * @return the goal selector, or null
     */
    public GoalSelector getGoalSelector(@NotNull NPC<?> npc) {
        return goalSelectors.get(npc.id());
    }

    /**
     * Registers a custom tick callback for an NPC.
     *
     * @param npc      the NPC
     * @param callback the tick callback
     */
    public void registerTickCallback(@NotNull NPC<?> npc, @NotNull Consumer<NPC<?>> callback) {
        tickCallbacks.put(npc.id(), callback);
    }

    /**
     * Unregisters the tick callback for an NPC.
     *
     * @param npc the NPC
     */
    public void unregisterTickCallback(@NotNull NPC<?> npc) {
        tickCallbacks.remove(npc.id());
    }

    /**
     * Starts the engine.
     */
    public void start() {
        running = true;
    }

    /**
     * Stops the engine.
     */
    public void stop() {
        running = false;
        goalSelectors.clear();
        tickCallbacks.clear();
    }

    /**
     * Returns whether the engine is running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Ticks all registered NPCs.
     * Should be called from the main server thread.
     */
    public void tick() {
        if (!running) {
            return;
        }

        tickCount++;

        for (NPC<?> npc : registry.all()) {
            if (npc.state() != NPCState.SPAWNED) {
                continue;
            }

            tickNPC(npc);
        }
    }

    /**
     * Ticks a single NPC.
     *
     * @param npc the NPC to tick
     */
    private void tickNPC(@NotNull NPC<?> npc) {
        // Tick goal selector
        GoalSelector selector = goalSelectors.get(npc.id());
        if (selector != null) {
            selector.tick();
        }

        // Run custom tick callback
        Consumer<NPC<?>> callback = tickCallbacks.get(npc.id());
        if (callback != null) {
            try {
                callback.accept(npc);
            } catch (Exception e) {
                // Log but don't crash
                System.err.println("[Phantom] Error in tick callback for NPC " + npc.id() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Gets the current tick count.
     *
     * @return tick count since engine start
     */
    public long tickCount() {
        return tickCount;
    }

    /**
     * Gets the number of NPCs with goal selectors.
     *
     * @return count
     */
    public int activeGoalSelectors() {
        return goalSelectors.size();
    }

    /**
     * Gets the number of NPCs with tick callbacks.
     *
     * @return count
     */
    public int activeTickCallbacks() {
        return tickCallbacks.size();
    }

    /**
     * Cleans up resources for an NPC.
     * Called when an NPC is destroyed.
     *
     * @param npc the NPC
     */
    public void cleanup(@NotNull NPC<?> npc) {
        goalSelectors.remove(npc.id());
        tickCallbacks.remove(npc.id());
    }
}
