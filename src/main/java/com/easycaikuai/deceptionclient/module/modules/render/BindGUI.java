package com.easycaikuai.deceptionclient.module.modules.render;

import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.Render2DEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.ColorProperty;
import com.easycaikuai.deceptionclient.util.KeyBindUtil;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BindGUI extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty background = new BooleanProperty("Background", true);
    public final ColorProperty bgColor = new ColorProperty("Bg Color", new Color(0, 0, 0, 100).getRGB());
    public final ColorProperty textColor = new ColorProperty("Text Color", Color.WHITE.getRGB());
    public final ColorProperty enabledColor = new ColorProperty("Enabled Color", new Color(100, 255, 100).getRGB());

    public BindGUI() {
        super("BindGUI", false, true);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (!isEnabled()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int x = 4;
        int y = 4;
        int lineHeight = mc.fontRendererObj.FONT_HEIGHT + 2;

        // 收集有绑定的模块（不限 hidden）
        List<Module> boundMods = new ArrayList<>();
        for (Module mod : Deception.moduleManager.modules.values()) {
            if (mod.getKey() != 0) {
                boundMods.add(mod);
            }
        }
        if (boundMods.isEmpty()) return;

        // 按文字长度排序（长→短）
        boundMods.sort(Comparator.comparingInt((Module m) -> {
            String keyName = KeyBindUtil.getKeyName(m.getKey());
            return (m.getName() + " [" + keyName + "]").length();
        }).reversed());

        // 计算最大宽度
        int maxWidth = 0;
        for (Module mod : boundMods) {
            String keyName = KeyBindUtil.getKeyName(mod.getKey());
            String line = mod.getName() + " [" + keyName + "]";
            int w = mc.fontRendererObj.getStringWidth(line);
            if (w > maxWidth) maxWidth = w;
        }
        int boxW = maxWidth + 10;
        int boxH = boundMods.size() * lineHeight + 6;

        // 背景
        if (background.getValue()) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RoundedUtils.drawRound(x, y, boxW, boxH, 3.0F, new Color(bgColor.getValue(), true));
            GlStateManager.disableBlend();
        }

        // 文字
        int textY = y + 4;
        int offColor = textColor.getValue();
        int onColor = enabledColor.getValue();
        for (Module mod : boundMods) {
            String keyName = KeyBindUtil.getKeyName(mod.getKey());
            String line = mod.getName() + " [" + keyName + "]";
            int color = mod.isEnabled() ? onColor : offColor;
            mc.fontRendererObj.drawString(line, x + 5, textY, color, true);
            textY += lineHeight;
        }
    }
}