package prisons.solar.npclib.api.physics;

import prisons.solar.npclib.api.math.Vector3d;

public interface PhysicsComponent {
    Vector3d velocity();
    void setVelocity(Vector3d velocity);
    void addVelocity(Vector3d delta);

    Vector3d acceleration();
    void setAcceleration(Vector3d acceleration);

    boolean hasGravity();
    void setGravity(boolean gravity);
    float gravityMultiplier();
    void setGravityMultiplier(float multiplier);

    float friction();
    void setFriction(float friction);

    float airResistance();
    void setAirResistance(float airResistance);

    boolean isOnGround();
    boolean isInWater();
    boolean isInLava();

    void applyForce(Vector3d force);
    void applyImpulse(Vector3d impulse);

    void tick(float deltaTime);
}
