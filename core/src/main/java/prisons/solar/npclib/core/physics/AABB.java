package prisons.solar.npclib.core.physics;

import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.math.Vector3d;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.world.BlockPosition;

public record AABB(
    double minX, double minY, double minZ,
    double maxX, double maxY, double maxZ
) {
    public boolean intersects(AABB other) {
        return minX < other.maxX() && maxX > other.minX &&
                minY < other.maxY && maxY > other.minY &&
                minZ < other.maxZ && maxZ > other.minZ;
    }

    public AABB offset(Vector3d offset) {
        return new AABB(
                minX + offset.getX(), minY + offset.getY(), minZ + offset.getZ(),
                maxX + offset.getX(), maxY + offset.getY(), maxZ + offset.getZ()
        );
    }

    public Vector3d center() {
        return new Vector3d(
                (minX + maxX) / 2,
                (minY + maxY) / 2,
                (minZ + maxZ) / 2
        );
    }

    public static AABB fromBlock(BlockPosition pos) {
        return new AABB(
                pos.x(), pos.y(), pos.z(),
                pos.x() + 1.0, pos.y() + 1.0, pos.z() + 1.0
        );
    }

    public static AABB fromEntityType(EntityType t, Position p) {
        // No epsilon - entities should stand directly on blocks
        // Physics engine handles collision resolution properly
        return switch (t) {
            case PLAYER -> new AABB(
                    p.x() - 0.3, p.y(), p.z() - 0.3,
                    p.x() + 0.3, p.y() + 1.8, p.z() + 0.3
            );
            case ZOMBIE, SKELETON -> new AABB(
                    p.x() - 0.3, p.y(), p.z() - 0.3,
                    p.x() + 0.3, p.y() + 1.95, p.z() + 0.3
            );
            default -> new AABB(
                    p.x() - 0.5, p.y(), p.z() - 0.5,
                    p.x() + 0.5, p.y() + 1.0, p.z() + 0.5
            );
        };
    }
}