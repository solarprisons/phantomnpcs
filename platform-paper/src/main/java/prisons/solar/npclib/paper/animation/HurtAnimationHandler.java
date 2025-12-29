package prisons.solar.npclib.paper.animation;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHurtAnimation;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.animation.EntityAnimation;
import prisons.solar.npclib.api.animation.NPCAnimation;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.paper.PaperViewer;

import java.util.Collection;

/**
 * Animation handler for hurt animations using the dedicated hurt animation packet.
 *
 * <p>This handler uses {@link WrapperPlayServerHurtAnimation} which provides
 * a dedicated packet type for hurt/damage animations. This packet works reliably
 * on all entity types including client-side NPCs.
 */
final class HurtAnimationHandler implements AnimationHandler {

    @Override
    public boolean supports(@NotNull EntityType type, @NotNull NPCAnimation animation) {
        return animation == EntityAnimation.TAKE_DAMAGE;
    }

    @Override
    public void playAnimation(int entityId, @NotNull NPCAnimation animation, @NotNull Collection<Viewer> viewers) {
        // Yaw parameter indicates damage direction on client
        // Using 0.0f as neutral default since we don't track damage source direction
        WrapperPlayServerHurtAnimation packet = new WrapperPlayServerHurtAnimation(entityId, 0.0f);
        for (Viewer viewer : viewers) {
            if (viewer instanceof PaperViewer paperViewer) {
                PacketEvents.getAPI().getPlayerManager()
                        .sendPacket(paperViewer.getPlayer(), packet);
            }
        }
    }
}
