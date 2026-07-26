package com.easycaikuai.deceptionclient.module.modules.misc;

import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.PacketEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.util.ChatUtil;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class FlagDetector extends Module {
    public FlagDetector() {
        super("FlagDetector", false, true);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled())
            return;

        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            ChatUtil.sendFormatted("&7[&cFlagDetector&7] &fServer flag detected (Lagback)!");
        }
    }
}
