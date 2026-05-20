package com.silverignis.systems.combat;

import com.silverignis.systems.combat.event.TriggerEvent;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;

public class TriggerBus {

    private final EnumMap<Trigger, List<Entry>> listeners = new EnumMap<>(Trigger.class);

    public Subscription subscribe(Trigger trigger, Combatant filter, TriggerListener listener){
        Entry e = new Entry(filter, listener);
        listeners.computeIfAbsent(trigger, k -> new ArrayList<>()).add(e);

        // return Subscription with defined unsubscribed method to be called by subscriber
        return () -> {
            List<Entry> bucket = listeners.get(trigger);
            if(bucket != null) bucket.remove(e);
        };

    }

    public void fire(TriggerEvent ev){
        List<Entry> bucket = listeners.get(ev.trigger);
        if (bucket== null || bucket.isEmpty()) return;

        //Snapshot: a listener may unsubscribe itself or another during dispatch.
        Entry[] snap = bucket.toArray(new Entry[0]);
        for(Entry e: snap){
            if (e.filter != null && e.filter != ev.combatant) continue;
            e.listener.onTrigger(ev);
        }
    }


    private record Entry(Combatant filter, TriggerListener listener) {}

    @FunctionalInterface
    public interface Subscription{
        void unsubscribe();
    }
}
