package prisons.solar.npclib.api.persistence;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SPI for persisting NPCs to storage.
 * Implementations can save NPCs to files, databases, etc.
 */
public interface NPCPersistence {

    /**
     * Saves an NPC to storage.
     *
     * @param npc the NPC to save
     * @return future that completes when saved
     */
    CompletableFuture<Void> save(@NotNull NPC<?> npc);

    /**
     * Saves all NPCs to storage.
     *
     * @param npcs the NPCs to save
     * @return future that completes when all saved
     */
    CompletableFuture<Void> saveAll(@NotNull Collection<NPC<?>> npcs);

    /**
     * Loads an NPC from storage.
     *
     * @param id the NPC ID
     * @return future containing the loaded NPC data, or empty if not found
     */
    CompletableFuture<Optional<NPCData>> load(@NotNull UUID id);

    /**
     * Loads all NPCs from storage.
     *
     * @return future containing all loaded NPC data
     */
    CompletableFuture<Collection<NPCData>> loadAll();

    /**
     * Deletes an NPC from storage.
     *
     * @param id the NPC ID
     * @return future that completes when deleted
     */
    CompletableFuture<Void> delete(@NotNull UUID id);

    /**
     * Checks if an NPC exists in storage.
     *
     * @param id the NPC ID
     * @return future containing true if exists
     */
    CompletableFuture<Boolean> exists(@NotNull UUID id);
}
