package prisons.solar.npclib.core.ai;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.ai.Goal;
import prisons.solar.npclib.api.ai.GoalSelector;
import prisons.solar.npclib.api.npc.NPC;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Simple implementation of {@link GoalSelector}.
 * Manages goal execution with priority-based selection and flag conflict resolution.
 */
public class SimpleGoalSelector implements GoalSelector {

    private final NPC<?> npc;
    private final List<Goal> goals = new CopyOnWriteArrayList<>();
    private final Set<Goal> runningGoals = Collections.newSetFromMap(new IdentityHashMap<>());
    private Goal activeGoal;
    private int activeFlags = 0;

    public SimpleGoalSelector(@NotNull NPC<?> npc) {
        this.npc = npc;
    }

    @Override
    public void addGoal(@NotNull Goal goal) {
        goals.add(goal);
        // Keep sorted by priority (lower = higher priority)
        goals.sort(Comparator.comparingInt(Goal::priority));
    }

    @Override
    public void removeGoal(@NotNull Goal goal) {
        if (runningGoals.contains(goal)) {
            goal.stop(npc);
            runningGoals.remove(goal);
            if (activeGoal == goal) {
                activeGoal = null;
            }
        }
        goals.remove(goal);
    }

    @Override
    public void removeGoals(@NotNull Class<? extends Goal> goalClass) {
        goals.removeIf(goal -> {
            if (goalClass.isInstance(goal)) {
                if (runningGoals.contains(goal)) {
                    goal.stop(npc);
                    runningGoals.remove(goal);
                    if (activeGoal == goal) {
                        activeGoal = null;
                    }
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public void clearGoals() {
        for (Goal goal : runningGoals) {
            goal.stop(npc);
        }
        runningGoals.clear();
        goals.clear();
        activeGoal = null;
        activeFlags = 0;
    }

    @Override
    public @NotNull Collection<Goal> goals() {
        return Collections.unmodifiableList(goals);
    }

    @Override
    public Goal activeGoal() {
        return activeGoal;
    }

    @Override
    public void tick() {
        // Evaluate goals in priority order
        for (Goal goal : goals) {
            if (runningGoals.contains(goal)) {
                // Check if should continue
                if (!goal.shouldContinue(npc)) {
                    goal.stop(npc);
                    runningGoals.remove(goal);
                    releaseFlags(goal);
                    if (activeGoal == goal) {
                        activeGoal = null;
                    }
                }
            } else {
                // Check if can start
                if (canStartGoal(goal)) {
                    // Check for conflicts with running goals
                    if (resolveConflicts(goal)) {
                        goal.start(npc);
                        runningGoals.add(goal);
                        acquireFlags(goal);
                        if (activeGoal == null || goal.priority() < activeGoal.priority()) {
                            activeGoal = goal;
                        }
                    }
                }
            }
        }

        // Tick all running goals
        for (Goal goal : runningGoals) {
            goal.tick(npc);
        }
    }

    @Override
    public boolean isRunning(@NotNull Goal goal) {
        return runningGoals.contains(goal);
    }

    @Override
    public boolean isRunning(@NotNull Goal.Flag flag) {
        return (activeFlags & flag.bit) != 0;
    }

    private boolean canStartGoal(Goal goal) {
        return goal.canStart(npc);
    }

    private boolean resolveConflicts(Goal newGoal) {
        int requiredFlags = newGoal.flags();

        // Check for conflicts with running goals
        for (Goal running : runningGoals) {
            int runningFlags = running.flags();
            if ((requiredFlags & runningFlags) != 0) {
                // Conflict detected
                if (!running.isInterruptible()) {
                    return false; // Can't interrupt
                }
                if (newGoal.priority() >= running.priority()) {
                    return false; // Not higher priority
                }
                // Will interrupt this goal
            }
        }

        // Stop conflicting goals
        Iterator<Goal> it = runningGoals.iterator();
        while (it.hasNext()) {
            Goal running = it.next();
            if ((requiredFlags & running.flags()) != 0) {
                running.stop(npc);
                releaseFlags(running);
                it.remove();
                if (activeGoal == running) {
                    activeGoal = null;
                }
            }
        }

        return true;
    }

    private void acquireFlags(Goal goal) {
        activeFlags |= goal.flags();
    }

    private void releaseFlags(Goal goal) {
        // Recalculate flags from remaining running goals
        activeFlags = 0;
        for (Goal running : runningGoals) {
            activeFlags |= running.flags();
        }
    }
}
