package prisons.solar.npclib.core.npc;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.appearance.NPCAppearance;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.NPCRegistry;
import prisons.solar.npclib.api.npc.NPCState;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.platform.adapter.PlatformAdapter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Simple implementation of {@link NPCRegistry}.
 */
public class SimpleNPCRegistry implements NPCRegistry {

    private final Map<UUID, NPC<?>> npcsById = new ConcurrentHashMap<>();
    private final Map<Integer, NPC<?>> npcsByEntityId = new ConcurrentHashMap<>();
    private final PlatformAdapter platform;
    private final BiFunction<EntityType, Position, NPC<?>> npcFactory;

    public SimpleNPCRegistry(@NotNull PlatformAdapter platform,
                             @NotNull BiFunction<EntityType, Position, NPC<?>> npcFactory) {
        this.platform = platform;
        this.npcFactory = npcFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends NPCAppearance> @NotNull NPC<A> create(@NotNull EntityType type, @NotNull Position position) {
        return (NPC<A>) npcFactory.apply(type, position);
    }

    @Override
    public void register(@NotNull NPC<?> npc) {
        if (npc.state() != NPCState.UNREGISTERED) {
            throw new IllegalStateException("NPC is already registered");
        }

        if (npc instanceof AbstractNPC<?> abstractNpc) {
            int entityId = platform.allocateEntityId();
            abstractNpc.setEntityId(entityId);

            // Atomically transition from UNREGISTERED to REGISTERED
            if (!abstractNpc.compareAndSetState(NPCState.UNREGISTERED, NPCState.REGISTERED)) {
                throw new IllegalStateException("NPC state changed during registration");
            }

            npcsById.put(npc.getId(), npc);
            npcsByEntityId.put(entityId, npc);
        }
    }

    @Override
    public void unregister(@NotNull NPC<?> npc) {
        if (npc.state() == NPCState.UNREGISTERED || npc.state() == NPCState.DESTROYED) {
            return;
        }

        // Despawn from all viewers first
        npc.despawn();

        if (npc instanceof AbstractNPC<?> abstractNpc) {
            abstractNpc.setState(NPCState.UNREGISTERED);

            // Unregister goal selector from NPCEngine
            var services = abstractNpc.getServices();
            if (services != null) {
                services.get(prisons.solar.npclib.core.engine.NPCEngine.class).ifPresent(engine ->
                    engine.unregisterGoalSelector(npc)
                );
            }
        }

        npcsById.remove(npc.getId());
        npcsByEntityId.remove(npc.entityId());
    }

    @Override
    public @NotNull Optional<NPC<?>> byId(@NotNull UUID id) {
        return Optional.ofNullable(npcsById.get(id));
    }

    @Override
    public @NotNull Optional<NPC<?>> byEntityId(int entityId) {
        return Optional.ofNullable(npcsByEntityId.get(entityId));
    }

    @Override
    public @NotNull Collection<NPC<?>> byType(@NotNull EntityType type) {
        return npcsById.values().stream()
                .filter(npc -> npc.entityType() == type)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public @NotNull Collection<NPC<?>> all() {
        return Collections.unmodifiableCollection(npcsById.values());
    }

    @Override
    public @NotNull Collection<NPC<?>> inRange(@NotNull Position center, double radius) {
        double radiusSq = radius * radius;
        return npcsById.values().stream()
                .filter(npc -> npc.getPosition().distanceSquared(center) <= radiusSq)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public @NotNull Collection<NPC<?>> inWorld(@NotNull String worldId) {
        return npcsById.values().stream()
                .filter(npc -> npc.getPosition().worldId().equals(worldId))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public int count() {
        return npcsById.size();
    }
}
