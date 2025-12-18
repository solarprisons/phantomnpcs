package prisons.solar.npclib.protocol.packet;

/**
 * Types of packets used for NPC rendering.
 */
public enum PacketType {
    // Entity spawning
    SPAWN_PLAYER,
    SPAWN_ENTITY,
    SPAWN_EXPERIENCE_ORB,

    // Entity removal
    ENTITY_DESTROY,

    // Player info (tablist)
    PLAYER_INFO_ADD,
    PLAYER_INFO_REMOVE,
    PLAYER_INFO_UPDATE,

    // Entity movement
    ENTITY_RELATIVE_MOVE,
    ENTITY_RELATIVE_MOVE_LOOK,
    ENTITY_LOOK,
    ENTITY_TELEPORT,
    ENTITY_HEAD_ROTATION,
    ENTITY_VELOCITY,

    // Entity state
    ENTITY_METADATA,
    ENTITY_EQUIPMENT,
    ENTITY_ANIMATION,
    ENTITY_STATUS,
    ENTITY_ATTRIBUTES,

    // Display entities
    SET_PASSENGERS,

    // World
    BLOCK_BREAK_ANIMATION,
    WORLD_EVENT
}
