package prisons.solar.npclib.paper.status;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.types.PlayerMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.status.CommonStatus;
import prisons.solar.npclib.api.status.EntityStatus;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.paper.PaperViewer;
import prisons.solar.npclib.paper.npc.EntityLibLivingNPC;

import java.util.Collection;

import static com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.PLAYER;

/**
 * Status handler for player death using entity metadata.
 *
 * <p>This handler sets the health metadata field (index 8) to 0, which triggers
 * the player death animation. Unlike mobs, players do not respond to the generic
 * entity status packet (code 3) for death animations - they require the health
 * metadata to be explicitly set to 0.
 *
 * <p>For mob/object death, use the generic {@link EntityStatusHandler} with
 * status code 3, which works correctly for non-player entities.
 */
final class PlayerDeathStatusHandler implements StatusHandler {

    @Override
    public boolean supports(@NotNull EntityType type, @NotNull EntityStatus status) {
        return status == CommonStatus.DEATH && type == EntityType.PLAYER;
    }

    @Override
    public void playStatus(int entityId, @NotNull EntityStatus status, @NotNull Collection<Viewer> viewers) {
        // Players trigger death animation via health metadata (index 8) set to 0
        EntityMeta meta = EntityMeta.createMeta(entityId, PLAYER);
        PlayerMeta pmeta = (PlayerMeta) meta;
        pmeta.setHealth(0.0f);
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(
                entityId,
                pmeta
        );

        for (Viewer viewer : viewers) {
            if (viewer instanceof PaperViewer paperViewer) {
                PacketEvents.getAPI().getPlayerManager()
                        .sendPacket(paperViewer.getPlayer(), packet);
            }
        }
    }
}
