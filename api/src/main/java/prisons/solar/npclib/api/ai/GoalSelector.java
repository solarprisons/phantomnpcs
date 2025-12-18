package prisons.solar.npclib.api.ai;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Manages and executes goals for an NPC.
 */
public interface GoalSelector {

    /**
     * Adds a goal to this selector.
     *
     * @param goal the goal to add
     */
    void addGoal(@NotNull Goal goal);

    /**
     * Removes a goal from this selector.
     *
     * @param goal the goal to remove
     */
    void removeGoal(@NotNull Goal goal);

    /**
     * Removes all goals of a specific type.
     *
     * @param goalClass the goal class
     */
    void removeGoals(@NotNull Class<? extends Goal> goalClass);

    /**
     * Clears all goals.
     */
    void clearGoals();

    /**
     * Gets all registered goals.
     *
     * @return collection of goals
     */
    @NotNull Collection<Goal> goals();

    /**
     * Gets the currently executing goal.
     *
     * @return the active goal, or null if none
     */
    Goal activeGoal();

    /**
     * Ticks the goal selector, evaluating and executing goals.
     */
    void tick();

    /**
     * Checks if a specific goal is currently running.
     *
     * @param goal the goal
     * @return true if running
     */
    boolean isRunning(@NotNull Goal goal);

    /**
     * Checks if any goal with the specified flag is running.
     *
     * @param flag the flag
     * @return true if any goal with that flag is running
     */
    boolean isRunning(@NotNull Goal.Flag flag);
}
