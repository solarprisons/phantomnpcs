package prisons.solar.npclib.api.appearance;

import org.jetbrains.annotations.NotNull;

/**
 * Appearance configuration for armor stand entities.
 * Extends LivingAppearance to support equipment.
 */
public interface ArmorStandAppearance extends LivingAppearance {

    /**
     * Sets whether this armor stand is small (baby size).
     *
     * @param small true for small
     */
    void setSmall(boolean small);

    /**
     * Returns whether this armor stand is small.
     *
     * @return true if small
     */
    boolean isSmall();

    /**
     * Sets whether this armor stand shows arms.
     *
     * @param showArms true to show arms
     */
    void setShowArms(boolean showArms);

    /**
     * Returns whether this armor stand shows arms.
     *
     * @return true if showing arms
     */
    boolean hasArms();

    /**
     * Sets whether this armor stand has a base plate.
     *
     * @param hasBasePlate true to show base plate
     */
    void setBasePlate(boolean hasBasePlate);

    /**
     * Returns whether this armor stand has a base plate.
     *
     * @return true if has base plate
     */
    boolean hasBasePlate();

    /**
     * Sets whether this armor stand is a marker (zero bounding box, invisible).
     * Marker armor stands cannot be interacted with.
     *
     * @param marker true for marker
     */
    void setMarker(boolean marker);

    /**
     * Returns whether this armor stand is a marker.
     *
     * @return true if marker
     */
    boolean isMarker();

    /**
     * Sets the head rotation.
     *
     * @param pitch pitch in degrees
     * @param yaw yaw in degrees
     * @param roll roll in degrees
     */
    void setHeadRotation(float pitch, float yaw, float roll);

    /**
     * Sets the body rotation.
     *
     * @param pitch pitch in degrees
     * @param yaw yaw in degrees
     * @param roll roll in degrees
     */
    void setBodyRotation(float pitch, float yaw, float roll);

    /**
     * Sets the left arm rotation.
     *
     * @param pitch pitch in degrees
     * @param yaw yaw in degrees
     * @param roll roll in degrees
     */
    void setLeftArmRotation(float pitch, float yaw, float roll);

    /**
     * Sets the right arm rotation.
     *
     * @param pitch pitch in degrees
     * @param yaw yaw in degrees
     * @param roll roll in degrees
     */
    void setRightArmRotation(float pitch, float yaw, float roll);

    /**
     * Sets the left leg rotation.
     *
     * @param pitch pitch in degrees
     * @param yaw yaw in degrees
     * @param roll roll in degrees
     */
    void setLeftLegRotation(float pitch, float yaw, float roll);

    /**
     * Sets the right leg rotation.
     *
     * @param pitch pitch in degrees
     * @param yaw yaw in degrees
     * @param roll roll in degrees
     */
    void setRightLegRotation(float pitch, float yaw, float roll);

    /**
     * Represents a 3D rotation (Euler angles).
     */
    record Rotation(float pitch, float yaw, float roll) {
        public static final Rotation ZERO = new Rotation(0, 0, 0);
    }

    /**
     * Gets the head rotation.
     *
     * @return the head rotation
     */
    @NotNull Rotation getHeadRotation();

    /**
     * Gets the body rotation.
     *
     * @return the body rotation
     */
    @NotNull Rotation getBodyRotation();

    /**
     * Gets the left arm rotation.
     *
     * @return the left arm rotation
     */
    @NotNull Rotation getLeftArmRotation();

    /**
     * Gets the right arm rotation.
     *
     * @return the right arm rotation
     */
    @NotNull Rotation getRightArmRotation();

    /**
     * Gets the left leg rotation.
     *
     * @return the left leg rotation
     */
    @NotNull Rotation getLeftLegRotation();

    /**
     * Gets the right leg rotation.
     *
     * @return the right leg rotation
     */
    @NotNull Rotation getRightLegRotation();
}
