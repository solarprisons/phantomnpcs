package prisons.solar.npclib.paper.animation;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.animation.EntityAnimation;
import prisons.solar.npclib.api.animation.NPCAnimation;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.paper.PaperViewer;

import java.util.Collection;

/**
 * Animation handler for universal entity animations using entity animation packets.
 *
 * <p>This handler supports {@link EntityAnimation} values that are common to all entity types.
 *
 * <p>Note: {@link EntityAnimation#TAKE_DAMAGE} is handled by {@link HurtAnimationHandler}
 * using a dedicated packet for better compatibility with client-side NPCs.
 */
final class CommonAnimationHandler implements AnimationHandler {

    @Override
    public boolean supports(@NotNull EntityType type, @NotNull NPCAnimation animation) {
        // Support all EntityAnimation values except TAKE_DAMAGE (handled by HurtAnimationHandler)
        if (animation instanceof EntityAnimation entityAnimation) {
            return entityAnimation != EntityAnimation.TAKE_DAMAGE;
        }

        return false;
    }

    @Override
    public void playAnimation(int entityId, @NotNull NPCAnimation animation, @NotNull Collection<Viewer> viewers) {
        EntityAnimationType animationType = getAnimationType(animation);

        WrapperPlayServerEntityAnimation packet = new WrapperPlayServerEntityAnimation(entityId, animationType);

        for (Viewer viewer : viewers) {
            if (viewer instanceof PaperViewer paperViewer) {
                PacketEvents.getAPI().getPlayerManager()
                        .sendPacket(paperViewer.getPlayer(), packet);
            }
        }
    }

    private EntityAnimationType getAnimationType(NPCAnimation animation) {
        if (animation instanceof EntityAnimation entityAnimation) {
            return switch (entityAnimation) {
                case SWING_MAIN_ARM -> EntityAnimationType.SWING_MAIN_ARM;
                case SWING_OFFHAND -> EntityAnimationType.SWING_OFF_HAND;
                case CRITICAL_EFFECT -> EntityAnimationType.CRITICAL_HIT;
                case MAGIC_CRITICAL_EFFECT -> EntityAnimationType.MAGIC_CRITICAL_HIT;
                case TAKE_DAMAGE -> throw new IllegalArgumentException("TAKE_DAMAGE should be handled by HurtAnimationHandler");
            };
        }

        throw new IllegalArgumentException("Not an entity animation: " + animation);
    }
}
