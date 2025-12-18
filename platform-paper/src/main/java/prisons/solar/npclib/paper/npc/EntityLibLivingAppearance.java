package prisons.solar.npclib.paper.npc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.LivingAppearance;

/**
 * Living appearance implementation for EntityLib-based NPCs.
 */
public class EntityLibLivingAppearance implements LivingAppearance {

    private final EntityLibLivingNPC npc;

    private String customName = "";
    private boolean customNameVisible = false;
    private boolean glowing = false;
    private boolean invisible = false;
    private boolean onFire = false;
    private EntityPose pose = EntityPose.STANDING;
    private final Object[] equipment = new Object[6];

    public EntityLibLivingAppearance(@NotNull EntityLibLivingNPC npc) {
        this.npc = npc;
    }

    @Override
    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
        markDirty();
    }

    @Override
    public boolean isGlowing() {
        return glowing;
    }

    @Override
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
        markDirty();
    }

    @Override
    public boolean isInvisible() {
        return invisible;
    }

    @Override
    public void setOnFire(boolean onFire) {
        this.onFire = onFire;
        markDirty();
    }

    @Override
    public boolean isOnFire() {
        return onFire;
    }

    @Override
    public void setCustomNameVisible(boolean visible) {
        this.customNameVisible = visible;
        markDirty();
    }

    @Override
    public boolean isCustomNameVisible() {
        return customNameVisible;
    }

    @Override
    public void setCustomName(@NotNull String name) {
        this.customName = name;
        markDirty();
    }

    @Override
    public @NotNull String getCustomName() {
        return customName;
    }

    @Override
    public void setEquipment(@NotNull EquipmentSlot slot, @Nullable Object item) {
        equipment[slot.ordinal()] = item;
        npc.syncEquipment();
    }

    @Override
    public @Nullable Object getEquipment(@NotNull EquipmentSlot slot) {
        return equipment[slot.ordinal()];
    }

    @Override
    public void clearEquipment() {
        for (int i = 0; i < equipment.length; i++) {
            equipment[i] = null;
        }
        npc.syncEquipment();
    }

    @Override
    public void setPose(@NotNull EntityPose pose) {
        this.pose = pose;
        markDirty();
    }

    @Override
    public @NotNull EntityPose getPose() {
        return pose;
    }

    @Override
    public void markDirty() {
        npc.syncMetadataToWrapper();
    }
}
