package prisons.solar.npclib.paper.npc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.ArmorStandAppearance;

import java.util.EnumMap;
import java.util.Map;

/**
 * EntityLib implementation of armor stand appearance.
 */
public class EntityLibArmorStandAppearance implements ArmorStandAppearance {

    private final EntityLibArmorStandNPC npc;
    private final Map<EquipmentSlot, Object> equipment = new EnumMap<>(EquipmentSlot.class);

    private boolean onFire;
    private boolean invisible;
    private boolean glowing;
    private String customName = "";
    private boolean customNameVisible;

    private boolean small;
    private boolean showArms;
    private boolean basePlate = true;
    private boolean marker;
    private EntityPose pose = EntityPose.STANDING;

    private Rotation headRotation = Rotation.ZERO;
    private Rotation bodyRotation = Rotation.ZERO;
    private Rotation leftArmRotation = Rotation.ZERO;
    private Rotation rightArmRotation = Rotation.ZERO;
    private Rotation leftLegRotation = Rotation.ZERO;
    private Rotation rightLegRotation = Rotation.ZERO;

    public EntityLibArmorStandAppearance(@NotNull EntityLibArmorStandNPC npc) {
        this.npc = npc;
    }

    @Override
    public void setOnFire(boolean onFire) {
        this.onFire = onFire;
        npc.onMetadataChanged();
    }

    @Override
    public boolean isOnFire() {
        return onFire;
    }

    @Override
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
        npc.onMetadataChanged();
    }

    @Override
    public boolean isInvisible() {
        return invisible;
    }

    @Override
    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
        npc.onMetadataChanged();
    }

    @Override
    public boolean isGlowing() {
        return glowing;
    }

    @Override
    public void setCustomName(@NotNull String name) {
        this.customName = name;
        npc.onMetadataChanged();
    }

    @Override
    public @NotNull String getCustomName() {
        return customName;
    }

    @Override
    public void setCustomNameVisible(boolean visible) {
        this.customNameVisible = visible;
        npc.onMetadataChanged();
    }

    @Override
    public boolean isCustomNameVisible() {
        return customNameVisible;
    }

    @Override
    public void setEquipment(@NotNull EquipmentSlot slot, @Nullable Object item) {
        if (item == null) {
            equipment.remove(slot);
        } else {
            equipment.put(slot, item);
        }
        npc.syncEquipment();
    }

    @Override
    public @Nullable Object getEquipment(@NotNull EquipmentSlot slot) {
        return equipment.get(slot);
    }

    @Override
    public void clearEquipment() {
        equipment.clear();
        npc.syncEquipment();
    }

    @Override
    public void setPose(@NotNull EntityPose pose) {
        this.pose = pose;
        npc.onMetadataChanged();
    }

    @Override
    public @NotNull EntityPose getPose() {
        return pose;
    }

    @Override
    public void setSmall(boolean small) {
        this.small = small;
        npc.onMetadataChanged();
    }

    @Override
    public boolean isSmall() {
        return small;
    }

    @Override
    public void setShowArms(boolean showArms) {
        this.showArms = showArms;
        npc.onMetadataChanged();
    }

    @Override
    public boolean hasArms() {
        return showArms;
    }

    @Override
    public void setBasePlate(boolean hasBasePlate) {
        this.basePlate = hasBasePlate;
        npc.onMetadataChanged();
    }

    @Override
    public boolean hasBasePlate() {
        return basePlate;
    }

    @Override
    public void setMarker(boolean marker) {
        this.marker = marker;
        npc.onMetadataChanged();
    }

    @Override
    public boolean isMarker() {
        return marker;
    }

    @Override
    public void setHeadRotation(float pitch, float yaw, float roll) {
        this.headRotation = new Rotation(pitch, yaw, roll);
        npc.onMetadataChanged();
    }

    @Override
    public void setBodyRotation(float pitch, float yaw, float roll) {
        this.bodyRotation = new Rotation(pitch, yaw, roll);
        npc.onMetadataChanged();
    }

    @Override
    public void setLeftArmRotation(float pitch, float yaw, float roll) {
        this.leftArmRotation = new Rotation(pitch, yaw, roll);
        npc.onMetadataChanged();
    }

    @Override
    public void setRightArmRotation(float pitch, float yaw, float roll) {
        this.rightArmRotation = new Rotation(pitch, yaw, roll);
        npc.onMetadataChanged();
    }

    @Override
    public void setLeftLegRotation(float pitch, float yaw, float roll) {
        this.leftLegRotation = new Rotation(pitch, yaw, roll);
        npc.onMetadataChanged();
    }

    @Override
    public void setRightLegRotation(float pitch, float yaw, float roll) {
        this.rightLegRotation = new Rotation(pitch, yaw, roll);
        npc.onMetadataChanged();
    }

    @Override
    public @NotNull Rotation getHeadRotation() {
        return headRotation;
    }

    @Override
    public @NotNull Rotation getBodyRotation() {
        return bodyRotation;
    }

    @Override
    public @NotNull Rotation getLeftArmRotation() {
        return leftArmRotation;
    }

    @Override
    public @NotNull Rotation getRightArmRotation() {
        return rightArmRotation;
    }

    @Override
    public @NotNull Rotation getLeftLegRotation() {
        return leftLegRotation;
    }

    @Override
    public @NotNull Rotation getRightLegRotation() {
        return rightLegRotation;
    }

    @Override
    public void markDirty() {
        npc.onMetadataChanged();
    }
}
