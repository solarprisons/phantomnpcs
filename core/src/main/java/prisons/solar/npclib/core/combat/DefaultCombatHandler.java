package prisons.solar.npclib.core.combat;

import prisons.solar.npclib.api.combat.CombatComponent;
import prisons.solar.npclib.api.combat.CombatHandler;
import prisons.solar.npclib.api.combat.Combatant;

public class DefaultCombatHandler implements CombatHandler {
    @Override
    public CombatComponent.AttackResult handleAttack(Combatant attacker, Combatant target) {
        return null;
    }

    @Override
    public boolean canAttack(Combatant attacker, Combatant target) {
        return false;
    }

    @Override
    public void onCombatStart(Combatant npc, Combatant target) {

    }

    @Override
    public void onCombatEnd(Combatant npc) {

    }
}
