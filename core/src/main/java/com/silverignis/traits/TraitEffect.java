package com.silverignis.traits;

import com.silverignis.skills.elements.Element;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.DamageSystem;
import com.silverignis.systems.combat.Trigger;
import com.silverignis.systems.combat.TriggerBus;
import com.silverignis.systems.combat.event.DamageEvent;
import com.silverignis.systems.combat.event.HealEvent;


public enum TraitEffect {

    ARCANE_MEMORY {
        public int slotCapacityBonus() {return  2;}
    },

    FLAME_RESISTANCE {
        public void onBattleStart(Combatant owner, TriggerBus bus, DamageSystem dmg) {
            subscribeWard(owner, bus, Element.FIRE);
        }
    },

    FROST_RESISTANCE {
        public void onBattleStart(Combatant owner, TriggerBus bus, DamageSystem dmg){
            subscribeWard(owner, bus, Element.ICE);
        }
    },

    VAMPIRIC_EDGE {
        public void onBattleStart(Combatant owner, TriggerBus bus, DamageSystem dmg) {
            bus.subscribe(Trigger.ON_HIT_LAND, owner, ev -> {
                int heal = ((DamageEvent) ev.payload).amount / 4;
                if (heal > 0) dmg.heal(new HealEvent(owner, heal));
            });
        }
    },

    SOUL_FEAST {
        public void onBattleStart(Combatant owner, TriggerBus bus, DamageSystem dmg) {
            bus.subscribe(Trigger.ON_KILL, owner, ev -> dmg.heal(new HealEvent(owner, 15)));
        }
    };

    /** Extra shared-pool slots this effect grants. */
    public int slotCapacityBonus() { return 0; }

    /** Subscribe this effect's listeners to the per-battle bus. Bus dies with the battle, so no unsubscribe. */
    public void onBattleStart(Combatant owner, TriggerBus bus, DamageSystem dmg) {}

    private static void subscribeWard(Combatant owner, TriggerBus bus, Element element) {
        bus.subscribe(Trigger.ON_DAMAGE_TAKEN_PRE, owner, ev -> {
            DamageEvent d = (DamageEvent) ev.payload;
            if (d.originalSkill != null && d.originalSkill.getElement() == element) d.amount /= 2;
        });
    }
}
