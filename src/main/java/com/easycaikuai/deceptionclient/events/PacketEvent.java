package com.easycaikuai.deceptionclient.events;

import net.minecraft.network.Packet;
import com.easycaikuai.deceptionclient.event.events.callables.EventCancellable;
import com.easycaikuai.deceptionclient.event.types.EventType;

public class PacketEvent extends EventCancellable {
    private final EventType type;
    private final Packet<?> packet;

    public PacketEvent(EventType type, Packet<?> packet) {
        this.type = type;
        this.packet = packet;
    }

    public EventType getType() {
        return this.type;
    }

    public Packet<?> getPacket() {
        return this.packet;
    }
}
