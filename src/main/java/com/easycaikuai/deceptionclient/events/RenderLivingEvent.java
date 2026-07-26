package com.easycaikuai.deceptionclient.events;

import net.minecraft.entity.EntityLivingBase;
import com.easycaikuai.deceptionclient.event.events.Event;
import com.easycaikuai.deceptionclient.event.types.EventType;

public class RenderLivingEvent implements Event {
    private final EventType type;
    private final EntityLivingBase entity;

    public RenderLivingEvent(EventType type, EntityLivingBase entityLivingBase) {
        this.type = type;
        this.entity = entityLivingBase;
    }

    public EventType getType() {
        return this.type;
    }

    public EntityLivingBase getEntity() {
        return this.entity;
    }
}
