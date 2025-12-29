package prisons.solar.npclib.paper.animation;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.animation.MobAnimation;
import prisons.solar.npclib.api.animation.NPCAnimation;
import prisons.solar.npclib.api.animation.ObjectAnimation;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.paper.PaperViewer;

import java.util.Collection;

/**
 * Animation handler for entity-specific animations using entity status packets.
 *
 * <p>This handler maps entity-specific animations (like ALLAY_DANCE, WARDEN_SONIC_BOOM)
 * to their corresponding entity status codes and sends status packets to viewers.
 */
final class EntityStatusAnimationHandler implements AnimationHandler {

    @Override
    public boolean supports(@NotNull EntityType type, @NotNull NPCAnimation animation) {
        // Handle entity-specific mob animations only
        if (animation instanceof MobAnimation mobAnimation) {
            return isEntitySpecific(mobAnimation);
        }

        return animation instanceof ObjectAnimation;
    }

    @Override
    public void playAnimation(int entityId, @NotNull NPCAnimation animation, @NotNull Collection<Viewer> viewers) {
        byte statusCode = getStatusCode(animation);

        WrapperPlayServerEntityStatus packet = new WrapperPlayServerEntityStatus(entityId, statusCode);

        for (Viewer viewer : viewers) {
            if (viewer instanceof PaperViewer paperViewer) {
                PacketEvents.getAPI().getPlayerManager()
                        .sendPacket(paperViewer.getPlayer(), packet);
            }
        }
    }

    private boolean isEntitySpecific(MobAnimation animation) {
        // All remaining MobAnimation values are entity-specific
        return true;
    }

    private byte getStatusCode(NPCAnimation animation) {
        if (animation instanceof MobAnimation mobAnimation) {
            return switch (mobAnimation) {
                case ALLAY_DANCE -> 60;
                case FOX_CHEW -> 45;
                case GOAT_PREPARE_RAM -> 58;
                case GOAT_STOP_RAM -> 59;
                case IRON_GOLEM_ATTACK, RAVAGER_ATTACK -> 4;
                case WARDEN_SONIC_BOOM -> 61;
                case WARDEN_TENDRIL_SHAKING -> 62;
                case WOLF_SHAKE_WATER -> 8;
                case WOLF_STOP_SHAKE_WATER -> 56;
                case SHEEP_EAT_GRASS -> 10;
                case TAME_SUCCESS -> 7;
                case TAME_FAIL -> 6;
            };
        } else if (animation instanceof ObjectAnimation objectAnimation) {
            return switch (objectAnimation) {
                case ARMOR_STAND_HIT -> 32;
            };
        }

        throw new IllegalArgumentException("Unsupported animation type: " + animation);
    }
}
