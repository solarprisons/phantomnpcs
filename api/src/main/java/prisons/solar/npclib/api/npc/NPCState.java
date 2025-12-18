package prisons.solar.npclib.api.npc;

/**
 * Represents the lifecycle state of an NPC.
 */
public enum NPCState {
    /**
     * NPC has been created but not yet registered with the engine.
     */
    UNREGISTERED,

    /**
     * NPC is registered with the engine but not spawned for any viewers.
     */
    REGISTERED,

    /**
     * NPC is spawned and visible to at least one viewer.
     */
    SPAWNED,

    /**
     * NPC was previously spawned but is now hidden from all viewers.
     */
    DESPAWNED,

    /**
     * NPC has been destroyed and cannot be reused.
     */
    DESTROYED
}
