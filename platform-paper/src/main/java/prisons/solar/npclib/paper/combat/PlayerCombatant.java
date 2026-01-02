package prisons.solar.npclib.paper.combat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.combat.Combatant;
import prisons.solar.npclib.api.health.HealthComponent;
import prisons.solar.npclib.api.npc.Position;

import java.util.UUID;

public class PlayerCombatant implements Combatant {

    /**
     * Checks if a player is performing a critical hit based on vanilla 1.9+ mechanics.
     *
     * <p>A critical hit occurs when all of the following conditions are met:
     * <ul>
     *   <li>Player is falling (fall distance greater than 0)</li>
     *   <li>Player is not on the ground</li>
     *   <li>Player is not in water</li>
     *   <li>Player is not inside a vehicle</li>
     *   <li>Player is not climbing (ladders, vines, etc.)</li>
     *   <li>Player is not sprinting</li>
     *   <li>Player does not have blindness effect</li>
     *   <li>Player's attack cooldown is at least 90%</li>
     * </ul>
     *
     * @param player the player to check
     * @return true if the player's next attack would be a critical hit
     */
    public static boolean isCriticalHit(@NotNull Player player) {
        return player.getFallDistance() > 0
                && !player.isOnGround()
                && !player.isInWater()
                && !player.isInsideVehicle()
                && !player.isClimbing()
                && !player.isSprinting()
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS)
                && player.getAttackCooldown() >= 0.9f;
    }

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
