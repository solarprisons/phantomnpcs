package prisons.solar.npclib.paper.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.NPCRegistry;
import prisons.solar.npclib.core.physics.CollisionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Listens for world unload events and cleans up NPCs in unloaded worlds.
 * This prevents memory leaks and references to invalid worlds.
 */
public class WorldUnloadListener implements Listener {

    private final NPCRegistry npcRegistry;
    private final CollisionManager collisionManager;

    public WorldUnloadListener(NPCRegistry npcRegistry, CollisionManager collisionManager) {
        this.npcRegistry = npcRegistry;
        this.collisionManager = collisionManager;
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        String worldId = event.getWorld().getUID().toString();

        // Find all NPCs in this world
        List<NPC<?>> npcsToDestroy = new ArrayList<>();
        for (NPC<?> npc : npcRegistry.all()) {
            if (npc.getPosition().worldId().equals(worldId)) {
                npcsToDestroy.add(npc);
            }
        }

        // Destroy NPCs in unloaded world
        for (NPC<?> npc : npcsToDestroy) {
            npcRegistry.unregister(npc);
        }

        // Clean up collision manager spatial data for this world
        int removedEntities = collisionManager.cleanupWorld(worldId);

        if (!npcsToDestroy.isEmpty() || removedEntities > 0) {
            System.out.println(String.format(
                    "[PhantomNPC] World '%s' unloaded: removed %d NPCs, cleaned up %d collision entities",
                    event.getWorld().getName(),
                    npcsToDestroy.size(),
                    removedEntities
            ));
        }
    }
}
