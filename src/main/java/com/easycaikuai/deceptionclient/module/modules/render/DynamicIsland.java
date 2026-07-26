package com.easycaikuai.deceptionclient.module.modules.render;

import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.Render2DEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.ColorProperty;
import com.easycaikuai.deceptionclient.util.glow.GlowUtils;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.awt.Color;

public class DynamicIsland extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ColorProperty accent = new ColorProperty("Accent", new Color(175, 82, 222).getRGB());
    public final BooleanProperty glow = new BooleanProperty("Glow", true);

    private String lastText = "";
    private float textW = 0;
    private int lastFps = 0, lastPing = 0;
    private String lastName = "", lastServer = "";

    public DynamicIsland() { super("DynamicIsland", true); }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!isEnabled() || mc.thePlayer == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        String name = mc.thePlayer.getName();
        int fps = Minecraft.getDebugFPS();
        int ping = 0;
        try {
            NetworkPlayerInfo pi = mc.getNetHandler().getPlayerInfo(name);
            if (pi != null) ping = pi.getResponseTime();
        } catch (Exception e) {}
        String server = "SP";
        try { if (mc.getCurrentServerData() != null) server = mc.getCurrentServerData().serverIP; } catch (Exception e) {}
        if (!name.equals(lastName) || fps != lastFps || ping != lastPing || !server.equals(lastServer)) {
            lastText = "§dDeception §7| " + name + " §7| " + server + " §7| FPS: " + fps + " §7| Ping: " + ping + "ms";
            textW = Deception.fontManager.s14.getStringWidth(lastText);
            lastName = name; lastFps = fps; lastPing = ping; lastServer = server;
        }
        float padX = 14;
        float w = textW + padX * 2 + 6;
        float h = 20;
        if (w < 10) return;
        float x = sr.getScaledWidth() / 2f - w / 2f;
        float y = 5;
        int ac = new Color(accent.getValue()).getRGB();
        if (glow.getValue()) {
            GlowUtils.drawGlow(x, y, w, h, 40, new Color(ac).darker());
            GlowUtils.drawGlow(x, y, w, h, 18, new Color(ac));
        }
        RoundedUtils.drawRound(x, y, w, h, 10, new Color(0, 0, 0, 140));
        RoundedUtils.drawRound(x + 0.5f, y + 0.5f, w - 1, h - 1, 9.5f, new Color(255, 255, 255, 8));
        Deception.fontManager.s14.drawString(lastText, x + padX, y + (h - Deception.fontManager.s14.getHeight()) / 2f + 1, 0xFFF0F2F5);
    }
}