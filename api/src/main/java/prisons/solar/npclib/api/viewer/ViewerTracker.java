package prisons.solar.npclib.api.viewer;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;

import java.util.Collection;
import java.util.UUID;

/**
 * Tracks which viewers can see which NPCs.
 */
public interface ViewerTracker {

    /**
     * Gets all viewers currently seeing an NPC.
     *
     * @param npc the NPC
     * @return collection of viewers
     */
    @NotNull Collection<Viewer> viewers(@NotNull NPC<?> npc);

    /**
     * Gets all NPCs visible to a viewer.
     *
     * @param viewer the viewer
     * @return collection of visible NPCs
     */
    @NotNull Collection<NPC<?>> visibleTo(@NotNull Viewer viewer);

    /**
     * Checks if a viewer can see an NPC.
     *
     * @param viewer the viewer
     * @param npc    the NPC
     * @return true if visible
     */
    boolean canSee(@NotNull Viewer viewer, @NotNull NPC<?> npc);

    /**
     * Manually shows an NPC to a viewer, bypassing visibility rules.
     *
     * @param npc    the NPC
     * @param viewer the viewer
     */
    void show(@NotNull NPC<?> npc, @NotNull Viewer viewer);

    /**
     * Manually hides an NPC from a viewer.
     *
     * @param npc    the NPC
     * @param viewer the viewer
     */
    void hide(@NotNull NPC<?> npc, @NotNull Viewer viewer);

    /**
     * Recalculates visibility for an NPC against all online viewers.
     *
     * @param npc the NPC
     */
    void updateVisibility(@NotNull NPC<?> npc);

    /**
     * Recalculates visibility for a viewer against all NPCs.
     *
     * @param viewer the viewer
     */
    void updateVisibility(@NotNull Viewer viewer);

    /**
     * Gets a viewer by UUID.
     *
     * @param uuid the viewer's UUID
     * @return the viewer, or null if not found/offline
     */
    Viewer viewer(@NotNull UUID uuid);

    /**
     * Gets all currently online viewers.
     *
     * @return collection of online viewers
     */
    @NotNull Collection<Viewer> onlineViewers();
}
