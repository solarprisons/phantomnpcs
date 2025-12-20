package prisons.solar.npclib.api.combat;

import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.health.DamageResult;

public class CombatComponent {
    public record AttackResult(boolean success, @Nullable DamageResult damageResult, String failureReason) {}
}
