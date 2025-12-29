package prisons.solar.npclib.core.ai.goals;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.ai.Goal;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Goal that makes the NPC look around randomly.
 */
public class LookRandomlyGoal implements Goal {

    private final int priority;
    private final int minLookTime;
    private final int maxLookTime;

    private int lookTimer;
    private float targetYaw;
    private float targetPitch;

    /**
     * Creates a look-randomly goal with default settings.
     */
    public LookRandomlyGoal() {
        this(80, 40, 80);
    }

    /**
     * Creates a look-randomly goal with custom settings.
     *
     * @param priority    goal priority
     * @param minLookTime minimum ticks to look in one direction
     * @param maxLookTime maximum ticks to look in one direction
     */
    public LookRandomlyGoal(int priority, int minLookTime, int maxLookTime) {
        this.priority = priority;
        this.minLookTime = minLookTime;
        this.maxLookTime = maxLookTime;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public boolean canStart(@NotNull NPC<?> npc) {
        return ThreadLocalRandom.current().nextFloat() < 0.02f;
    }

    @Override
    public boolean shouldContinue(@NotNull NPC<?> npc) {
        return lookTimer > 0;
    }

    @Override
    public void start(@NotNull NPC<?> npc) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        lookTimer = minLookTime + random.nextInt(maxLookTime - minLookTime);

        Position pos = npc.getPosition();
        targetYaw = pos.yaw() + (random.nextFloat() - 0.5f) * 90f;
        targetPitch = (random.nextFloat() - 0.5f) * 40f;
    }

    @Override
    public void tick(@NotNull NPC<?> npc) {
        lookTimer--;

        Position pos = npc.getPosition();
        float currentYaw = pos.yaw();
        float currentPitch = pos.pitch();

        // Smoothly interpolate towards target
        float yawDiff = targetYaw - currentYaw;
        float pitchDiff = targetPitch - currentPitch;

        float newYaw = currentYaw + yawDiff * 0.1f;
        float newPitch = currentPitch + pitchDiff * 0.1f;

        // Create look target position
        double radYaw = Math.toRadians(newYaw);
        double radPitch = Math.toRadians(newPitch);

        double x = pos.x() - Math.sin(radYaw) * Math.cos(radPitch) * 5;
        double y = pos.y() + 1.6 - Math.sin(radPitch) * 5;
        double z = pos.z() + Math.cos(radYaw) * Math.cos(radPitch) * 5;

        npc.lookAt(Position.of(pos.worldId(), x, y, z));
    }

    @Override
    public void stop(@NotNull NPC<?> npc) {
        lookTimer = 0;
    }

    @Override
    public int flags() {
        return Flag.LOOK.bit;
    }
}
