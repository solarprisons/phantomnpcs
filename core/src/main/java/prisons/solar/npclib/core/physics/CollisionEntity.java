package prisons.solar.npclib.core.physics;

import prisons.solar.npclib.api.npc.Position;

import java.util.UUID;

public class CollisionEntity {
    private final UUID id;
    private Position position;
    private AABB aabb;
    private final String worldId;

    public CollisionEntity(UUID id, Position position, AABB aabb, String worldId) {
        this.id = id;
        this.position = position;
        this.aabb = aabb;
        this.worldId = worldId;
    }

    public UUID getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public AABB getAabb() {
        return aabb;
    }

    public void setAabb(AABB aabb) {
        this.aabb = aabb;
    }

    public String getWorldId() {
        return worldId;
    }
}
