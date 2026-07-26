package com.easycaikuai.deceptionclient.module.modules.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.ui.ConfigGui;

public class ConfigModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public ConfigModule() {
        super("Config", false);
        setKey(Keyboard.KEY_NONE);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        mc.displayGuiScreen(new ConfigGui());
    }
}
