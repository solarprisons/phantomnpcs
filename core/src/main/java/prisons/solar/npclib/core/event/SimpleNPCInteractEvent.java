package prisons.solar.npclib.core.event;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.npc.NPCInteractEvent;
import prisons.solar.npclib.api.interaction.InteractionHandler;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.viewer.Viewer;

import java.time.Instant;

/**
 * Simple implementation of {@link NPCInteractEvent}.
 */
public class SimpleNPCInteractEvent implements NPCInteractEvent {

    private final NPC<?> npc;
    private final Viewer viewer;
    private final InteractionHandler.ClickType clickType;
    private final InteractionHandler.Hand hand;
    private boolean cancelled;
    private final Instant timestamp;

    public SimpleNPCInteractEvent(@NotNull NPC<?> npc,
                                   @NotNull Viewer viewer,
                                   @NotNull InteractionHandler.ClickType clickType,
                                   @NotNull InteractionHandler.Hand hand) {
        this.npc = npc;
        this.viewer = viewer;
        this.clickType = clickType;
        this.hand = hand;
        this.timestamp = Instant.now();
    }

    @Override
    public @NotNull NPC<?> npc() {
        return npc;
    }

    @Override
    public @NotNull Viewer viewer() {
        return viewer;
    }

    @Override
    public @NotNull InteractionHandler.ClickType clickType() {
        return clickType;
    }

    @Override
    public @NotNull InteractionHandler.Hand hand() {
        return hand;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull Instant timestamp() {
        return timestamp;
    }
}
