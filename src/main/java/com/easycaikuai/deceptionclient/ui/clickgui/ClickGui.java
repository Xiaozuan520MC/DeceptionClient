package com.easycaikuai.deceptionclient.ui.clickgui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.Property;
import com.easycaikuai.deceptionclient.property.properties.*;
import com.easycaikuai.deceptionclient.util.RenderUtil;

import java.awt.Color;
import java.io.IOException;
import java.util.*;

public class ClickGui extends GuiScreen {

    private final List<CategoryPanel> panels = new ArrayList<>();

    public ClickGui() {
        // 每个分类手动添加模块（和 OpenMyau 一样的方式）
        addPanel("Combat",
            "AimAssist", "AntiFireball", "AutoClicker", "AutoLava", "AutoProjectiles",
            "BackTrack", "BlockHit", "Criticals", "HitBox", "JumpReset",
            "KillAura", "LagRange", "MoreKB", "NoHitDelay", "Reach",
            "SprintReset", "TargetStrafe", "Timer", "TickBase", "Velocity", "Wtap");
        addPanel("Movement",
            "AntiAFK", "AntiVoid", "Blink", "Eagle", "Fly", "Jesus",
            "KeepSprint", "LongJump", "NoFall", "NoJumpDelay", "NoSlow",
            "SafeWalk", "Speed", "Sprint");
        addPanel("Render",
            "Animations", "BedESP", "BlockOverlay", "Chams", "ChestESP",
            "ChineseHat", "DynamicIsland", "ESP", "FreeLook",
            "FullBright", "GuiModule", "HUD", "Icon", "Indicators", "Island",
            "ItemESP", "ItemPhysics", "KillEffect", "NameTags",
            "NoHurtCam", "Notification", "Potion", "Radar", "TargetESP",
            "TargetHUD", "TeamHealthDisplay", "Tracers", "Trajectories",
            "ViewClip", "WaterMark", "Xray");
        addPanel("Player",
            "AutoBlockIn", "AutoGapple", "AutoHeal", "AutoSwap", "AutoTool",
            "ChestAura", "ChestStealer", "FastBow", "FastPlace", "GhostHand",
            "InvManager", "InvWalk", "LegitScaffold", "MCF", "Refill",
            "Scaffold", "SpeedMine", "Stuck");
        addPanel("Misc",
            "AntiObbyTrap", "AntiObfuscate", "AutoL", "BedNuker", "BedTracker",
            "Disabler", "Displace", "FakeLag", "FlagDetector", "KillSound",
            "LightningTracker", "NickHider", "NoRotate", "ServerLag", "Spammer",
            "ConfigModule");
    }

    private void addPanel(String name, String... modNames) {
        List<Module> mods = new ArrayList<>();
        for (String n : modNames) {
            Module m = Deception.moduleManager.getModule(n);
            if (m != null) mods.add(m);
        }
        mods.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        panels.add(new CategoryPanel(name, mods));
    }

    // ─── 位置 ──
    private int panelY = 5;
    private int nextY() { int y = panelY; panelY += 20; return y; }

    @Override
    public void initGui() {
        int[] ys = {5, 25, 45, 65, 85};
        for (int i = 0; i < panels.size(); i++) panels.get(i).y = ys[i % ys.length];
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, new Color(0, 0, 0, 100).getRGB());
        fontRendererObj.drawStringWithShadow("Deception " + (Deception.version != null ? Deception.version : "dev"), 4, height - 3 - fontRendererObj.FONT_HEIGHT * 2, new Color(60, 162, 253).getRGB());
        fontRendererObj.drawStringWithShadow("dev", 4, height - 3 - fontRendererObj.FONT_HEIGHT, new Color(60, 162, 253).getRGB());

        for (CategoryPanel p : panels) {
            p.render(mx, my);
            p.drag(mx, my);
        }
    }

    @Override protected void mouseClicked(int mx, int my, int btn) throws IOException {
        for (CategoryPanel p : panels) {
            if (p.insideHeader(mx, my)) {
                if (btn == 0) { p.dragging = true; p.dx = mx - p.x; p.dy = my - p.y; }
                if (p.isOnToggle(mx, my)) { p.opened = !p.opened; return; }
                if (p.isOnPin(mx, my)) { p.pin = !p.pin; return; }
                return;
            }
            if (p.opened) {
                for (ModuleRow r : p.rows) {
                    int ry = p.y + 16 + r.offset;
                    if (mx >= p.x + 1 && mx <= p.x + 91 && my >= ry && my <= ry + 14) {
                        if (btn == 1) r.expanded = !r.expanded;
                        else r.mod.toggle();
                        return;
                    }
                }
            }
        }
    }
    @Override protected void mouseReleased(int mx, int my, int btn) {
        for (CategoryPanel p : panels) p.dragging = false;
    }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int w = Mouse.getEventDWheel();
        if (w != 0) {
            for (CategoryPanel p : panels) {
                if (p.opened) p.scroll -= w > 0 ? 12 : -12;
            }
        }
    }
    @Override protected void keyTyped(char c, int k) throws IOException {
        if (k == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(null);
    }
    @Override public boolean doesGuiPauseGame() { return false; }

    // ═══════════════════════════════════════════════════════════
    //  CategoryPanel
    // ═══════════════════════════════════════════════════════════
    class CategoryPanel {
        String name; List<ModuleRow> rows; int x = 5, y = 5;
        boolean opened = false, dragging = false, pin = false;
        int dx, dy, scroll = 0;
        static final int W = 92, BH = 13, MAX_H = 300;

        CategoryPanel(String n, List<Module> mods) { name = n; rows = new ArrayList<>(); for (Module m : mods) { ModuleRow r = new ModuleRow(m); r.setParent(this); rows.add(r); } }

        void render(int mx, int my) {
            int displayH = 0;
            for (ModuleRow r : rows) displayH += r.height();
            if (opened && !rows.isEmpty()) {
                int dh = Math.min(displayH, MAX_H);
                Gui.drawRect(x - 1, y, x + W + 1, y + BH + dh + 4, new Color(0, 0, 0, 100).getRGB());
            }
            Gui.drawRect(x - 2, y, x + W + 2, y + BH + 3, new Color(0, 0, 0, 200).getRGB());
            fontRendererObj.drawString(name, x + 2, y + 4, -1, false);
            fontRendererObj.drawString(opened ? "-" : "+", x + 80, (float)(y + 4.5), Color.WHITE.getRGB(), false);

            if (opened && !rows.isEmpty()) {
                int clipH = Math.min(displayH, MAX_H);
                int bottom = y + BH + MAX_H + 3;
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                ScaledResolution sr = new ScaledResolution(mc);
                double scale = sr.getScaleFactor();
                GL11.glScissor((int)(x * scale), (int)((sr.getScaledHeight() - bottom) * scale), (int)(W * scale), (int)(MAX_H * scale));

                int cy = 0;
                for (ModuleRow r : rows) {
                    int rh = r.height();
                    if (cy + rh > scroll && cy < scroll + MAX_H) {
                        r.offset = BH + 3 + (int)(cy - scroll);
                        r.draw();
                    }
                    cy += rh;
                }
                GL11.glDisable(GL11.GL_SCISSOR_TEST);

                if (displayH > MAX_H) {
                    int sy = (int)(y + BH + 3 + (float)scroll / displayH * MAX_H);
                    int sh = Math.max(4, MAX_H * MAX_H / displayH);
                    Gui.drawRect(x + W - 2, sy, x + W, sy + sh, new Color(255, 255, 255, 60).getRGB());
                }
            }
        }

        void drag(int mx, int my) { if (dragging) { x = mx - dx; y = my - dy; } }

        boolean insideHeader(int mx, int my) { return mx >= x && mx <= x + W && my >= y && my <= y + BH; }
        boolean isOnToggle(int mx, int my) { return mx >= x + 77 && mx <= x + W - 6 && my >= y + 2 && my <= y + BH + 1; }
        boolean isOnPin(int mx, int my) { return mx >= x + W - 13 && mx <= x + W && my >= y + 2 && my <= y + BH + 1; }
    }

    // ═══════════════════════════════════════════════════════════
    //  ModuleRow
    // ═══════════════════════════════════════════════════════════
    class ModuleRow {
        Module mod; boolean expanded = false; int offset;
        CategoryPanel parent;

        ModuleRow(Module m) { mod = m; parent = null; }
        void setParent(CategoryPanel p) { parent = p; }
        int px() { return parent != null ? parent.x : 5; }
        int pw() { return parent != null ? parent.W : 92; }

        int height() {
            if (!expanded) return 14;
            int h = 16;
            List<Property<?>> props = Deception.propertyManager.properties.get(mod.getClass());
            if (props != null) h += props.size() * 12 + 12;
            return h;
        }

        void draw() {
            // 模块名行
            int bgColor = mod.isEnabled() ? new Color(30, 30, 30, 180).getRGB() : new Color(0, 0, 0, 0).getRGB();
            int textColor = mod.isEnabled() ? -1 : new Color(100, 100, 100).getRGB();

            Gui.drawRect(px() + 2, offset, px() + pw() - 2, offset + 12, bgColor);
            fontRendererObj.drawString((expanded ? "-" : "+") + " " + mod.getName(), px() + 4, offset + 2, textColor, false);

            if (!expanded) return;

            int sy = offset + 14;
            List<Property<?>> props = Deception.propertyManager.properties.get(mod.getClass());
            if (props != null) for (Property<?> p : props) {
                drawProperty(p, sy);
                sy += 12;
            }
            // Bind
            fontRendererObj.drawString("Bind: " + (mod.getKey() == 0 ? "NONE" : Keyboard.getKeyName(mod.getKey())), px() + 5, sy + 1, new Color(140, 140, 140).getRGB(), false);
        }

        void drawProperty(Property<?> p, int sy) {
            if (p instanceof BooleanProperty) {
                boolean v = ((BooleanProperty)p).getValue();
                fontRendererObj.drawString((v ? "[x] " : "[ ] ") + p.getName(), px() + 5, sy + 1, v ? new Color(60, 162, 253).getRGB() : new Color(100, 100, 100).getRGB(), false);
            } else if (p instanceof ModeProperty) {
                ModeProperty mp = (ModeProperty)p;
                fontRendererObj.drawString(p.getName() + ": " + mp.getModeString(), px() + 5, sy + 1, new Color(140, 140, 140).getRGB(), false);
            } else if (p instanceof FloatProperty || p instanceof IntProperty || p instanceof PercentProperty) {
                double min, max, val; String d;
                if (p instanceof FloatProperty) { FloatProperty fp = (FloatProperty)p; min = fp.getMinimum(); max = fp.getMaximum(); val = fp.getValue(); d = String.format("%.1f", val); }
                else if (p instanceof IntProperty) { IntProperty ip = (IntProperty)p; min = ip.getMinimum(); max = ip.getMaximum(); val = ip.getValue(); d = String.valueOf((int)val); }
                else { PercentProperty pp = (PercentProperty)p; min = pp.getMinimum(); max = pp.getMaximum(); val = pp.getValue(); d = (int)val + "%"; }
                float pct = (float)((val - min) / Math.max(max - min, 0.001));
                fontRendererObj.drawString(p.getName() + " " + d, px() + 5, sy + 1, new Color(180, 180, 180).getRGB(), false);
                Gui.drawRect(px() + 5, sy + 10, px() + pw() - 5, sy + 11, new Color(60, 60, 60).getRGB());
                Gui.drawRect(px() + 5, sy + 10, (int)(px() + 5 + (pw() - 10) * pct), sy + 11, new Color(60, 162, 253).getRGB());
            }
        }
    }
}
