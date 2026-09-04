package com.silverignis.systems.combat.status.visuals;

import com.badlogic.gdx.graphics.Color;
import com.silverignis.particles.ParticleEngine;
import com.silverignis.systems.combat.Combatant;
import com.silverignis.systems.combat.StatusType;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

/** Per-combatant status visuals: polls the owner's StatusContainer, runs one
 *  {@link StatusVisual} per active registered status, and composes the sprite
 *  tint. Render-side sibling of AnimController — it never changes sim state.
 *  Statuses stack: every live visual updates and tints, in StatusType order. */
public final class StatusFx {

    private static final Map<StatusType, Supplier<StatusVisual>> REGISTRY = new EnumMap<>(Map.of(
        StatusType.FREEZE, FreezeVisual::new,
        StatusType.STUN,   StunVisual::new,
        StatusType.POISON, PoisonVisual::new,
        StatusType.BURN,   BurnVisual::new,
        StatusType.COMBUSTION, CombustionVisual::new,
        StatusType.FROSTBITE, FrostbiteVisual::new,
        StatusType.VENOM,     VenomVisual::new,
        StatusType.CONDUCTIVE, ConductiveVisual::new,
        StatusType.GRAVITY,   GravityVisual::new));

    private final Combatant owner;
    private final EnumMap<StatusType, StatusVisual> live = new EnumMap<>(StatusType.class);

    public StatusFx(Combatant owner) { this.owner = owner; }

    public void update(float delta, ParticleEngine particles) {
        for (Map.Entry<StatusType, Supplier<StatusVisual>> reg : REGISTRY.entrySet()) {
            StatusType type = reg.getKey();
            boolean active = owner.isAlive() && owner.getStatusContainer().has(type);
            StatusVisual v = live.get(type);
            if (active && v == null) {
                v = reg.getValue().get();
                v.begin(owner, particles);
                live.put(type, v);
            } else if (!active && v != null) {
                v.end();
            }
        }
        for (Iterator<StatusVisual> it = live.values().iterator(); it.hasNext(); ) {
            StatusVisual v = it.next();
            v.update(delta, owner);
            if (v.isDone()) it.remove();
        }
    }

    /** Sprite color for the owner's billboard draw: white at {@code alpha},
     *  blended by each active status in StatusType order. */
    public Color spriteColor(Color out, float alpha) {
        out.set(1f, 1f, 1f, alpha);
        for (StatusVisual v : live.values()) v.tint(out);
        return out;
    }
}
