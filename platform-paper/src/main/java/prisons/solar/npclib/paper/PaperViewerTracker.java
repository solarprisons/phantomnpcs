package prisons.solar.npclib.paper;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.NPCRegistry;
import prisons.solar.npclib.api.npc.NPCState;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.api.viewer.ViewerTracker;
import prisons.solar.npclib.api.viewer.VisibilityRule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paper implementation of {@link ViewerTracker}.
 */
public class PaperViewerTracker implements ViewerTracker {

    private final PaperPlatformAdapter platform;
    private final NPCRegistry registry;
    private final double viewDistance;
    private final double viewDistanceSquared;

    private final Set<String> manuallyHidden = ConcurrentHashMap.newKeySet();
    private final Set<String> manuallyShown = ConcurrentHashMap.newKeySet();

    private VisibilityRule globalRule;

    public PaperViewerTracker(@NotNull PaperPlatformAdapter platform,
                               @NotNull NPCRegistry registry,
                               double viewDistance) {
        this.platform = platform;
        this.registry = registry;
        this.viewDistance = viewDistance;
        this.viewDistanceSquared = viewDistance * viewDistance;
        this.globalRule = VisibilityRule.withinDistance(viewDistance)
                .and(this::sameWorld);
    }

    private boolean sameWorld(Viewer viewer, NPC<?> npc) {
        return viewer.position().worldId().equals(npc.position().worldId());
    }

    @Override
    public @NotNull Collection<Viewer> viewers(@NotNull NPC<?> npc) {
        return npc.viewers();
    }

    @Override
    public @NotNull Collection<NPC<?>> visibleTo(@NotNull Viewer viewer) {
        List<NPC<?>> visible = new ArrayList<>();
        for (NPC<?> npc : registry.all()) {
            if (npc.isVisibleTo(viewer)) {
                visible.add(npc);
            }
        }
        return Collections.unmodifiableList(visible);
    }

    @Override
    public boolean canSee(@NotNull Viewer viewer, @NotNull NPC<?> npc) {
        return npc.isVisibleTo(viewer);
    }

    @Override
    public void show(@NotNull NPC<?> npc, @NotNull Viewer viewer) {
        String key = viewerNpcKey(viewer, npc);
        manuallyHidden.remove(key);
        manuallyShown.add(key);

        if (!npc.isVisibleTo(viewer) && npc.state() == NPCState.SPAWNED) {
            npc.spawn(viewer);
        }
    }

    @Override
    public void hide(@NotNull NPC<?> npc, @NotNull Viewer viewer) {
        String key = viewerNpcKey(viewer, npc);
        manuallyShown.remove(key);
        manuallyHidden.add(key);

        if (npc.isVisibleTo(viewer)) {
            npc.despawn(viewer);
        }
    }

    @Override
    public void updateVisibility(@NotNull NPC<?> npc) {
        if (!canUpdateVisibility(npc)) {
            return;
        }

        for (Viewer viewer : platform.onlineViewers()) {
            updateVisibility(viewer, npc);
        }
    }

    @Override
    public void updateVisibility(@NotNull Viewer viewer) {
        for (NPC<?> npc : registry.all()) {
            if (canUpdateVisibility(npc)) {
                updateVisibility(viewer, npc);
            }
        }
    }

    private boolean canUpdateVisibility(NPC<?> npc) {
        NPCState state = npc.state();
        return state == NPCState.SPAWNED || state == NPCState.REGISTERED;
    }

    private void updateVisibility(@NotNull Viewer viewer, @NotNull NPC<?> npc) {
        String key = viewerNpcKey(viewer, npc);

        // Check manual overrides first
        if (manuallyHidden.contains(key)) {
            if (npc.isVisibleTo(viewer)) {
                npc.despawn(viewer);
            }
            return;
        }

        if (manuallyShown.contains(key)) {
            if (!npc.isVisibleTo(viewer)) {
                npc.spawn(viewer);
            }
            return;
        }

        // Apply global visibility rule
        boolean shouldBeVisible = globalRule.test(viewer, npc);
        boolean isVisible = npc.isVisibleTo(viewer);

        if (shouldBeVisible && !isVisible) {
            npc.spawn(viewer);
        } else if (!shouldBeVisible && isVisible) {
            npc.despawn(viewer);
        }
    }

    @Override
    public Viewer viewer(@NotNull UUID uuid) {
        return platform.viewer(uuid);
    }

    @Override
    public @NotNull Collection<Viewer> onlineViewers() {
        return platform.onlineViewers();
    }

    /**
     * Sets the global visibility rule.
     *
     * @param rule the visibility rule
     */
    public void setGlobalRule(@NotNull VisibilityRule rule) {
        this.globalRule = rule;
    }

    /**
     * Gets the global visibility rule.
     *
     * @return the visibility rule
     */
    public @NotNull VisibilityRule getGlobalRule() {
        return globalRule;
    }

    /**
     * Gets the configured view distance.
     *
     * @return view distance in blocks
     */
    public double getViewDistance() {
        return viewDistance;
    }

    /**
     * Clears manual visibility overrides for a viewer.
     *
     * @param viewer the viewer
     */
    public void clearManualOverrides(@NotNull Viewer viewer) {
        String prefix = viewer.id().toString() + ":";
        manuallyHidden.removeIf(key -> key.startsWith(prefix));
        manuallyShown.removeIf(key -> key.startsWith(prefix));
    }

    /**
     * Clears manual visibility overrides for an NPC.
     *
     * @param npc the NPC
     */
    public void clearManualOverrides(@NotNull NPC<?> npc) {
        String suffix = ":" + npc.id().toString();
        manuallyHidden.removeIf(key -> key.endsWith(suffix));
        manuallyShown.removeIf(key -> key.endsWith(suffix));
    }

    private String viewerNpcKey(Viewer viewer, NPC<?> npc) {
        return viewer.id().toString() + ":" + npc.id().toString();
    }
}
