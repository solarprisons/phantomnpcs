package prisons.solar.npclib.core.ai.goals;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.ai.Goal;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.viewer.Viewer;

/**
 * Goal that makes the NPC look at the nearest viewer.
 */
public class LookAtViewerGoal implements Goal {

    private final double maxDistance;
    private final int priority;
    private Viewer target;

    /**
     * Creates a look-at-viewer goal.
     *
     * @param maxDistance maximum distance to look at viewers
     */
    public LookAtViewerGoal(double maxDistance) {
        this(maxDistance, 50);
    }

    /**
     * Creates a look-at-viewer goal with custom priority.
     *
     * @param maxDistance maximum distance
     * @param priority    goal priority
     */
    public LookAtViewerGoal(double maxDistance, int priority) {
        this.maxDistance = maxDistance;
        this.priority = priority;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public boolean canStart(@NotNull NPC<?> npc) {
        target = findNearestViewer(npc);
        return target != null;
    }

    @Override
    public boolean shouldContinue(@NotNull NPC<?> npc) {
        if (target == null || !target.isOnline()) {
            return false;
        }

        Position npcPos = npc.getPosition();
        Position viewerPos = target.position();

        // Check same world and distance
        if (!npcPos.worldId().equals(viewerPos.worldId())) {
            return false;
        }

        return npcPos.distanceSquared(viewerPos) <= maxDistance * maxDistance;
    }

    @Override
    public void start(@NotNull NPC<?> npc) {
        // Initial look
        if (target != null) {
            npc.lookAt(target);
        }
    }

    @Override
    public void tick(@NotNull NPC<?> npc) {
        if (target != null && target.isOnline()) {
            npc.lookAt(target);
        }
    }

    @Override
    public void stop(@NotNull NPC<?> npc) {
        target = null;
    }

    @Override
    public int flags() {
        return Flag.LOOK.bit;
    }

    private Viewer findNearestViewer(NPC<?> npc) {
        Position npcPos = npc.getPosition();
        double maxDistSq = maxDistance * maxDistance;

        Viewer nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (Viewer viewer : npc.viewers()) {
            if (!viewer.isOnline()) continue;

            Position viewerPos = viewer.position();
            if (!npcPos.worldId().equals(viewerPos.worldId())) continue;

            double distSq = npcPos.distanceSquared(viewerPos);
            if (distSq <= maxDistSq && distSq < nearestDistSq) {
                nearest = viewer;
                nearestDistSq = distSq;
            }
        }

        return nearest;
    }
}
