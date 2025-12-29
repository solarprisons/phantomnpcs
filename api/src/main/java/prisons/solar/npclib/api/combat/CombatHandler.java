package prisons.solar.npclib.api.combat;

import prisons.solar.npclib.api.npc.NPC;

public interface CombatHandler {
    CombatComponent.AttackResult handleAttack(Combatant attacker, Combatant target, float baseDamage);
    boolean canAttack(Combatant attacker, Combatant target);
    void onCombatStart(Combatant npc, Combatant target);
    void onCombatEnd(Combatant npc);
}
