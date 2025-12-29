package prisons.solar.npclib.paper.status;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.status.EntityStatus;
import prisons.solar.npclib.api.viewer.Viewer;

import java.util.Collection;

/**
 * Handler for entity status effects.
 *
 * <p>Status handlers determine which entity statuses they support and how to play them.
 */
interface StatusHandler {

    /**
     * Checks if this handler supports the given entity type and status.
     *
     * @param type   the entity type
     * @param status the status
     * @return true if supported
     */
    boolean supports(@NotNull EntityType type, @NotNull EntityStatus status);

    /**
     * Plays an entity status for the given viewers.
     *
     * @param entityId the entity ID
     * @param status   the status to play
     * @param viewers  the viewers to show the status to
     */
    void playStatus(int entityId, @NotNull EntityStatus status, @NotNull Collection<Viewer> viewers);
}
