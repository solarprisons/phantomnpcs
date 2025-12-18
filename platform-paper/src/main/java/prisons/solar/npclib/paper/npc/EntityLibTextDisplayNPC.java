package prisons.solar.npclib.paper.npc;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.appearance.TextDisplayAppearance;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.npc.Position;

/**
 * Text display NPC implementation using EntityLib.
 */
public class EntityLibTextDisplayNPC extends AbstractEntityLibDisplayNPC<TextDisplayAppearance> {

    private final EntityLibTextDisplayAppearance textDisplayAppearance;

    public EntityLibTextDisplayNPC(@NotNull Position position) {
        super(EntityType.TEXT_DISPLAY, position);
        this.textDisplayAppearance = new EntityLibTextDisplayAppearance(this);
        this.appearance = textDisplayAppearance;
    }

    @Override
    protected com.github.retrooper.packetevents.protocol.entity.type.EntityType getPacketEventsEntityType() {
        return EntityTypes.TEXT_DISPLAY;
    }

    @Override
    protected void applySpecificMeta(AbstractDisplayMeta baseMeta) {
        if (!(baseMeta instanceof TextDisplayMeta meta)) {
            return;
        }

        // Text content - parse legacy color codes
        String text = textDisplayAppearance.getText();
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        meta.setText(component);

        // Text properties
        meta.setLineWidth(textDisplayAppearance.getLineWidth());
        meta.setBackgroundColor(textDisplayAppearance.getBackgroundColor());
        meta.setTextOpacity((byte) textDisplayAppearance.getTextOpacity());
        meta.setShadow(textDisplayAppearance.hasShadow());
        meta.setSeeThrough(textDisplayAppearance.isSeeThrough());
        meta.setUseDefaultBackground(textDisplayAppearance.hasDefaultBackground());

        // Alignment
        TextDisplayAppearance.TextAlignment alignment = textDisplayAppearance.getAlignment();
        meta.setAlignLeft(alignment == TextDisplayAppearance.TextAlignment.LEFT);
        meta.setAlignRight(alignment == TextDisplayAppearance.TextAlignment.RIGHT);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void syncMetadataToWrapper() {
        if (wrapperEntity == null) {
            return;
        }

        wrapperEntity.consumeEntityMeta(TextDisplayMeta.class, meta -> {
            // Base entity flags
            meta.setOnFire(textDisplayAppearance.isOnFire());
            meta.setInvisible(textDisplayAppearance.isInvisible());
            meta.setGlowing(textDisplayAppearance.isGlowing());

            // Display common properties
            meta.setBillboardConstraints(convertBillboard(textDisplayAppearance.getBillboardMode()));
            meta.setTransformationInterpolationDuration(textDisplayAppearance.getInterpolationDuration());
            meta.setInterpolationDelay(textDisplayAppearance.getInterpolationDelay());
            meta.setViewRange(textDisplayAppearance.getViewRange());
            meta.setShadowRadius(textDisplayAppearance.getShadowRadius());
            meta.setShadowStrength(textDisplayAppearance.getShadowStrength());
            meta.setWidth(textDisplayAppearance.getDisplayWidth());
            meta.setHeight(textDisplayAppearance.getDisplayHeight());

            // Text-specific properties
            String text = textDisplayAppearance.getText();
            Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
            meta.setText(component);
            meta.setLineWidth(textDisplayAppearance.getLineWidth());
            meta.setBackgroundColor(textDisplayAppearance.getBackgroundColor());
            meta.setTextOpacity((byte) textDisplayAppearance.getTextOpacity());
            meta.setShadow(textDisplayAppearance.hasShadow());
            meta.setSeeThrough(textDisplayAppearance.isSeeThrough());
            meta.setUseDefaultBackground(textDisplayAppearance.hasDefaultBackground());

            // Alignment
            TextDisplayAppearance.TextAlignment alignment = textDisplayAppearance.getAlignment();
            meta.setAlignLeft(alignment == TextDisplayAppearance.TextAlignment.LEFT);
            meta.setAlignRight(alignment == TextDisplayAppearance.TextAlignment.RIGHT);
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
