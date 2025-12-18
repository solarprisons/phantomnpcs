package prisons.solar.npclib.paper.npc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.ItemDisplayAppearance;

/**
 * EntityLib implementation of item display appearance.
 */
public class EntityLibItemDisplayAppearance extends AbstractEntityLibDisplayAppearance<EntityLibItemDisplayNPC>
        implements ItemDisplayAppearance {

    private Object item;
    private DisplayTransform displayTransform = DisplayTransform.NONE;

    public EntityLibItemDisplayAppearance(@NotNull EntityLibItemDisplayNPC npc) {
        super(npc);
    }

    @Override
    public void setItem(@NotNull Object item) {
        this.item = item;
        npc.onMetadataChanged();
    }

    @Override
    public @Nullable Object getItem() {
        return item;
    }

    @Override
    public void setDisplayTransform(@NotNull DisplayTransform transform) {
        this.displayTransform = transform;
        npc.onMetadataChanged();
    }

    @Override
    public @NotNull DisplayTransform getDisplayTransform() {
        return displayTransform;
    }
}
