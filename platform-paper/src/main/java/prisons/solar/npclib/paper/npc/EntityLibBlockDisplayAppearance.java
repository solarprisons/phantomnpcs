package prisons.solar.npclib.paper.npc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.BlockDisplayAppearance;

/**
 * EntityLib implementation of block display appearance.
 */
public class EntityLibBlockDisplayAppearance extends AbstractEntityLibDisplayAppearance<EntityLibBlockDisplayNPC>
        implements BlockDisplayAppearance {

    private Object blockData;

    public EntityLibBlockDisplayAppearance(@NotNull EntityLibBlockDisplayNPC npc) {
        super(npc);
    }

    @Override
    public void setBlock(@NotNull Object blockData) {
        this.blockData = blockData;
        npc.onMetadataChanged();
    }

    @Override
    public @Nullable Object getBlock() {
        return blockData;
    }
}
