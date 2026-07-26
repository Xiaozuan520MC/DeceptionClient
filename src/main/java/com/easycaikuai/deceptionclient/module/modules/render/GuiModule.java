package com.easycaikuai.deceptionclient.module.modules.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.ui.deception.DeceptionGui;

public class GuiModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public GuiModule() {
        super("ClickGui", false);
        setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        mc.displayGuiScreen(new DeceptionGui());
    }
}
