package prisons.solar.npclib.paper.status;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.status.EntityStatus;
import prisons.solar.npclib.api.viewer.Viewer;

import java.util.Collection;
import java.util.List;

/**
 * Registry of status handlers.
 *
 * <p>This class coordinates all status handlers and delegates status playing
 * to the appropriate handler.
 */
public final class StatusHandlers {

    private static final List<StatusHandler> HANDLERS = List.of(
            new PlayerDeathStatusHandler(),  // Try dedicated death packet first for players
            new EntityStatusHandler()         // Fall back to generic status codes
    );

    private StatusHandlers() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Plays an entity status for the given viewers.
     *
     * @param entityType the entity type
     * @param entityId   the entity ID
     * @param status     the status to play
     * @param viewers    the viewers to show the status to
     * @throws IllegalArgumentException if no handler supports the status
     */
    public static void playStatus(@NotNull EntityType entityType, int entityId,
                                   @NotNull EntityStatus status, @NotNull Collection<Viewer> viewers) {
        for (StatusHandler handler : HANDLERS) {
            if (handler.supports(entityType, status)) {
                handler.playStatus(entityId, status, viewers);
                return;
            }
        }

        throw new IllegalArgumentException("No handler found for status: " + status + " on entity type: " + entityType);
    }

    /**
     * Checks if any handler supports the given entity type and status.
     *
     * @param entityType the entity type
     * @param status     the status
     * @return true if supported
     */
    public static boolean supportsStatus(@NotNull EntityType entityType, @NotNull EntityStatus status) {
        return HANDLERS.stream()
                .anyMatch(handler -> handler.supports(entityType, status));
    }
}
