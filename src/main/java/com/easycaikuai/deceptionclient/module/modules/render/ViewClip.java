package com.easycaikuai.deceptionclient.module.modules.render;

import net.minecraft.client.Minecraft;
import com.easycaikuai.deceptionclient.module.Module;

public class ViewClip extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public ViewClip() {
        super("ViewClip", false, true);
    }

    @Override
    public void onEnabled() {
        if (mc.theWorld != null) {
            mc.renderGlobal.loadRenderers();
        }
    }

    @Override
    public void onDisabled() {
        if (mc.theWorld != null) {
            mc.renderGlobal.loadRenderers();
        }
    }
}
