package prisons.solar.npclib.api.ai;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;

/**
 * Represents an AI goal that an NPC can execute.
 * Goals are prioritized and can be interrupted by higher priority goals.
 */
public interface Goal {

    /**
     * Gets the priority of this goal.
     * Lower numbers = higher priority.
     *
     * @return the priority (0-100)
     */
    int priority();

    /**
     * Checks if this goal can start executing.
     *
     * @param npc the NPC
     * @return true if the goal can start
     */
    boolean canStart(@NotNull NPC<?> npc);

    /**
     * Checks if this goal should continue executing.
     *
     * @param npc the NPC
     * @return true if the goal should continue
     */
    boolean shouldContinue(@NotNull NPC<?> npc);

    /**
     * Called when the goal starts executing.
     *
     * @param npc the NPC
     */
    void start(@NotNull NPC<?> npc);

    /**
     * Called each tick while the goal is executing.
     *
     * @param npc the NPC
     */
    void tick(@NotNull NPC<?> npc);

    /**
     * Called when the goal stops executing.
     *
     * @param npc the NPC
     */
    void stop(@NotNull NPC<?> npc);

    /**
     * Returns whether this goal can be interrupted by other goals.
     *
     * @return true if interruptible
     */
    default boolean isInterruptible() {
        return true;
    }

    /**
     * Returns the goal flags that this goal uses.
     * Goals with overlapping flags cannot run simultaneously.
     *
     * @return the flags
     */
    default int flags() {
        return Flag.MOVE.bit | Flag.LOOK.bit;
    }

    /**
     * Goal flags for resource management.
     */
    enum Flag {
        MOVE(1),
        LOOK(2),
        JUMP(4),
        TARGET(8);

        public final int bit;

        Flag(int bit) {
            this.bit = bit;
        }
    }
}
