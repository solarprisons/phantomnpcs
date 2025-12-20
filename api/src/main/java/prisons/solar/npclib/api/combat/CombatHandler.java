package prisons.solar.npclib.api.combat;

import prisons.solar.npclib.api.npc.NPC;

public interface CombatHandler {
    CombatComponent.AttackResult handleAttack(Combatant attacker, Combatant target);
    boolean canAttack(Combatant attacker, Combatant target);
    void onCombatStart(Combatant npc, Combatant target);
    void onCombatEnd(Combatant npc);
}
