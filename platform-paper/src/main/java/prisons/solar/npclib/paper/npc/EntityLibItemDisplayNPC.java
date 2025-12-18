package prisons.solar.npclib.paper.npc;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.ItemDisplayMeta;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.appearance.DisplayAppearance;
import prisons.solar.npclib.api.appearance.ItemDisplayAppearance;
import prisons.solar.npclib.api.npc.Position;

public class EntityLibItemDisplayNPC extends AbstractEntityLibDisplayNPC<ItemDisplayAppearance> {

    private final EntityLibItemDisplayAppearance itemDisplayAppearance;

    public EntityLibItemDisplayNPC(@NotNull Position position) {
        super(prisons.solar.npclib.api.entity.EntityType.ITEM_DISPLAY, position);
        this.itemDisplayAppearance = new EntityLibItemDisplayAppearance(this);
        this.appearance = itemDisplayAppearance;
    }

    @Override
    protected EntityType getPacketEventsEntityType() {
        return EntityTypes.ITEM_DISPLAY;
    }

    @Override
    protected void applySpecificMeta(AbstractDisplayMeta baseMeta) {
        if (!(baseMeta instanceof ItemDisplayMeta meta)) {
            return;
        }

        Object item = itemDisplayAppearance.getItem();
        if (item instanceof org.bukkit.inventory.ItemStack bukkitItem) {
            ItemStack peItem = SpigotConversionUtil.fromBukkitItemStack(bukkitItem);
            meta.setItem(peItem);
        }

        meta.setDisplayType(convertDisplayTransform(itemDisplayAppearance.getDisplayTransform()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void syncMetadataToWrapper() {
        if (wrapperEntity == null) {
            return;
        }

        wrapperEntity.consumeEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setOnFire(itemDisplayAppearance.isOnFire());
            meta.setInvisible(itemDisplayAppearance.isInvisible());
            meta.setGlowing(itemDisplayAppearance.isGlowing());

            meta.setBillboardConstraints(convertBillboard(itemDisplayAppearance.getBillboardMode()));
            meta.setTransformationInterpolationDuration(itemDisplayAppearance.getInterpolationDuration());
            meta.setInterpolationDelay(itemDisplayAppearance.getInterpolationDelay());
            meta.setViewRange(itemDisplayAppearance.getViewRange());
            meta.setShadowRadius(itemDisplayAppearance.getShadowRadius());
            meta.setShadowStrength(itemDisplayAppearance.getShadowStrength());
            meta.setWidth(itemDisplayAppearance.getDisplayWidth());
            meta.setHeight(itemDisplayAppearance.getDisplayHeight());

            Object item = itemDisplayAppearance.getItem();
            if (item instanceof org.bukkit.inventory.ItemStack bukkitItem) {
                ItemStack peItem = SpigotConversionUtil.fromBukkitItemStack(bukkitItem);
                meta.setItem(peItem);
            }

            meta.setDisplayType(convertDisplayTransform(itemDisplayAppearance.getDisplayTransform()));
        });

        wrapperEntity.refresh();
    }

    private ItemDisplayMeta.DisplayType convertDisplayTransform(ItemDisplayAppearance.DisplayTransform transform) {
        return switch (transform) {
            case NONE -> ItemDisplayMeta.DisplayType.NONE;
            case THIRDPERSON_LEFTHAND -> ItemDisplayMeta.DisplayType.THIRD_PERSON_LEFT_HAND;
            case THIRDPERSON_RIGHTHAND -> ItemDisplayMeta.DisplayType.THIRD_PERSON_RIGHT_HAND;
            case FIRSTPERSON_LEFTHAND -> ItemDisplayMeta.DisplayType.FIRST_PERSON_LEFT_HAND;
            case FIRSTPERSON_RIGHTHAND -> ItemDisplayMeta.DisplayType.FIRST_PERSON_RIGHT_HAND;
            case HEAD -> ItemDisplayMeta.DisplayType.HEAD;
            case GUI -> ItemDisplayMeta.DisplayType.GUI;
            case GROUND -> ItemDisplayMeta.DisplayType.GROUND;
            case FIXED -> ItemDisplayMeta.DisplayType.FIXED;
        };
    }

    private AbstractDisplayMeta.BillboardConstraints convertBillboard(DisplayAppearance.BillboardMode mode) {
        return switch (mode) {
            case FIXED -> AbstractDisplayMeta.BillboardConstraints.FIXED;
            case VERTICAL -> AbstractDisplayMeta.BillboardConstraints.VERTICAL;
            case HORIZONTAL -> AbstractDisplayMeta.BillboardConstraints.HORIZONTAL;
            case CENTER -> AbstractDisplayMeta.BillboardConstraints.CENTER;
        };
    }
}
