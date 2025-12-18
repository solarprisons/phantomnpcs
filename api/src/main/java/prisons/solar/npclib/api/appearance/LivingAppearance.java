package prisons.solar.npclib.api.appearance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Appearance configuration for living entities (players, mobs).
 */
public interface LivingAppearance extends NPCAppearance {

    /**
     * Equipment slot for living entities.
     */
    enum EquipmentSlot {
        MAIN_HAND,
        OFF_HAND,
        HEAD,
        CHEST,
        LEGS,
        FEET
    }

    /**
     * Sets equipment in a slot.
     *
     * @param slot the equipment slot
     * @param item the item data (platform-specific, null to clear)
     */
    void setEquipment(@NotNull EquipmentSlot slot, @Nullable Object item);

    /**
     * Gets equipment in a slot.
     *
     * @param slot the equipment slot
     * @return the item data, or null if empty
     */
    @Nullable Object getEquipment(@NotNull EquipmentSlot slot);

    /**
     * Clears all equipment.
     */
    void clearEquipment();

    /**
     * Sets the entity pose.
     *
     * @param pose the pose
     */
    void setPose(@NotNull EntityPose pose);

    /**
     * Gets the current pose.
     *
     * @return the pose
     */
    @NotNull EntityPose getPose();

    /**
     * Entity pose states.
     */
    enum EntityPose {
        STANDING,
        FALL_FLYING,
        SLEEPING,
        SWIMMING,
        SPIN_ATTACK,
        SNEAKING,
        LONG_JUMPING,
        DYING,
        CROAKING,
        USING_TONGUE,
        SITTING,
        ROARING,
        SNIFFING,
        EMERGING,
        DIGGING,
        SLIDING,
        SHOOTING,
        INHALING
    }
}
