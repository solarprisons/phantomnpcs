package prisons.solar.npclib.paper.combat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.combat.Combatant;
import prisons.solar.npclib.api.health.HealthComponent;
import prisons.solar.npclib.api.npc.Position;

import java.util.UUID;

public class PlayerCombatant implements Combatant {
    private final @NotNull Player player;
    private final HealthComponent health;

    public PlayerCombatant(@NotNull Player player, HealthComponent health) {
        this.player = player;
        this.health = health;
    }

    @Override
    public UUID getId() {
        return player.getUniqueId();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public Position getPosition() {
        Location l = player.getLocation();
        return Position.of(l.getWorld().getUID().toString(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    @Override
    public HealthComponent healthComponent() {
        return health;
    }

    @Override
    public boolean isBlocking() {
        return player.isBlocking();
    }
}
