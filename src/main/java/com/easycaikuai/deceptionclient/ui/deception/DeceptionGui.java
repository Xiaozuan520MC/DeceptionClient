package com.easycaikuai.deceptionclient.ui.deception;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.config.Config;
import com.easycaikuai.deceptionclient.module.Category;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.Property;
import com.easycaikuai.deceptionclient.property.properties.*;
import com.easycaikuai.deceptionclient.util.animation.Easing;
import com.easycaikuai.deceptionclient.util.animation.RiseAnim;
import com.easycaikuai.deceptionclient.util.animations.advanced.ContinualAnimation;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class DeceptionGui extends GuiScreen {

    private static final int SIDEBAR_W = 88;
    private static final int CAT_H = 26;
    private static final int TITLE_H = 22;
    private static final int ROW_H = 30;
    private static final int SET_H = 20;
    private static final int CARD_GAP = 3;
    private static final int RADIUS = 7;

    private int PANEL_W, PANEL_H;

    // Rise 风格配色 — 紫色主题
    private static final int PANEL_BG  = 0xEB1E1E23;
    private static final int SIDE_BG   = 0xEB19191E;
    private static final int ACCENT    = 0xFFAF52DE;
    private static final int GREEN     = 0xFF34C759;
    private static final int TEXT      = 0xFFEBEBF0;
    private static final int TEXT_SUB  = 0xFF93939F;
    private static final int TEXT_DIM  = 0xFF5A5F6D;
    private static final int INPUT_BG  = 0x28232840;
    private static final int MODULE_BG_ON  = 0x28203848;
    private static final int MODULE_BG_OFF = 0x181A1E28;
    private static final int TOGGLE_OFF    = 0xFF39393D;

    private static Category sel = Category.COMBAT;
    private static final Set<String> exp = new HashSet<>();
    private static String rebind = null;
    private static float scroll = 0, smoothScroll = 0;
    private static String search = "";
    private int mx, my, pX, pY;

    private String configName = "default";
    private boolean editingConfig = false;
    private List<String> savedConfigs = new ArrayList<>();
    private static final File CONFIG_DIR = new File("./config/Deception/");

    // ─── 动画系统 ───────────────────────────────────────────

    /** 开启动画：面板从中心缩放淡入 */
    private final RiseAnim openAnim = new RiseAnim(Easing.EASE_OUT_QUINT, 400);

    /** 分类切换动画：高亮条平滑移动到目标分类 */
    private final RiseAnim categoryAnim = new RiseAnim(Easing.EASE_OUT_CIRC, 250);
    private float categoryTargetY = 0;

    /** 模块展开/折叠动画（高度） */
    private final Map<String, ContinualAnimation> expandAnims = new HashMap<>();

    /** 开关动画 */
    private static class ToggleAnim {
        float target, current;
    }
    private final Map<String, ToggleAnim> toggleAnims = new HashMap<>();

    /** 模块悬停动画 */
    private final Map<String, RiseAnim> hoverAnims = new HashMap<>();

    private void refreshConfigs() {
        savedConfigs.clear();
        if (CONFIG_DIR.exists()) {
            File[] files = CONFIG_DIR.listFiles((d, n) -> n.endsWith(".json"));
            if (files != null) for (File f : files)
                savedConfigs.add(f.getName().replace(".json", ""));
        }
        savedConfigs.sort(String::compareToIgnoreCase);
    }

    @Override
    public void initGui() {
        PANEL_W = width * 3 / 4;
        PANEL_H = height * 3 / 4;
        pX = (width - PANEL_W) / 2;
        pY = (height - PANEL_H) / 2;
        refreshConfigs();

        // 重置开启动画
        openAnim.setValue(0);
        openAnim.run(1);

        // 初始化分类动画位置
        categoryTargetY = getCategoryY(sel);
        categoryAnim.setValue(categoryTargetY);
        categoryAnim.run(categoryTargetY);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        this.mx = mx; this.my = my;

        // ── 平滑滚动 ──
        smoothScroll += (scroll - smoothScroll) * 0.28f;
        if (Math.abs(scroll - smoothScroll) < 0.5f) smoothScroll = scroll;

        // ── 开启动画 ──
        openAnim.run(1);
        double openProgress = openAnim.getValue();

        drawDefaultBackground();

        // 应用开启动画：缩放 + 淡入
        float openScale = 0.92f + 0.08f * (float) openProgress;
        int openAlpha = Math.min(255, Math.max(0, (int) (220 * openProgress)));

        GL11.glPushMatrix();
        GL11.glTranslatef(pX + PANEL_W / 2f, pY + PANEL_H / 2f, 0);
        GL11.glScalef(openScale, openScale, 1);
        GL11.glTranslatef(-pX - PANEL_W / 2f, -pY - PANEL_H / 2f, 0);

        // 绘制主面板（半透明背景）
        int bgColor = (openAlpha << 24) | (PANEL_BG & 0xFFFFFF);
        RoundedUtils.drawRound(pX, pY, PANEL_W, PANEL_H, RADIUS, new Color(bgColor, true));
        RoundedUtils.drawRound(pX + 40, pY, PANEL_W - 80, 2, 1, new Color(ACCENT));

        drawSidebar();
        drawTitle();
        drawContent();

        GL11.glPopMatrix();
    }

    @Override public boolean doesGuiPauseGame() { return true; }
    @Override public void onGuiClosed() { rebind = null; search = ""; editingConfig = false; }

    // ═══════════════════════════════════════════════════════
    //  侧栏
    // ═══════════════════════════════════════════════════════

    void drawSidebar() {
        RoundedUtils.drawRound(pX + 4, pY + 6, SIDEBAR_W - 4, PANEL_H - 12, 8, new Color(SIDE_BG, true));

        Deception.fontManager.s14.drawString("Deception",
                pX + SIDEBAR_W / 2f - Deception.fontManager.s14.getStringWidth("Deception") / 2f,
                pY + 26, ACCENT);
        String ver = Deception.version != null ? "v" + Deception.version : "";
        Deception.fontManager.s12.drawString(ver,
                pX + SIDEBAR_W / 2f - Deception.fontManager.s12.getStringWidth(ver) / 2f,
                pY + 40, TEXT_DIM);

        // ── 更新分类动画 ──
        categoryAnim.run(categoryTargetY);
        float catAnimatedY = (float) categoryAnim.getValue();

        // 绘制高亮条（动画位置）
        RoundedUtils.drawRound(pX + 6, catAnimatedY, SIDEBAR_W - 12, CAT_H, CAT_H / 2f, new Color(ACCENT));

        int sy = pY + 60;
        for (Category c : Category.values()) {
            if (c == Category.CONFIG) continue;
            boolean selC = c == sel;
            boolean hover = mx >= pX + 6 && mx <= pX + SIDEBAR_W - 6 && my >= sy && my <= sy + CAT_H;

            if (!selC && hover) {
                RoundedUtils.drawRound(pX + 6, sy, SIDEBAR_W - 12, CAT_H, CAT_H / 2f, new Color(INPUT_BG, true));
            }

            // 选中的高亮直接由上面的动画条绘制，此处不重复绘制
            if (!selC) {
                String label = c.name().charAt(0) + c.name().substring(1).toLowerCase();
                int lw = Deception.fontManager.s12.getStringWidth(label);
                Deception.fontManager.s12.drawString(label,
                        pX + SIDEBAR_W / 2f - lw / 2f,
                        sy + (CAT_H - Deception.fontManager.s12.getHeight()) / 2f + 1,
                        TEXT_SUB);
            }
            sy += CAT_H + 2;
        }

        // 选中的分类文字单独绘制（在最上层）
        if (sel != null) {
            String selLabel = sel.name().charAt(0) + sel.name().substring(1).toLowerCase();
            int lw = Deception.fontManager.s12.getStringWidth(selLabel);
            Deception.fontManager.s12.drawString(selLabel,
                    pX + SIDEBAR_W / 2f - lw / 2f,
                    catAnimatedY + (CAT_H - Deception.fontManager.s12.getHeight()) / 2f + 1,
                    0xFFFFFFFF);
        }
    }

    /** 获取分类在侧栏中的 Y 坐标 */
    private float getCategoryY(Category cat) {
        int sy = pY + 60;
        for (Category c : Category.values()) {
            if (c == Category.CONFIG) continue;
            if (c == cat) return sy;
            sy += CAT_H + 2;
        }
        return sy;
    }

    // ═══════════════════════════════════════════════════════
    //  顶栏
    // ═══════════════════════════════════════════════════════

    void drawTitle() {
        if (sel != Category.SETTINGS && sel != Category.CONFIG) {
            String display = search.isEmpty() ? "Search..." : search;
            boolean cursor = System.currentTimeMillis() % 800 > 400;
            int sw = Deception.fontManager.s12.getStringWidth(display);
            int sx = pX + PANEL_W - sw - 26;
            RoundedUtils.drawRound(sx - 4, pY + 4, sw + 16, 14, 7, new Color(INPUT_BG, true));
            Deception.fontManager.s12.drawString(
                    search.isEmpty() ? "§8Search..." : "§7" + search + (cursor ? "§8|" : ""),
                    sx, pY + 6, search.isEmpty() ? TEXT_DIM : TEXT_SUB);
        }
        String close = "✕";
        int cx = pX + PANEL_W - 16;
        boolean hClose = mx >= cx - 8 && mx <= cx + 8 && my >= pY + 2 && my <= pY + 18;
        Deception.fontManager.s14.drawString(close, cx - Deception.fontManager.s14.getStringWidth(close) / 2f,
                pY + 4, hClose ? 0xFFFF453A : TEXT_DIM);
    }

    // ═══════════════════════════════════════════════════════
    //  主内容
    // ═══════════════════════════════════════════════════════

    void drawContent() {
        int cx = pX + SIDEBAR_W + 4, cw = PANEL_W - SIDEBAR_W - 8;
        int top = pY + TITLE_H + 2, ch = PANEL_H - TITLE_H - 6;
        if (sel == Category.SETTINGS) { drawSettings(cx, top, cw, ch); return; }
        drawModules(cx, top, cw, ch);
    }

    // ═══════════════════════════════════════════════════════
    //  模块列表
    // ═══════════════════════════════════════════════════════

    void drawModules(int x, int y, int w, int h) {
        List<Module> mods = getModules();
        if (mods.isEmpty()) { Deception.fontManager.s14.drawString("No modules", x + w / 2f - 30, y + 40, TEXT_DIM); return; }

        // 预计算高度（使用动画高度）
        int[] mhs = new int[mods.size()];
        int totalH = 0;
        for (int i = 0; i < mods.size(); i++) {
            mhs[i] = getAnimatedModuleHeight(mods.get(i));
            totalH += mhs[i] + CARD_GAP;
        }
        totalH -= CARD_GAP;

        int maxScroll = Math.max(0, totalH - h + 10);
        if (scroll > 0) scroll = 0;
        if (scroll < -maxScroll) scroll = -maxScroll;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glScissor((int) (x * sf), (int) ((sr.getScaledHeight() - (y + h)) * sf),
                Math.max(0, (int) (w * sf)), Math.max(0, (int) (h * sf)));

        int dy = y + (int) smoothScroll;
        for (int i = 0; i < mods.size(); i++) {
            if (dy + mhs[i] >= y && dy <= y + h) drawModule(mods.get(i), x, dy, w, mhs[i]);
            dy += mhs[i] + CARD_GAP;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (totalH > h) {
            float thumbH = Math.max(16, (float) h / totalH * h);
            float thumbY = y + (float) -smoothScroll / totalH * h;
            RoundedUtils.drawRound(x + w - 2, y, 2, h, 1, new Color(0x18202840, true));
            RoundedUtils.drawRound(x + w - 2, thumbY, 2, thumbH, 1, new Color(TEXT_DIM));
        }
    }

    /** 获取模块的动画高度（展开/折叠带平滑过渡） */
    int getAnimatedModuleHeight(Module m) {
        String key = m.getName();
        boolean isExpanded = exp.contains(key);
        int baseH = ROW_H + 2;
        int expandedH = baseH;
        if (isExpanded) {
            expandedH += 2 + SET_H;
            List<Property<?>> props = Deception.propertyManager.properties.get(m.getClass());
            if (props != null) expandedH += props.size() * SET_H;
        }

        ContinualAnimation anim = expandAnims.get(key);
        if (anim == null) {
            anim = new ContinualAnimation();
            expandAnims.put(key, anim);
        }

        int target = isExpanded ? expandedH : baseH;
        anim.animate(target, 250);
        return Math.max(baseH, (int) anim.getOutput());
    }

    void drawModule(Module m, int x, int y, int w, int h) {
        boolean on = m.isEnabled(), expd = exp.contains(m.getName());
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + ROW_H;

        // ── 悬停动画 ──
        String hKey = "mod_" + m.getName();
        RiseAnim hAnim = hoverAnims.computeIfAbsent(hKey, k -> {
            RiseAnim a = new RiseAnim(Easing.EASE_OUT_QUAD, 150);
            a.setValue(0);
            a.run(0);
            return a;
        });
        hAnim.run(hover ? 1 : 0);
        double hoverFactor = hAnim.getValue();

        // ── 卡片背景（带悬停颜色过渡） ──
        int cardColor = on ? MODULE_BG_ON : MODULE_BG_OFF;
        int hoverColor = on ? 0x38285070 : 0x20202840;
        int finalColor = lerpColor(cardColor, hoverColor, (float) hoverFactor);
        RoundedUtils.drawRound(x, y, w, h, 8, new Color(finalColor, true));

        // ── 左侧状态条（开关状态颜色过渡） ──
        int barColor = lerpColor(TEXT_DIM, GREEN, on ? 1f : 0.5f);
        RoundedUtils.drawRound(x + 2, y + 5, 2, ROW_H - 10, 1, new Color(barColor));

        // ── 模块名称 ──
        String prefix = expd ? "▼ " : "▶ ";
        int nameColor = on ? TEXT : TEXT_DIM;
        Deception.fontManager.s14.drawString(prefix + m.getName(), x + 12,
                y + (ROW_H - Deception.fontManager.s14.getHeight()) / 2f + 1, nameColor);

        String[] suffix = m.getSuffix();
        if (suffix != null && suffix.length > 0 && suffix[0] != null && !suffix[0].isEmpty()) {
            Deception.fontManager.s12.drawString(suffix[0],
                    x + 14 + Deception.fontManager.s14.getStringWidth(prefix + m.getName()),
                    y + (ROW_H - Deception.fontManager.s12.getHeight()) / 2f + 1, TEXT_DIM);
        }

        // ── 状态指示灯（带平滑开关动画） ──
        drawToggleDot(x + w - 14, y + ROW_H / 2f - 3, 6, on);

        if (!expd) return;

        // ── 展开设置区 ──
        int sy = y + ROW_H + 2;
        boolean rb = rebind != null && rebind.equals(m.getName());
        boolean kHover = mx >= x + 4 && mx <= x + w / 2f - 2 && my >= sy && my <= sy + SET_H;
        RoundedUtils.drawRound(x + 4, sy, w / 2f - 2, SET_H - 2, 5,
                new Color(rb ? 0x40284060 : (kHover ? INPUT_BG : 0x10141828)));
        String keyName = m.getKey() == 0 ? "NONE" : Keyboard.getKeyName(m.getKey());
        Deception.fontManager.s12.drawString(rb ? "§7Press key..." : ("§8Bind: §7" + keyName),
                x + 10, sy + (SET_H - 2 - Deception.fontManager.s12.getHeight()) / 2f, rb ? TEXT : TEXT_SUB);
        sy += SET_H;

        List<Property<?>> props = Deception.propertyManager.properties.get(m.getClass());
        if (props != null) for (Property<?> p : props) {
            drawSetting(p, x + 4, sy, w - 8);
            sy += SET_H;
        }
    }

    // ═══════════════════════════════════════════════════════
    //  设置项
    // ═══════════════════════════════════════════════════════

    void drawSetting(Property<?> p, int x, int y, int w) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + SET_H - 1;
        if (hover) RoundedUtils.drawRound(x, y, w, SET_H - 1, 5, new Color(0x15FFFFFF, true));

        if (p instanceof BooleanProperty) {
            boolean val = ((BooleanProperty) p).getValue();
            drawAnimatedSwitch(x + w - 34, y + (SET_H - 18) / 2f, 34, 18, val, "sp_" + p.getName());
            Deception.fontManager.s12.drawString(p.getName(), x + 6,
                    y + (SET_H - Deception.fontManager.s12.getHeight()) / 2f, TEXT);

        } else if (p instanceof ModeProperty) {
            ModeProperty mp = (ModeProperty) p;
            int mw = (int) Math.min(w * 0.55f, 220);
            int pillX = x + w - mw;
            int pillY = y + (SET_H - 18) / 2;

            // Background pill
            RoundedUtils.drawRound(pillX, pillY, mw, 18, 9, new Color(INPUT_BG, true));

            String modeName = mp.getModeString();
            int modeW = Deception.fontManager.s12.getStringWidth(modeName);
            int arrowH = 18;

            // Left arrow
            Deception.fontManager.s12.drawString("◀", pillX + 3,
                    pillY + (arrowH - Deception.fontManager.s12.getHeight()) / 2f + 1, TEXT_DIM);

            // Mode name (centered)
            Deception.fontManager.s12.drawString(modeName,
                    pillX + (mw - modeW) / 2f,
                    pillY + (arrowH - Deception.fontManager.s12.getHeight()) / 2f + 1, 0xFFFFFFFF);

            // Right arrow
            Deception.fontManager.s12.drawString("▶", pillX + mw - 3 - Deception.fontManager.s12.getStringWidth("▶"),
                    pillY + (arrowH - Deception.fontManager.s12.getHeight()) / 2f + 1, TEXT_DIM);

            // Mode count indicator
            if (mp.getModes().length > 4) {
                String count = "§8" + (mp.getValue() + 1) + "/" + mp.getModes().length;
                int cw = Deception.fontManager.s12.getStringWidth(count);
                Deception.fontManager.s12.drawString(count, x + w - mw - cw - 4,
                        y + (SET_H - Deception.fontManager.s12.getHeight()) / 2f + 1, TEXT_DIM);
            }

            Deception.fontManager.s12.drawString(p.getName(), x + 6,
                    y + (SET_H - Deception.fontManager.s12.getHeight()) / 2f, TEXT);

        } else if (p instanceof FloatProperty || p instanceof IntProperty || p instanceof PercentProperty) {
            double min, max, val;
            String display;
            if (p instanceof FloatProperty) {
                FloatProperty fp = (FloatProperty) p;
                min = fp.getMinimum(); max = fp.getMaximum(); val = fp.getValue();
                display = String.format("%.1f", val);
            } else if (p instanceof IntProperty) {
                IntProperty ip = (IntProperty) p;
                min = ip.getMinimum(); max = ip.getMaximum(); val = ip.getValue();
                display = String.valueOf((int) val);
            } else {
                PercentProperty pp = (PercentProperty) p;
                min = pp.getMinimum(); max = pp.getMaximum(); val = pp.getValue();
                display = (int) val + "%";
            }
            Deception.fontManager.s12.drawString(p.getName(), x + 6, y + 1, TEXT);
            Deception.fontManager.s12.drawString(display,
                    x + 8 + Deception.fontManager.s12.getStringWidth(p.getName()), y + 2, TEXT_SUB);
            float slY = y + SET_H - 7, slW = w - 10;
            float pct = (float) ((val - min) / Math.max(max - min, 0.001));
            RoundedUtils.drawRound(x + 5, slY, slW, 2, 1, new Color(INPUT_BG));
            RoundedUtils.drawRound(x + 5, slY, slW * pct, 2, 1, new Color(ACCENT));
            // 滑块旋钮（带发光效果）
            float knobX = x + 5 + slW * pct;
            RoundedUtils.drawRound(knobX - 2.5f, slY - 1.5f, 5, 5, 2.5f, new Color(0xFFFFFFFF));
            RoundedUtils.drawRound(knobX - 1.5f, slY - 0.5f, 3, 3, 1.5f, new Color(ACCENT));
        }
    }

    /** 带动画的状态指示灯 */
    void drawToggleDot(float x, float y, float size, boolean on) {
        String key = "dot_" + x + "_" + y;
        ToggleAnim anim = toggleAnims.computeIfAbsent(key, k -> {
            ToggleAnim a = new ToggleAnim();
            a.target = on ? 1f : 0f;
            a.current = a.target;
            return a;
        });
        float target = on ? 1f : 0f;
        if (target != anim.target) anim.target = target;
        anim.current += (anim.target - anim.current) * 0.18f;
        if (Math.abs(anim.current - anim.target) < 0.005f) anim.current = anim.target;

        int dotColor = lerpColor(TEXT_DIM, GREEN, anim.current);
        float dotScale = 0.7f + 0.3f * anim.current;
        float dotSize = size * dotScale;

        RoundedUtils.drawRound(x + (size - dotSize) / 2f, y + (size - dotSize) / 2f,
                dotSize, dotSize, dotSize / 2f, new Color(dotColor));
    }

    /** 带动画平滑过渡的开关 */
    void drawAnimatedSwitch(float x, float y, float w, float h, boolean on, String key) {
        ToggleAnim anim = toggleAnims.computeIfAbsent(key, k -> {
            ToggleAnim a = new ToggleAnim();
            a.target = on ? 1f : 0f;
            a.current = a.target;
            return a;
        });
        float target = on ? 1f : 0f;
        if (target != anim.target) anim.target = target;
        anim.current += (anim.target - anim.current) * 0.18f;
        if (Math.abs(anim.current - anim.target) < 0.005f) anim.current = anim.target;

        float t = anim.current;
        float r = h / 2f;

        // 轨道（颜色过渡）
        int track = lerpColor(TOGGLE_OFF, 0xFF34C759, t);
        RoundedUtils.drawRound(x, y, w, h, r, new Color(track, true));

        // 旋钮（带平滑位移 + 阴影）
        float ks = h - 4;
        float kx = x + 2 + (w - ks - 4) * t;
        float ky = y + 2;

        // 阴影
        RoundedUtils.drawRound(kx + 0.5f, ky + 0.5f, ks, ks, ks / 2f, new Color(0x40000000, true));
        // 旋钮
        RoundedUtils.drawRound(kx, ky, ks, ks, ks / 2f, new Color(0xFFFFFFFF));
    }

    /** 旧版 Switch 方法保留兼容（内部调用新版） */
    void drawSwitch(float x, float y, float w, float h, boolean on) {
        float r = h / 2f;
        RoundedUtils.drawRound(x, y, w, h, r, new Color(on ? 0xFF34C759 : 0xFF39393D));
        float ks = h - 4;
        float kx = x + 2 + (w - ks - 4) * (on ? 1 : 0);
        RoundedUtils.drawRound(kx + 0.5f, y + 2, ks, ks, ks / 2f, new Color(0x40000000, true));
        RoundedUtils.drawRound(kx, y + 2, ks, ks, ks / 2f, new Color(0xFFFFFFFF));
    }

    // ═══════════════════════════════════════════════════════
    //  Settings 配置面板
    // ═══════════════════════════════════════════════════════

    void drawSettings(int x, int y, int w, int h) {
        Deception.fontManager.s14.drawString("Configuration", x + 6, y + 2, ACCENT);
        y += 22;

        int inputW = w - 110;
        boolean hInput = mx >= x + 4 && mx <= x + 4 + inputW && my >= y && my <= y + 28;
        RoundedUtils.drawRound(x + 4, y, inputW, 28, 6, new Color(hInput || editingConfig ? 0x38304868 : 0x181C2E, true));
        String nameDisp = editingConfig
                ? configName + (System.currentTimeMillis() % 800 > 400 ? "▍" : "")
                : (configName.isEmpty() ? "§8Name your config..." : "§7" + configName);
        Deception.fontManager.s12.drawString(nameDisp, x + 12, y + (28 - Deception.fontManager.s12.getHeight()) / 2f + 1,
                editingConfig || !configName.isEmpty() ? TEXT : TEXT_DIM);

        boolean hSave = mx >= x + 8 + inputW && mx <= x + 8 + inputW + 96 && my >= y && my <= y + 28;
        RoundedUtils.drawRound(x + 8 + inputW, y + 2, 96, 24, 6, new Color(hSave ? ACCENT : 0x202844));
        Deception.fontManager.s12.drawString("Save Config", x + 8 + inputW + (96 - Deception.fontManager.s12.getStringWidth("Save Config")) / 2f,
                y + (24 - Deception.fontManager.s12.getHeight()) / 2f + 1, 0xFFFFFFFF);
        y += 38;

        RoundedUtils.drawRound(x + 6, y, w - 12, 1, 0.5f, new Color(0x20284060, true));
        y += 8;

        Deception.fontManager.s12.drawString("Saved Configurations", x + 6, y, TEXT_SUB);
        y += 18;

        if (savedConfigs.isEmpty()) {
            Deception.fontManager.s12.drawString("§8No saved configs", x + 10, y + 8, TEXT_DIM);
            return;
        }

        int listH = h - (y - (pY + TITLE_H + 2 + 22)) - 4;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr2 = new ScaledResolution(mc);
        int sf2 = sr2.getScaleFactor();
        GL11.glScissor((int)(x * sf2), (int)((sr2.getScaledHeight() - (pY + TITLE_H + 2 + 22 + h)) * sf2),
                Math.max(0, (int)(w * sf2)), Math.max(0, (int)(listH * sf2)));

        for (String cfg : savedConfigs) {
            boolean hover = mx >= x + 2 && mx <= x + w - 28 && my >= y && my <= y + 26;
            boolean active = cfg.equals(configName);

            RoundedUtils.drawRound(x + 2, y, w - 28, 26, 6, new Color(active ? 0x222A4A : (hover ? 0x1A1E32 : 0x121624), true));

            if (active) RoundedUtils.drawRound(x + 4, y + 4, 3, 18, 1.5f, new Color(ACCENT));

            Deception.fontManager.s12.drawString((active ? "▸ " : "  ") + cfg, x + 14, y + (26 - Deception.fontManager.s12.getHeight()) / 2f + 1,
                    active ? ACCENT : (hover ? TEXT : TEXT_SUB));

            if (active) {
                Deception.fontManager.s12.drawString("loaded", x + 18 + Deception.fontManager.s12.getStringWidth("  " + cfg),
                        y + (26 - Deception.fontManager.s12.getHeight()) / 2f + 1, GREEN);
            }

            boolean hDel = mx >= x + w - 24 && mx <= x + w - 4 && my >= y && my <= y + 26;
            RoundedUtils.drawRound(x + w - 24, y + 5, 20, 16, 4, new Color(hDel ? 0x442028 : 0x181A22, true));
            Deception.fontManager.s12.drawString("✕", x + w - 24 + (20 - Deception.fontManager.s12.getStringWidth("✕")) / 2f,
                    y + (16 - Deception.fontManager.s12.getHeight()) / 2f + 1, hDel ? 0xFFFF453A : TEXT_DIM);

            y += 30;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    // ═══════════════════════════════════════════════════════
    //  工具
    // ═══════════════════════════════════════════════════════

    private List<Module> getModules() {
        List<Module> r = new ArrayList<>();
        for (Module m : Deception.moduleManager.modules.values())
            if (m.getCategory() == sel) r.add(m);
        r.sort(Comparator.comparing(Module::getName));
        if (!search.isEmpty()) { String low = search.toLowerCase(); r.removeIf(m -> !m.getName().toLowerCase().contains(low)); }
        return r;
    }

    /** 颜色插值 */
    private int lerpColor(int from, int to, float t) {
        if (t <= 0) return from;
        if (t >= 1) return to;
        int a = (int) (((from >> 24) & 0xFF) * (1 - t) + ((to >> 24) & 0xFF) * t);
        int r = (int) (((from >> 16) & 0xFF) * (1 - t) + ((to >> 16) & 0xFF) * t);
        int g = (int) (((from >> 8) & 0xFF) * (1 - t) + ((to >> 8) & 0xFF) * t);
        int b = (int) ((from & 0xFF) * (1 - t) + (to & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ═══════════════════════════════════════════════════════
    //  鼠标事件
    // ═══════════════════════════════════════════════════════

    @Override protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (mx >= pX + PANEL_W - 24 && mx <= pX + PANEL_W && my >= pY + 2 && my <= pY + 18) { mc.displayGuiScreen(null); return; }
        if (mx >= pX + 6 && mx <= pX + SIDEBAR_W - 6) {
            int sy = pY + 60;
            for (Category c : Category.values()) {
                if (c == Category.CONFIG) continue;
                if (my >= sy && my <= sy + CAT_H) {
                    // 切换分类时触发动画
                    if (c != sel) {
                        categoryTargetY = getCategoryY(c);
                        categoryAnim.run(categoryTargetY);
                    }
                    sel = c; scroll = 0; editingConfig = false; return;
                }
                sy += CAT_H + 2;
            }
            return;
        }
        int cx = pX + SIDEBAR_W + 4, cw = PANEL_W - SIDEBAR_W - 8;
        int top = pY + TITLE_H + 2, ch = PANEL_H - TITLE_H - 6;
        if (sel == Category.SETTINGS) { handleSettingsClick(mx, my); return; }

        List<Module> mods = getModules();
        int dy = top + (int) smoothScroll;
        for (Module m : mods) {
            int mh = getAnimatedModuleHeight(m);
            if (dy + mh >= top && dy <= top + ch) {
                if (mx >= cx + cw - 20 && mx <= cx + cw - 8 && my >= dy && my <= dy + ROW_H) { m.toggle(); return; }
                if (mx >= cx && mx <= cx + cw && my >= dy && my <= dy + ROW_H) {
                    if (btn == 1) { if (exp.contains(m.getName())) exp.remove(m.getName()); else exp.add(m.getName()); }
                    else m.toggle();
                    return;
                }
                if (exp.contains(m.getName())) {
                    int sy = dy + ROW_H + 2;
                    if (mx >= cx + 4 && mx <= cx + cw / 2f - 2 && my >= sy && my <= sy + SET_H) { rebind = m.getName(); return; }
                    sy += SET_H;
                    List<Property<?>> props = Deception.propertyManager.properties.get(m.getClass());
                    if (props != null) for (Property<?> p : props) {
                        if (mx >= cx + 4 && mx <= cx + cw - 4 && my >= sy && my <= sy + SET_H - 1) { handleSettingClick(p, mx, my, btn, cx + 4, sy, cw - 8); return; }
                        sy += SET_H;
                    }
                }
            }
            dy += mh + CARD_GAP;
        }
    }

    void handleSettingsClick(int mx, int my) {
        int x = pX + SIDEBAR_W + 4, y = pY + TITLE_H + 2 + 22, w = PANEL_W - SIDEBAR_W - 8, inputW = w - 100;
        if (mx >= x + 4 && mx <= x + 4 + inputW && my >= y && my <= y + 22) { editingConfig = true; return; }
        if (mx >= x + 8 + inputW && mx <= x + 8 + inputW + 86 && my >= y + 1 && my <= y + 21) {
            editingConfig = false; if (!configName.isEmpty()) { new Config(configName, false).save(); refreshConfigs(); } return;
        }
        y += 38 + 8 + 18;
        for (String cfg : savedConfigs) {
            if (mx >= x + w - 24 && mx <= x + w - 4 && my >= y && my <= y + 26) {
                new java.io.File("./config/Deception/", cfg + ".json").delete();
                refreshConfigs();
                return;
            }
            if (mx >= x + 2 && mx <= x + w - 28 && my >= y && my <= y + 26) { configName = cfg; new Config(cfg, false).load(); refreshConfigs(); return; }
            y += 30;
        }
    }

    void handleSettingClick(Property<?> p, int mx, int my, int btn, float px, float py, float pw) {
        if (p instanceof BooleanProperty) { ((BooleanProperty) p).setValue(!((BooleanProperty) p).getValue()); }
        else if (p instanceof ModeProperty) {
            ModeProperty mp = (ModeProperty) p;
            float pillW = Math.min(pw * 0.55f, 220), pillX = px + pw - pillW, pillY = py + (SET_H - 18) / 2f;
            if (mx >= pillX && mx <= pillX + pillW && my >= pillY && my <= pillY + 18) {
                float third = pillW / 3;
                if (mx < pillX + third) { mp.previousMode(); }
                else if (mx > pillX + pillW - third) { mp.nextMode(); }
            } else { if (btn == 0) mp.nextMode(); else mp.previousMode(); }
        } else if (p instanceof FloatProperty || p instanceof IntProperty || p instanceof PercentProperty) {
            float slY = py + SET_H - 7, slW = pw - 10;
            if (my >= slY - 2 && my <= slY + 4) {
                float pct = Math.max(0, Math.min(1, (mx - px - 5) / slW));
                if (p instanceof FloatProperty) { FloatProperty fp = (FloatProperty) p; fp.setValue(fp.getMinimum() + (fp.getMaximum() - fp.getMinimum()) * pct); }
                else if (p instanceof IntProperty) { IntProperty ip = (IntProperty) p; ip.setValue((int) (ip.getMinimum() + (ip.getMaximum() - ip.getMinimum()) * pct)); }
                else { PercentProperty pp = (PercentProperty) p; pp.setValue((int) (pp.getMinimum() + (pp.getMaximum() - pp.getMinimum()) * pct)); }
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  键盘
    // ═══════════════════════════════════════════════════════

    @Override protected void keyTyped(char c, int k) throws IOException {
        if (rebind != null) {
            if (k == Keyboard.KEY_ESCAPE) { Module m = getModule(rebind); if (m != null) m.setKey(0); rebind = null; return; }
            if (k == Keyboard.KEY_DELETE || k == Keyboard.KEY_BACK) k = 0;
            Module m = getModule(rebind); if (m != null) m.setKey(k); rebind = null; return;
        }
        if (editingConfig) {
            if (k == Keyboard.KEY_ESCAPE || k == Keyboard.KEY_RETURN) { editingConfig = false; return; }
            if (k == Keyboard.KEY_BACK || k == Keyboard.KEY_DELETE) { if (!configName.isEmpty()) configName = configName.substring(0, configName.length() - 1); return; }
            if (c >= 32 && c < 127 && configName.length() < 30) configName += c; return;
        }
        if (k == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
        if (k == Keyboard.KEY_BACK || k == Keyboard.KEY_DELETE) { if (!search.isEmpty()) search = search.substring(0, search.length() - 1); return; }
        if (c >= 32 && c < 127) search += c;
    }

    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) scroll += d > 0 ? 24 : -24;
    }

    private Module getModule(String name) {
        for (Module m : Deception.moduleManager.modules.values())
            if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }
}