package prisons.solar.npclib.core.combat;

import prisons.solar.npclib.api.combat.CombatComponent;
import prisons.solar.npclib.api.combat.CombatHandler;
import prisons.solar.npclib.api.combat.Combatant;
import prisons.solar.npclib.api.health.HealthComponent;

public class DefaultCombatHandler implements CombatHandler {

    @Override
    public CombatComponent.AttackResult handleAttack(Combatant attacker, Combatant target, float baseDamage) {
        float finalDamage = baseDamage;

        HealthComponent.DamageSource source = new HealthComponent.DamageSource(
                HealthComponent.DamageType.ENTITY_ATTACK,
                attacker,
                null,
                target.getPosition(),
                false // Critical hits should be calculated at the interaction layer
        );

        HealthComponent h = target.healthComponent();
        HealthComponent.DamageResult result = h.damage(source, finalDamage);

        return new CombatComponent.AttackResult(true, result, "");
    }

    @Override
    public boolean canAttack(Combatant attacker, Combatant target) {
        return !target.healthComponent().isDead();
    }

    @Override
    public void onCombatStart(Combatant npc, Combatant target) {
        // No-op: Override in subclass to handle combat start events
    }

    @Override
    public void onCombatEnd(Combatant npc) {
        // No-op: Override in subclass to handle combat end events
    }
}
