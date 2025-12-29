package prisons.solar.npclib.paper.status;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityStatus;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.status.CommonStatus;
import prisons.solar.npclib.api.status.EntityStatus;
import prisons.solar.npclib.api.status.MobStatus;
import prisons.solar.npclib.api.status.ObjectStatus;
import prisons.solar.npclib.api.status.PlayerStatus;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.paper.PaperViewer;

import java.util.Collection;

/**
 * Status handler for all entity status effects using entity status packets.
 *
 * <p>This handler maps entity statuses to their corresponding status codes
 * and sends entity status packets to viewers.
 */
final class EntityStatusHandler implements StatusHandler {

    @Override
    public boolean supports(@NotNull EntityType type, @NotNull EntityStatus status) {
        // All entity statuses use entity status packets
        return status instanceof CommonStatus || status instanceof PlayerStatus ||
               status instanceof MobStatus || status instanceof ObjectStatus;
    }

    @Override
    public void playStatus(int entityId, @NotNull EntityStatus status, @NotNull Collection<Viewer> viewers) {
        byte statusCode = getStatusCode(status);

        WrapperPlayServerEntityStatus packet = new WrapperPlayServerEntityStatus(entityId, statusCode);

        for (Viewer viewer : viewers) {
            if (viewer instanceof PaperViewer paperViewer) {
                PacketEvents.getAPI().getPlayerManager()
                        .sendPacket(paperViewer.getPlayer(), packet);
            }
        }
    }

    private byte getStatusCode(EntityStatus status) {
        if (status instanceof CommonStatus commonStatus) {
            return switch (commonStatus) {
                case DEATH -> 3;
                case TOTEM_ACTIVATION -> 35;
                case PORTAL_PARTICLES -> 46;
                case BREAK_MAIN_HAND -> 47;
                case BREAK_OFFHAND -> 48;
                case BREAK_HELMET -> 49;
                case BREAK_CHESTPLATE -> 50;
                case BREAK_LEGGINGS -> 51;
                case BREAK_BOOTS -> 52;
                case HONEY_SLIDE_PARTICLES -> 54;
            };
        } else if (status instanceof PlayerStatus playerStatus) {
            return switch (playerStatus) {
                case SHIELD_BLOCK -> 29;
                case SHIELD_BREAK -> 30;
                case SWAP_HANDS -> 55;
            };
        } else if (status instanceof MobStatus mobStatus) {
            return switch (mobStatus) {
                case ATTACK -> 4;
                case TAME_FAIL -> 6;
                case TAME_SUCCESS -> 7;
                case WOLF_SHAKE_START -> 8;
                case SHEEP_EAT_GRASS -> 10;
                case LOVE_HEARTS -> 18;
                case FOX_CHEW -> 45;
                case WOLF_SHAKE_STOP -> 56;
                case GOAT_PREPARE_RAM -> 58;
                case GOAT_STOP_RAM -> 59;
                case ALLAY_DANCE -> 60;
                case WARDEN_SONIC_BOOM -> 61;
                case WARDEN_TENDRIL_SHAKING -> 62;
            };
        } else if (status instanceof ObjectStatus objectStatus) {
            return switch (objectStatus) {
                case ARMOR_STAND_HIT -> 32;
            };
        }

        throw new IllegalArgumentException("Unsupported status type: " + status);
    }
}
