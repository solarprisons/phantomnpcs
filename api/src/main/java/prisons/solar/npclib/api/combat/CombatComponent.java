package prisons.solar.npclib.api.combat;

import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.health.HealthComponent;

public class CombatComponent {
    public record AttackResult(boolean success, @Nullable HealthComponent.DamageResult damageResult, String failureReason) {}
}
