package prisons.solar.npclib.core.combat;

import prisons.solar.npclib.api.combat.Combatant;
import prisons.solar.npclib.api.combat.DamageCalculator;
import prisons.solar.npclib.api.health.HealthComponent;

public class DefaultDamageCalculator implements DamageCalculator {
    @Override
    public float calculateDamage(HealthComponent.DamageSource source, float baseDamage, Combatant target) {
        return baseDamage;
    }

    @Override
    public float applyArmorReduction(float damage, Combatant target) {
        return damage;
    }

    @Override
    public float applyEffects(float damage, Combatant target) {
        return damage;
    }
}
