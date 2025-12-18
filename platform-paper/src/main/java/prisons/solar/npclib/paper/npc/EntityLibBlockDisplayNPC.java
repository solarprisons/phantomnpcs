package prisons.solar.npclib.paper.npc;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.BlockDisplayMeta;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.appearance.BlockDisplayAppearance;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.npc.Position;

/**
 * Block display NPC implementation using EntityLib.
 */
public class EntityLibBlockDisplayNPC extends AbstractEntityLibDisplayNPC<BlockDisplayAppearance> {

    private final EntityLibBlockDisplayAppearance blockDisplayAppearance;

    public EntityLibBlockDisplayNPC(@NotNull Position position) {
        super(EntityType.BLOCK_DISPLAY, position);
        this.blockDisplayAppearance = new EntityLibBlockDisplayAppearance(this);
        this.appearance = blockDisplayAppearance;
    }

    @Override
    protected com.github.retrooper.packetevents.protocol.entity.type.EntityType getPacketEventsEntityType() {
        return EntityTypes.BLOCK_DISPLAY;
    }

    @Override
    protected void applySpecificMeta(AbstractDisplayMeta baseMeta) {
        if (!(baseMeta instanceof BlockDisplayMeta meta)) {
            return;
        }

        Object block = blockDisplayAppearance.getBlock();
        if (block instanceof BlockData bukkitBlockData) {
            WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(bukkitBlockData);
            meta.setBlockState(state);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void syncMetadataToWrapper() {
        if (wrapperEntity == null) {
            return;
        }

        wrapperEntity.consumeEntityMeta(BlockDisplayMeta.class, meta -> {
            // Base entity flags
            meta.setOnFire(blockDisplayAppearance.isOnFire());
            meta.setInvisible(blockDisplayAppearance.isInvisible());
            meta.setGlowing(blockDisplayAppearance.isGlowing());

            // Display common properties
            meta.setBillboardConstraints(convertBillboard(blockDisplayAppearance.getBillboardMode()));
            meta.setTransformationInterpolationDuration(blockDisplayAppearance.getInterpolationDuration());
            meta.setInterpolationDelay(blockDisplayAppearance.getInterpolationDelay());
            meta.setViewRange(blockDisplayAppearance.getViewRange());
            meta.setShadowRadius(blockDisplayAppearance.getShadowRadius());
            meta.setShadowStrength(blockDisplayAppearance.getShadowStrength());
            meta.setWidth(blockDisplayAppearance.getDisplayWidth());
            meta.setHeight(blockDisplayAppearance.getDisplayHeight());

            // Block-specific properties
            Object block = blockDisplayAppearance.getBlock();
            if (block instanceof BlockData bukkitBlockData) {
                WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(bukkitBlockData);
                meta.setBlockState(state);
            }
        });

        wrapperEntity.refresh();
    }

    private AbstractDisplayMeta.BillboardConstraints convertBillboard(
            prisons.solar.npclib.api.appearance.DisplayAppearance.BillboardMode mode) {
        return switch (mode) {
            case FIXED -> AbstractDisplayMeta.BillboardConstraints.FIXED;
            case VERTICAL -> AbstractDisplayMeta.BillboardConstraints.VERTICAL;
            case HORIZONTAL -> AbstractDisplayMeta.BillboardConstraints.HORIZONTAL;
            case CENTER -> AbstractDisplayMeta.BillboardConstraints.CENTER;
        };
    }
}
