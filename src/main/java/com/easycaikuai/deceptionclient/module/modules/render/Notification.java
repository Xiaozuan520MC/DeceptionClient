package com.easycaikuai.deceptionclient.module.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.Render2DEvent;
import com.easycaikuai.deceptionclient.management.NotificationManager;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.IntProperty;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;

import java.awt.Color;
import java.util.List;

public class Notification extends Module {

    public final BooleanProperty shadow = new BooleanProperty("Shadow", true);
    public final IntProperty stayTime = new IntProperty("Stay Time", 3000, 500, 10000);

    public static final java.util.concurrent.CopyOnWriteArrayList<com.easycaikuai.deceptionclient.util.NotificationTask> tasks = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final java.util.concurrent.CopyOnWriteArrayList<com.easycaikuai.deceptionclient.util.NotificationTask> disTasks = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static NotificationManager mgr = null;
    private static NotificationManager getMgr() { if (mgr == null) mgr = new NotificationManager(); return mgr; }

    public static void post(String name) { getMgr().add(name, 3000, 0x34C759); }
    public static void postDis(String name) { getMgr().add(name, 3000, 0xFF453A); }

    public Notification() { super("Notification", true, true); }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!isEnabled()) return;

        List<NotificationManager.NotificationEntry> entries = getMgr().getActive();
        if (entries.isEmpty()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        float scale = 1f;
        float sw = sr.getScaledWidth() / scale;
        float sh = sr.getScaledHeight() / scale;
        float margin = 8, padX = 8, padY = 5, spacing = 4;
        float y = sh - margin;

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1);

        for (int i = entries.size() - 1; i >= 0; i--) {
            NotificationManager.NotificationEntry entry = entries.get(i);
            float age = entry.getAge();
            float dur = entry.durationMillis;

            float alpha = Math.min(1, age / 100f);
            float fadeOut = age > dur * 0.7f ? Math.max(0, (dur - age) / (dur * 0.3f)) : 1f;
            alpha = Math.min(alpha, fadeOut);
            if (alpha <= 0.01f) continue;

            String text = entry.message;
            float tw = Deception.fontManager.s14.getStringWidth(text);
            float th = Deception.fontManager.s14.getHeight();
            float boxW = Math.max(86, tw + padX * 2 + 2);
            float boxH = th + padY * 2 + 3;
            float x = sw - margin - boxW;

            y -= boxH;

            boolean on = entry.color == 0x34C759;
            int statusColor = new Color(on ? 52 : 255, on ? 199 : 69, on ? 89 : 58, (int)(255 * alpha)).getRGB();

            int glass = new Color(10, 12, 16, (int)(92 * alpha)).getRGB();
            int depth = new Color(0, 0, 0, (int)(28 * alpha)).getRGB();
            int neutralText = new Color(238, 241, 245, (int)(242 * alpha)).getRGB();

            float radius = 6;

            // 阴影
            RoundedUtils.drawRound(x + 1, y + 1.5f, boxW, boxH, radius + 0.5f, new Color(depth, true));
            // 主背景
            RoundedUtils.drawRound(x, y, boxW, boxH, radius, new Color(glass, true));
            // 高光层
            RoundedUtils.drawRound(x + 1, y + 1, boxW - 2, boxH - 2, radius - 0.5f, new Color(255, 255, 255, (int)(9 * alpha)));
            // 边框
            RoundedUtils.drawRoundOutline(x + 0.5f, y + 0.5f, boxW - 1, boxH - 1, radius, 1f, new Color(0, true), new Color(255, 255, 255, (int)(24 * alpha)));

            // 进度条
            float progress = age > dur * 0.85f ? 0 : 1 - age / (dur * 0.85f);
            float px2 = x + 8, py2 = y + boxH - 2, pw = boxW - 16;
            RoundedUtils.drawRound(px2, py2, pw, 1, 0.5f, new Color(255, 255, 255, (int)(10 * alpha)));
            RoundedUtils.drawRound(px2, py2, pw * progress, 1, 0.5f, new Color(statusColor, true));

            // 文字
            String clean = text.replace("Enabled ", "").replace("Disabled ", "");
            Deception.fontManager.s14.drawString(clean, x + padX + 1, y + padY + 1, neutralText);

            // 状态标记
            String statusMark = on ? "ON" : "OFF";
            float statusW = Deception.fontManager.s12.getStringWidth(statusMark);
            Deception.fontManager.s12.drawString(statusMark, x + boxW - padX - 1 - statusW, y + padY + 2, statusColor);

            y -= spacing;
        }

        GlStateManager.popMatrix();
    }

}
