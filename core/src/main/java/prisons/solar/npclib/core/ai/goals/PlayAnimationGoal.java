package prisons.solar.npclib.core.ai.goals;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.ai.Goal;
import prisons.solar.npclib.api.npc.NPC;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Goal that plays animations periodically.
 */
public class PlayAnimationGoal implements Goal {

    private final NPC.Animation animation;
    private final int priority;
    private final float chance;
    private final int cooldownTicks;

    private int cooldown;

    /**
     * Creates an animation goal.
     *
     * @param animation the animation to play
     */
    public PlayAnimationGoal(@NotNull NPC.Animation animation) {
        this(animation, 90, 0.01f, 100);
    }

    /**
     * Creates an animation goal with custom settings.
     *
     * @param animation     the animation to play
     * @param priority      goal priority
     * @param chance        chance per tick to play (0.0-1.0)
     * @param cooldownTicks minimum ticks between animations
     */
    public PlayAnimationGoal(@NotNull NPC.Animation animation, int priority, float chance, int cooldownTicks) {
        this.animation = animation;
        this.priority = priority;
        this.chance = chance;
        this.cooldownTicks = cooldownTicks;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public boolean canStart(@NotNull NPC<?> npc) {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        return ThreadLocalRandom.current().nextFloat() < chance;
    }

    @Override
    public boolean shouldContinue(@NotNull NPC<?> npc) {
        return false; // Single execution
    }

    @Override
    public void start(@NotNull NPC<?> npc) {
        npc.playAnimation(animation);
        cooldown = cooldownTicks;
    }

    @Override
    public void tick(@NotNull NPC<?> npc) {
        // Nothing to tick
    }

    @Override
    public void stop(@NotNull NPC<?> npc) {
        // Nothing to cleanup
    }

    @Override
    public int flags() {
        return 0; // Doesn't use any flags
    }
}
