package com.easycaikuai.deceptionclient.events;

import com.easycaikuai.deceptionclient.event.events.Event;
import com.easycaikuai.deceptionclient.event.types.EventType;

public class TickEvent implements Event {
    private final EventType type;

    public TickEvent(EventType type) {
        this.type = type;
    }

    public EventType getType() {
        return this.type;
    }
}
