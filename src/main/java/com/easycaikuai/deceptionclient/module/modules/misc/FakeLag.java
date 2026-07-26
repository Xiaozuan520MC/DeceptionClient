package com.easycaikuai.deceptionclient.module.modules.misc;

import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.events.PacketEvent;
import com.easycaikuai.deceptionclient.events.UpdateEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.IntProperty;
import com.easycaikuai.deceptionclient.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;

import java.util.concurrent.ConcurrentLinkedQueue;

public class FakeLag extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // The amount of latency to simulate in milliseconds
    public final IntProperty delay = new IntProperty("delay-ms", 200, 50, 5000);

    private final ConcurrentLinkedQueue<PacketData> packetQueue = new ConcurrentLinkedQueue<>();
    private boolean isDispatching = false;

    public FakeLag() {
        super("FakeLag", false);
    }

    @Override
    public void onEnabled() {
        packetQueue.clear();
        this.isDispatching = false;
    }

    @Override
    public void onDisabled() {
        this.isDispatching = true;
        while (!packetQueue.isEmpty()) {
            PacketUtil.sendPacket(packetQueue.poll().packet);
        }
        this.isDispatching = false;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if(!this.enabled) return;
        if (event.getType() == EventType.SEND) {
            if (this.isDispatching) {
                return;
            }

            if (mc.thePlayer == null || mc.theWorld == null) {
                return;
            }

            Packet<?> packet = event.getPacket();

            event.setCancelled(true);

            packetQueue.add(new PacketData(packet, System.currentTimeMillis()));
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.PRE) {
            if (mc.thePlayer == null) return;

            long currentTime = System.currentTimeMillis();
            long delayTime = this.delay.getValue();

            if (packetQueue.isEmpty()) return;

            while (!packetQueue.isEmpty()) {
                PacketData data = packetQueue.peek();

                if (currentTime - data.timestamp >= delayTime) {
                    packetQueue.poll();

                    this.isDispatching = true;
                    PacketUtil.sendPacket(data.packet);
                    this.isDispatching = false;
                } else {
                    break;
                }
            }
        }
    }

    private static class PacketData {
        private final Packet<?> packet;
        private final long timestamp;

        public PacketData(Packet<?> packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}