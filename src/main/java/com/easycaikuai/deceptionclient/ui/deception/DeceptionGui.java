package com.easycaikuai.deceptionclient.ui.deception;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
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
import com.easycaikuai.deceptionclient.util.shader.BlurUtils;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Deception ClickGui —— "Nebula" 深紫星空主题。
 *
 * <p>三阶段渲染：① 基础形状（毛玻璃面板 / 卡片 / 控件底）→ ② Bloom 光晕（开启的开关、滑条填充、
 * 选中分类、激活模块侧条、标题光晕等所有强调元素一次性进 bloom buffer 产生柔和发光）
 * → ③ 锐利前景（文字 / 旋钮 / 描边），保证文字清晰。
 * <p>状态机 / 坐标 / 命中检测 / 动画 / 交互与旧版完全一致，仅替换绘制与配色。
 */
public class DeceptionGui extends GuiScreen {

    // ─── 布局尺寸（绘制与命中检测共用，勿随意改动）───────────
    private static final int SIDEBAR_W = 112;
    private static final int CAT_H = 32;
    private static final int TITLE_H = 46;
    private static final int ROW_H = 32;
    private static final int SET_H = 28;
    private static final int CARD_GAP = 4;
    private static final int RADIUS = 14;
    private static final int CARD_R = 8;

    private int PANEL_W, PANEL_H;

    // ─── Nebula 深紫星空调色板 ───────────────────────────────
    private static final int DIM         = 0x90000000;   // 背景压暗
    private static final int PANEL_GLASS  = 0xE8100C1A;    // 深紫黑玻璃
    private static final int PANEL_SOLID  = 0xF014101C;    // 面板实底（可读性）
    private static final int CARD         = 0xE61B1733;    // 模块卡
    private static final int CARD_ON      = 0xE6241A38;    // 模块卡（开启，紫调）
    private static final int CARD_HOVER   = 0x10FFFFFF;    // 悬停叠加
    private static final int ACCENT       = 0xFFA855F7;    // violet-500 主紫
    private static final int ACCENT_BR    = 0xFFC084FC;    // violet-400 亮紫
    private static final int ACCENT_DK    = 0xFF7C3AED;    // violet-600 深紫
    private static final int TEXT         = 0xFFF3EEFF;    // 暖白
    private static final int TEXT_SUB     = 0xFF9B93B4;    // 次级
    private static final int TEXT_DIM     = 0xFF5F5876;    // 暗淡
    private static final int SEP          = 0x18FFFFFF;    // 分隔线
    private static final int TOGGLE_OFF   = 0xFF2A2540;    // 开关关态
    private static final int TRACK        = 0xFF201B30;    // 滑条轨道
    private static final int FIELD        = 0x1AFFFFFF;    // 输入/控件底
    private static final int BORDER       = 0x55A855F7;    // 面板紫描边
    private static final int RED          = 0xFFFF5C6E;
    private static final int GREEN        = 0xFF5BE58F;

    private static Category sel = Category.COMBAT;
    private static final Set<String> exp = new HashSet<>();
    private static String rebind = null;
    private static float scroll = 0, smoothScroll = 0;
    private static String search = "";
    private int mx, my, pX, pY;
    private float catAnimatedY = 0;

    private String configName = "default";
    private boolean editingConfig = false;
    private List<String> savedConfigs = new ArrayList<>();
    private static final File CONFIG_DIR = new File("./config/Deception/");

    // ─── 动画系统 ───────────────────────────────────────────
    private final RiseAnim openAnim = new RiseAnim(Easing.EASE_OUT_QUINT, 400);
    private final RiseAnim categoryAnim = new RiseAnim(Easing.EASE_OUT_CIRC, 250);
    private float categoryTargetY = 0;
    private final Map<String, ContinualAnimation> expandAnims = new HashMap<>();

    private static class ToggleAnim { float target, current; }
    private final Map<String, ToggleAnim> toggleAnims = new HashMap<>();
    private final Map<String, RiseAnim> hoverAnims = new HashMap<>();

    // ─── 布局助手（绘制与命中检测共用）──────────────────────
    private int contentX() { return pX + SIDEBAR_W + 6; }
    private int contentW() { return PANEL_W - SIDEBAR_W - 14; }
    private int contentTop() { return pY + TITLE_H + 8; }
    private int contentH() { return PANEL_H - TITLE_H - 16; }
    private int sidebarListTop() { return pY + 56; }

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
        PANEL_W = Math.min(Math.max(width * 3 / 4, 520), 680);
        PANEL_H = Math.min(height * 5 / 6, 480);
        pX = (width - PANEL_W) / 2;
        pY = (height - PANEL_H) / 2;
        refreshConfigs();

        openAnim.setValue(0);
        openAnim.run(1);

        categoryTargetY = getCategoryY(sel);
        categoryAnim.setValue(categoryTargetY);
        categoryAnim.run(categoryTargetY);
    }

    // ═══════════════════════════════════════════════════════
    //  主绘制：三阶段（基础 → Bloom 光晕 → 锐利前景）
    // ═══════════════════════════════════════════════════════

    @Override
    public void drawScreen(int mx, int my, float pt) {
        this.mx = mx; this.my = my;

        // ── 平滑滚动 ──
        smoothScroll += (scroll - smoothScroll) * 0.28f;
        if (Math.abs(scroll - smoothScroll) < 0.5f) smoothScroll = scroll;

        // ── 开启动画 ──
        openAnim.run(1);
        double openProgress = openAnim.getValue();
        drawRect(0, 0, width, height, DIM);

        float openScale = 0.94f + 0.06f * (float) openProgress;

        GL11.glPushMatrix();
        GL11.glTranslatef(pX + PANEL_W / 2f, pY + PANEL_H / 2f, 0);
        GL11.glScalef(openScale, openScale, 1);
        GL11.glTranslatef(-pX - PANEL_W / 2f, -pY - PANEL_H / 2f, 0);

        categoryAnim.run(categoryTargetY);
        catAnimatedY = (float) categoryAnim.getValue();

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // ── 面板柔光晕（bloom halo）──
        BlurUtils.prepareBloom();
        RoundedUtils.drawRound(pX, pY, PANEL_W, PANEL_H, RADIUS, new Color(0x267C3AED, true));
        // 顶部紫色渐变 accent 条（光晕源）
        RoundedUtils.drawGradientHorizontal(pX + 1, pY + 1, PANEL_W - 2, 3, 2,
                new Color(0x66A855F7, true), new Color(0x66C084FC, true));
        BlurUtils.bloomEnd(3, 9f);

        // ── 毛玻璃面板 ──
        BlurUtils.prepareBlur();
        RoundedUtils.drawRound(pX, pY, PANEL_W, PANEL_H, RADIUS, new Color(PANEL_GLASS, true));
        BlurUtils.blurEnd(2, 8f);

        // ── 面板实底（保证文字可读，保留部分玻璃质感）──
        RoundedUtils.drawRound(pX, pY, PANEL_W, PANEL_H, RADIUS, new Color(PANEL_SOLID, true));
        RoundedUtils.drawRoundOutline(pX, pY, PANEL_W, PANEL_H, RADIUS, 1f,
                new Color(0x00000000, true), new Color(BORDER, true));

        // ── 阶段 ① 基础形状 ──
        drawSidebarBase();
        drawNavBarBase();
        drawContentBase();

        // ── 阶段 ② Bloom 光晕（所有强调元素一次性进 buffer）──
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        BlurUtils.prepareBloom();
        drawSidebarGlow();
        drawNavBarGlow();
        drawContentGlow();
        BlurUtils.bloomEnd(4, 5f);

        // ── 阶段 ③ 锐利前景（文字 / 旋钮 / 描边）──
        drawSidebarText();
        drawNavBarText();
        drawContentText();

        GL11.glPopMatrix();
    }

    @Override public boolean doesGuiPauseGame() { return true; }
    @Override public void onGuiClosed() { rebind = null; search = ""; editingConfig = false; }

    // ═══════════════════════════════════════════════════════
    //  内容分发
    // ═══════════════════════════════════════════════════════

    private void drawContentBase() {
        if (sel == Category.SETTINGS) { drawSettingsBase(contentX(), contentTop(), contentW(), contentH()); return; }
        drawModulesBase(contentX(), contentTop(), contentW(), contentH());
    }
    private void drawContentGlow() {
        if (sel == Category.SETTINGS) { drawSettingsGlow(contentX(), contentTop(), contentW(), contentH()); return; }
        drawModulesGlow(contentX(), contentTop(), contentW(), contentH());
    }
    private void drawContentText() {
        if (sel == Category.SETTINGS) { drawSettingsText(contentX(), contentTop(), contentW(), contentH()); return; }
        drawModulesText(contentX(), contentTop(), contentW(), contentH());
    }

    // ═══════════════════════════════════════════════════════
    //  侧栏
    // ═══════════════════════════════════════════════════════

    void drawSidebarBase() {
        drawRect(pX + SIDEBAR_W, pY + 8, pX + SIDEBAR_W + 1, pY + PANEL_H - 8, SEP);
        int sy = sidebarListTop();
        for (Category c : Category.values()) {
            if (c == Category.CONFIG) continue;
            if (c != sel)
                RoundedUtils.drawRound(pX + 6, sy, SIDEBAR_W - 12, CAT_H, 8, new Color(0x00000000, true));
            sy += CAT_H + 2;
        }
    }

    void drawSidebarGlow() {
        // 选中分类光晕源
        RoundedUtils.drawRound(pX + 6, catAnimatedY, SIDEBAR_W - 12, CAT_H, 8, new Color(0x77A855F7, true));
    }

    void drawSidebarText() {
        // 品牌
        Deception.fontManager.s16.drawString("◆ Deception", pX + 14, pY + 18, ACCENT_BR);
        String ver = Deception.version != null ? "v" + Deception.version : "";
        Deception.fontManager.s12.drawString(ver, pX + 14, pY + 36, TEXT_DIM);

        // 选中胶囊（锐利）
        RoundedUtils.drawRound(pX + 6, catAnimatedY, SIDEBAR_W - 12, CAT_H, 8, new Color(0x33A855F7, true));

        int sy = sidebarListTop();
        for (Category c : Category.values()) {
            if (c == Category.CONFIG) continue;
            boolean selC = c == sel;
            boolean hover = mx >= pX + 6 && mx <= pX + SIDEBAR_W - 6 && my >= sy && my <= sy + CAT_H;
            if (!selC && hover)
                RoundedUtils.drawRound(pX + 6, sy, SIDEBAR_W - 12, CAT_H, 8, new Color(CARD_HOVER, true));
            drawSidebarRow(c, sy, selC);
            sy += CAT_H + 2;
        }
    }

    void drawSidebarRow(Category c, float sy, boolean selected) {
        drawCategoryIcon(c, pX + 14, sy + (CAT_H - 10) / 2f, selected);
        String label = c.getDisplayName();
        Deception.fontManager.s14.drawString(label, pX + 32,
                sy + (CAT_H - Deception.fontManager.s14.getHeight()) / 2f + 1,
                selected ? TEXT : TEXT_SUB);
    }

    private void drawCategoryIcon(Category c, float x, float y, boolean selected) {
        ItemStack stack = iconFor(c);
        if (stack == null) return;
        float scale = 0.6f;
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableBlend();
        GlStateManager.translate(x / scale, y / scale, 0);
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
        GlStateManager.enableBlend();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private ItemStack iconFor(Category c) {
        switch (c) {
            case COMBAT: return new ItemStack(Items.diamond_sword);
            case MOVEMENT: return new ItemStack(Items.diamond_boots);
            case RENDER: return new ItemStack(Items.ender_eye);
            case PLAYER: return new ItemStack(Items.golden_apple);
            case MISC: return new ItemStack(Items.clock);
            case SETTINGS: return new ItemStack(Items.compass);
            default: return null;
        }
    }

    private float getCategoryY(Category cat) {
        int sy = sidebarListTop();
        for (Category c : Category.values()) {
            if (c == Category.CONFIG) continue;
            if (c == cat) return sy;
            sy += CAT_H + 2;
        }
        return sy;
    }

    // ═══════════════════════════════════════════════════════
    //  导航栏
    // ═══════════════════════════════════════════════════════

    void drawNavBarBase() {
        drawRect(pX + SIDEBAR_W + 6, pY + TITLE_H, pX + PANEL_W - 8, pY + TITLE_H + 1, SEP);
        if (sel != Category.SETTINGS && sel != Category.CONFIG) {
            int sw = 150, sh = 26;
            int sx = pX + PANEL_W - 8 - 22 - 6 - sw;
            int sy = pY + (TITLE_H - sh) / 2;
            RoundedUtils.drawRound(sx, sy, sw, sh, 9, new Color(FIELD, true));
        }
        int cx = pX + PANEL_W - 8 - 18, cy = pY + (TITLE_H - 22) / 2;
        boolean hClose = mx >= cx && mx <= cx + 22 && my >= cy && my <= cy + 22;
        RoundedUtils.drawRound(cx, cy, 22, 22, 11, new Color(hClose ? 0x33FF5C6E : 0x10FFFFFF, true));
    }

    void drawNavBarGlow() {
        // 标题光晕源
        String title = sel != null ? sel.getDisplayName() : "";
        int tw = Deception.fontManager.s18.getStringWidth(title);
        RoundedUtils.drawRound(pX + SIDEBAR_W + 10, pY + 10, tw + 8, 26, 8, new Color(0x33A855F7, true));
    }

    void drawNavBarText() {
        String title = sel != null ? sel.getDisplayName() : "";
        Deception.fontManager.s18.drawString(title, pX + SIDEBAR_W + 14,
                pY + (TITLE_H - Deception.fontManager.s18.getHeight()) / 2f + 1, TEXT);

        if (sel != Category.SETTINGS && sel != Category.CONFIG) {
            int sw = 150, sh = 26;
            int sx = pX + PANEL_W - 8 - 22 - 6 - sw;
            int sy = pY + (TITLE_H - sh) / 2;
            boolean cursor = System.currentTimeMillis() % 800 > 400;
            Deception.fontManager.getFont(13).drawString(
                    search.isEmpty() ? "§7Search" : "§f" + search + (cursor ? "§8|" : ""),
                    sx + 10, sy + (sh - Deception.fontManager.getFont(13).getHeight()) / 2f + 1,
                    search.isEmpty() ? TEXT_DIM : TEXT);
        }

        int cx = pX + PANEL_W - 8 - 18, cy = pY + (TITLE_H - 22) / 2;
        boolean hClose = mx >= cx && mx <= cx + 22 && my >= cy && my <= cy + 22;
        Deception.fontManager.s14.drawString("✕", cx + 11 - Deception.fontManager.s14.getStringWidth("✕") / 2f,
                cy + (22 - Deception.fontManager.s14.getHeight()) / 2f + 1, hClose ? RED : TEXT_SUB);
    }

    // ═══════════════════════════════════════════════════════
    //  模块列表
    // ═══════════════════════════════════════════════════════

    private int[] moduleHeights;

    void drawModulesBase(int x, int y, int w, int h) {
        List<Module> mods = getModules();
        if (mods.isEmpty()) return;
        moduleHeights = new int[mods.size()];
        int totalH = 0;
        for (int i = 0; i < mods.size(); i++) { moduleHeights[i] = getAnimatedModuleHeight(mods.get(i)); totalH += moduleHeights[i] + CARD_GAP; }
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
            if (dy + moduleHeights[i] >= y && dy <= y + h) drawModuleBase(mods.get(i), x, dy, w, moduleHeights[i]);
            dy += moduleHeights[i] + CARD_GAP;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    void drawModulesGlow(int x, int y, int w, int h) {
        List<Module> mods = getModules();
        if (mods.isEmpty()) return;
        int dy = y + (int) smoothScroll;
        for (int i = 0; i < mods.size(); i++) {
            int mh = moduleHeights != null && i < moduleHeights.length ? moduleHeights[i] : getAnimatedModuleHeight(mods.get(i));
            if (dy + mh >= y && dy <= y + h) drawModuleGlow(mods.get(i), x, dy, w, mh);
            dy += mh + CARD_GAP;
        }
    }

    void drawModulesText(int x, int y, int w, int h) {
        List<Module> mods = getModules();
        if (mods.isEmpty()) { Deception.fontManager.s14.drawString("No modules", x + 12, y + 16, TEXT_DIM); return; }
        int totalH = 0; for (int mh : moduleHeights) totalH += mh + CARD_GAP; totalH -= CARD_GAP;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glScissor((int) (x * sf), (int) ((sr.getScaledHeight() - (y + h)) * sf),
                Math.max(0, (int) (w * sf)), Math.max(0, (int) (h * sf)));

        int dy = y + (int) smoothScroll;
        for (int i = 0; i < mods.size(); i++) {
            if (dy + moduleHeights[i] >= y && dy <= y + h) drawModuleText(mods.get(i), x, dy, w, moduleHeights[i]);
            dy += moduleHeights[i] + CARD_GAP;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // 滚动条
        if (totalH > h) {
            float thumbH = Math.max(20, (float) h / totalH * h);
            float thumbY = y + (float) -smoothScroll / totalH * h;
            RoundedUtils.drawRound(x + w - 3, y, 2, h, 1, new Color(0x10FFFFFF, true));
            RoundedUtils.drawRound(x + w - 3, thumbY, 2, thumbH, 1, new Color(0x40A855F7, true));
        }
    }

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
        ContinualAnimation anim = expandAnims.computeIfAbsent(key, k -> new ContinualAnimation());
        int target = isExpanded ? expandedH : baseH;
        anim.animate(target, 250);
        return Math.max(baseH, (int) anim.getOutput());
    }

    // ── 单个模块：基础形状 ──
    void drawModuleBase(Module m, int x, int y, int w, int h) {
        boolean on = m.isEnabled(), expd = exp.contains(m.getName());
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + ROW_H;
        String hKey = "mod_" + m.getName();
        RiseAnim hAnim = hoverAnims.computeIfAbsent(hKey, k -> { RiseAnim a = new RiseAnim(Easing.EASE_OUT_QUAD, 150); a.setValue(0); a.run(0); return a; });
        hAnim.run(hover ? 1 : 0);
        float hf = (float) hAnim.getValue();

        int base = lerpColor(on ? CARD_ON : CARD, CARD_HOVER, hf * 0.6f);
        RoundedUtils.drawRoundOutline(x, y, w, h, CARD_R, 1f,
                new Color(base, true), new Color(on ? 0x44A855F7 : 0x10FFFFFF, true));

        if (!expd) return;
        drawRect(x + 10, y + ROW_H, x + w - 10, y + ROW_H + 1, SEP);
    }

    // ── 单个模块：光晕源 ──
    void drawModuleGlow(Module m, int x, int y, int w, int h) {
        boolean on = m.isEnabled();
        if (on) {
            // 激活模块左侧 accent 条光晕
            RoundedUtils.drawRound(x + 1, y + 4, 3, ROW_H - 8, 2, new Color(0x88A855F7, true));
            // 开关轨道光晕
            float sx = x + w - 44, sy = y + (ROW_H - 22) / 2f;
            RoundedUtils.drawRound(sx, sy, 38, 22, 11, new Color(0x77A855F7, true));
        }
        if (!exp.contains(m.getName())) return;
        // 设置项光晕
        int sy = y + ROW_H + 2 + SET_H;
        List<Property<?>> props = Deception.propertyManager.properties.get(m.getClass());
        if (props != null) for (Property<?> p : props) { drawSettingGlow(p, x + 4, sy, w - 8); sy += SET_H; }
    }

    // ── 单个模块：锐利前景（文字 + 旋钮）──
    void drawModuleText(Module m, int x, int y, int w, int h) {
        boolean on = m.isEnabled(), expd = exp.contains(m.getName());

        String prefix = expd ? "▾" : "▸";
        Deception.fontManager.s12.drawString(prefix, x + 10,
                y + (ROW_H - Deception.fontManager.s12.getHeight()) / 2f + 1, on ? ACCENT_BR : TEXT_DIM);
        Deception.fontManager.s14.drawString(m.getName(), x + 24,
                y + (ROW_H - Deception.fontManager.s14.getHeight()) / 2f + 1, on ? TEXT : TEXT_SUB);
        String[] suffix = m.getSuffix();
        if (suffix != null && suffix.length > 0 && suffix[0] != null && !suffix[0].isEmpty())
            Deception.fontManager.s12.drawString(suffix[0],
                    x + 26 + Deception.fontManager.s14.getStringWidth(m.getName()),
                    y + (ROW_H - Deception.fontManager.s12.getHeight()) / 2f + 2, on ? ACCENT_BR : TEXT_DIM);

        // 锐利开关
        drawSwitchSharp(x + w - 44, y + (ROW_H - 22) / 2f, 38, 22, on, "mod_" + m.getName());

        if (!expd) return;
        int sy = y + ROW_H + 2;
        // 绑定键行
        boolean rb = rebind != null && rebind.equals(m.getName());
        boolean kHover = mx >= x + 4 && mx <= x + w / 2f - 2 && my >= sy && my <= sy + SET_H;
        if (rb || kHover) RoundedUtils.drawRound(x + 6, sy + 2, w / 2f - 8, SET_H - 4, 6, new Color(rb ? 0x33A855F7 : FIELD, true));
        String keyName = m.getKey() == 0 ? "NONE" : Keyboard.getKeyName(m.getKey());
        Deception.fontManager.getFont(13).drawString(rb ? "§7Press key..." : ("§8Bind §7" + keyName),
                x + 14, sy + (SET_H - Deception.fontManager.getFont(13).getHeight()) / 2f + 1, rb ? TEXT : TEXT_SUB);
        sy += SET_H;

        List<Property<?>> props = Deception.propertyManager.properties.get(m.getClass());
        if (props != null) {
            for (int i = 0; i < props.size(); i++) {
                if (i > 0) drawRect(x + 12, sy, x + w - 12, sy + 1, SEP);
                drawSettingText(props.get(i), x + 4, sy, w - 8);
                sy += SET_H;
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  设置项（三阶段）
    // ═══════════════════════════════════════════════════════

    void drawSettingGlow(Property<?> p, int x, int y, int w) {
        if (p instanceof BooleanProperty) {
            boolean val = ((BooleanProperty) p).getValue();
            if (val) RoundedUtils.drawRound(x + w - 44, y + (SET_H - 22) / 2f, 38, 22, 11, new Color(0x77A855F7, true));
        } else if (p instanceof FloatProperty || p instanceof IntProperty || p instanceof PercentProperty) {
            double min, max, val;
            if (p instanceof FloatProperty) { FloatProperty fp = (FloatProperty) p; min = fp.getMinimum(); max = fp.getMaximum(); val = fp.getValue(); }
            else if (p instanceof IntProperty) { IntProperty ip = (IntProperty) p; min = ip.getMinimum(); max = ip.getMaximum(); val = ip.getValue(); }
            else { PercentProperty pp = (PercentProperty) p; min = pp.getMinimum(); max = pp.getMaximum(); val = pp.getValue(); }
            float slY = y + SET_H - 8, slX = x + 12, slW = w - 24;
            float pct = (float) ((val - min) / Math.max(max - min, 0.001));
            if (pct > 0)
                RoundedUtils.drawGradientHorizontal(slX, slY, Math.max(2, slW * pct), 4, 2,
                        new Color(0x88A855F7, true), new Color(0x88C084FC, true));
        } else if (p instanceof ModeProperty) {
            float mw = Math.min(w * 0.5f, 200), pillX = x + w - mw, pillY = y + (SET_H - 20) / 2f;
            float segW = mw - 24;
            RoundedUtils.drawRound(pillX + 12, pillY + 2, segW, 16, 7, new Color(0x77A855F7, true));
        }
    }

    void drawSettingText(Property<?> p, int x, int y, int w) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + SET_H - 1;
        if (hover) RoundedUtils.drawRound(x, y, w, SET_H - 1, 6, new Color(0x0CFFFFFF, true));

        if (p instanceof BooleanProperty) {
            boolean val = ((BooleanProperty) p).getValue();
            Deception.fontManager.getFont(13).drawString(p.getName(), x + 12,
                    y + (SET_H - Deception.fontManager.getFont(13).getHeight()) / 2f + 1, TEXT);
            drawSwitchSharp(x + w - 44, y + (SET_H - 22) / 2f, 38, 22, val, "sp_" + p.getName());
        } else if (p instanceof ModeProperty) {
            ModeProperty mp = (ModeProperty) p;
            float mw = Math.min(w * 0.5f, 200), pillX = x + w - mw, pillY = y + (SET_H - 20) / 2f;
            RoundedUtils.drawRound(pillX, pillY, mw, 20, 9, new Color(FIELD, true));
            String modeName = mp.getModeString();
            int modeW = Deception.fontManager.getFont(13).getStringWidth(modeName);
            float segW = mw - 24;
            RoundedUtils.drawRound(pillX + 12, pillY + 2, segW, 16, 7, new Color(ACCENT, true));
            Deception.fontManager.getFont(13).drawString("‹", pillX + 4, pillY + (20 - Deception.fontManager.getFont(13).getHeight()) / 2f + 1, TEXT_DIM);
            Deception.fontManager.getFont(13).drawString(modeName, pillX + 12 + (segW - modeW) / 2f, pillY + (20 - Deception.fontManager.getFont(13).getHeight()) / 2f + 1, TEXT);
            Deception.fontManager.getFont(13).drawString("›", pillX + mw - 4 - Deception.fontManager.getFont(13).getStringWidth("›"), pillY + (20 - Deception.fontManager.getFont(13).getHeight()) / 2f + 1, TEXT_DIM);
            if (mp.getModes().length > 4)
                Deception.fontManager.s12.drawString("§8" + (mp.getValue() + 1) + "/" + mp.getModes().length, x + 12,
                        y + (SET_H - Deception.fontManager.s12.getHeight()) / 2f + 1, TEXT_DIM);
            else Deception.fontManager.getFont(13).drawString(p.getName(), x + 12,
                        y + (SET_H - Deception.fontManager.getFont(13).getHeight()) / 2f + 1, TEXT);
        } else if (p instanceof FloatProperty || p instanceof IntProperty || p instanceof PercentProperty) {
            double min, max, val; String display;
            if (p instanceof FloatProperty) { FloatProperty fp = (FloatProperty) p; min = fp.getMinimum(); max = fp.getMaximum(); val = fp.getValue(); display = String.format("%.1f", val); }
            else if (p instanceof IntProperty) { IntProperty ip = (IntProperty) p; min = ip.getMinimum(); max = ip.getMaximum(); val = ip.getValue(); display = String.valueOf((int) val); }
            else { PercentProperty pp = (PercentProperty) p; min = pp.getMinimum(); max = pp.getMaximum(); val = pp.getValue(); display = (int) val + "%"; }
            Deception.fontManager.getFont(13).drawString(p.getName(), x + 12, y + 4, TEXT);
            int vw = Deception.fontManager.getFont(13).getStringWidth(display);
            Deception.fontManager.getFont(13).drawString("§7" + display, x + w - 12 - vw, y + 4, TEXT_SUB);
            float slY = y + SET_H - 8, slX = x + 12, slW = w - 24;
            float pct = (float) ((val - min) / Math.max(max - min, 0.001));
            RoundedUtils.drawRound(slX, slY, slW, 4, 2, new Color(TRACK, true));
            RoundedUtils.drawGradientHorizontal(slX, slY, Math.max(2, slW * pct), 4, 2, new Color(ACCENT), new Color(ACCENT_BR));
            float knobX = slX + slW * pct;
            RoundedUtils.drawRound(knobX - 4, slY - 3, 10, 10, 5, new Color(0x66000000, true));
            RoundedUtils.drawRound(knobX - 5, slY - 4, 10, 10, 5, new Color(0xFFFFFFFF, true));
        }
    }

    // ── 带平滑动画的开关：轨道 + 旋钮（锐利）──
    private float toggleT(String key, boolean on) {
        ToggleAnim anim = toggleAnims.computeIfAbsent(key, k -> { ToggleAnim a = new ToggleAnim(); a.target = on ? 1f : 0f; a.current = a.target; return a; });
        float target = on ? 1f : 0f;
        if (target != anim.target) anim.target = target;
        anim.current += (anim.target - anim.current) * 0.20f;
        if (Math.abs(anim.current - anim.target) < 0.005f) anim.current = anim.target;
        return anim.current;
    }

    void drawSwitchSharp(float x, float y, float w, float h, boolean on, String key) {
        float t = toggleT(key, on);
        float r = h / 2f;
        int track = lerpColor(TOGGLE_OFF, ACCENT, t);
        RoundedUtils.drawRound(x, y, w, h, r, new Color(track, true));
        float ks = h - 6;
        float kx = x + 3 + (w - ks - 6) * t, ky = y + 3;
        RoundedUtils.drawRound(kx + 0.5f, ky + 0.5f, ks, ks, ks / 2f, new Color(0x66000000, true));
        RoundedUtils.drawRound(kx, ky, ks, ks, ks / 2f, new Color(0xFFFFFFFF, true));
    }

    /** 旧版兼容 */
    void drawSwitch(float x, float y, float w, float h, boolean on) {
        drawSwitchSharp(x, y, w, h, on, "compat_" + x + "_" + y);
    }

    // ═══════════════════════════════════════════════════════
    //  Settings 配置面板（三阶段）
    // ═══════════════════════════════════════════════════════

    void drawSettingsBase(int x, int y, int w, int h) {
        int inputW = w - 104, cy = y + 20;
        boolean hInput = mx >= x + 4 && mx <= x + 4 + inputW && my >= cy && my <= cy + 30;
        RoundedUtils.drawRoundOutline(x + 4, cy, inputW, 30, 8, 1f,
                new Color(hInput || editingConfig ? 0x33A855F7 : FIELD, true),
                new Color(editingConfig ? 0x55A855F7 : 0x10FFFFFF, true));
    }

    void drawSettingsGlow(int x, int y, int w, int h) {
        int inputW = w - 104, cy = y + 20;
        // Save 按钮光晕
        RoundedUtils.drawGradientHorizontal(x + 8 + inputW, cy + 2, 92, 26, 8,
                new Color(0x77A855F7, true), new Color(0x77C084FC, true));
        // 激活配置行光晕
        int ly = cy + 42 + 12 + 20;
        for (String cfg : savedConfigs) {
            if (cfg.equals(configName))
                RoundedUtils.drawRound(x + 4, ly, w - 28, 28, 8, new Color(0x55A855F7, true));
            ly += 32;
        }
    }

    void drawSettingsText(int x, int y, int w, int h) {
        int cy = y;
        Deception.fontManager.s12.drawString("CONFIGURATION", x + 12, cy, TEXT_DIM);
        cy += 20;

        int inputW = w - 104;
        String nameDisp = editingConfig ? configName + (System.currentTimeMillis() % 800 > 400 ? "▍" : "")
                : (configName.isEmpty() ? "§7Name your config..." : "§f" + configName);
        Deception.fontManager.getFont(13).drawString(nameDisp, x + 14,
                cy + (30 - Deception.fontManager.getFont(13).getHeight()) / 2f + 1,
                editingConfig || !configName.isEmpty() ? TEXT : TEXT_DIM);

        boolean hSave = mx >= x + 8 + inputW && mx <= x + 8 + inputW + 92 && my >= cy && my <= cy + 30;
        RoundedUtils.drawGradientHorizontal(x + 8 + inputW, cy + 2, 92, 26, 8,
                new Color(hSave ? ACCENT_BR : ACCENT), new Color(hSave ? ACCENT : ACCENT_DK));
        Deception.fontManager.getFont(13).drawString("Save", x + 8 + inputW + (92 - Deception.fontManager.getFont(13).getStringWidth("Save")) / 2f,
                cy + (26 - Deception.fontManager.getFont(13).getHeight()) / 2f + 3, 0xFFFFFFFF);
        cy += 42;

        drawRect(x + 12, cy, x + w - 12, cy + 1, SEP);
        cy += 12;
        Deception.fontManager.s12.drawString("SAVED CONFIGURATIONS", x + 12, cy, TEXT_DIM);
        cy += 20;

        if (savedConfigs.isEmpty()) { Deception.fontManager.getFont(13).drawString("§7No saved configs", x + 14, cy + 6, TEXT_DIM); return; }

        int listTop = cy, listH = h - (cy - y) - 4;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr2 = new ScaledResolution(mc);
        int sf2 = sr2.getScaleFactor();
        GL11.glScissor((int) (x * sf2), (int) ((sr2.getScaledHeight() - (listTop + listH)) * sf2),
                Math.max(0, (int) (w * sf2)), Math.max(0, (int) (listH * sf2)));

        for (int i = 0; i < savedConfigs.size(); i++) {
            if (i > 0) drawRect(x + 14, cy, x + w - 14, cy + 1, SEP);
            String cfg = savedConfigs.get(i);
            boolean hover = mx >= x + 4 && mx <= x + w - 28 && my >= cy && my <= cy + 28;
            boolean active = cfg.equals(configName);
            RoundedUtils.drawRound(x + 4, cy, w - 28, 28, 8,
                    new Color(active ? 0x33A855F7 : (hover ? 0x14FFFFFF : 0x00FFFFFF), true));
            Deception.fontManager.getFont(13).drawString(cfg, x + 16,
                    cy + (28 - Deception.fontManager.getFont(13).getHeight()) / 2f + 1, active ? TEXT : (hover ? TEXT : TEXT_SUB));
            if (active) {
                String loaded = "loaded";
                int lw = Deception.fontManager.s12.getStringWidth(loaded);
                Deception.fontManager.s12.drawString(loaded, x + w - 44 - lw,
                        cy + (28 - Deception.fontManager.s12.getHeight()) / 2f + 1, GREEN);
            }
            boolean hDel = mx >= x + w - 24 && mx <= x + w - 4 && my >= cy + 4 && my <= cy + 24;
            RoundedUtils.drawRound(x + w - 24, cy + 4, 20, 20, 10, new Color(hDel ? 0x33FF5C6E : 0x18FFFFFF, true));
            Deception.fontManager.s12.drawString("✕", x + w - 24 + (20 - Deception.fontManager.s12.getStringWidth("✕")) / 2f,
                    cy + 4 + (20 - Deception.fontManager.s12.getHeight()) / 2f + 1, hDel ? RED : TEXT_SUB);
            cy += 32;
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
        int cbX = pX + PANEL_W - 8 - 22, cbY = pY + (TITLE_H - 22) / 2;
        if (mx >= cbX && mx <= cbX + 22 && my >= cbY && my <= cbY + 22) { mc.displayGuiScreen(null); return; }

        if (mx >= pX + 6 && mx <= pX + SIDEBAR_W - 6) {
            int sy = sidebarListTop();
            for (Category c : Category.values()) {
                if (c == Category.CONFIG) continue;
                if (my >= sy && my <= sy + CAT_H) {
                    if (c != sel) { categoryTargetY = getCategoryY(c); categoryAnim.run(categoryTargetY); }
                    sel = c; scroll = 0; editingConfig = false; return;
                }
                sy += CAT_H + 2;
            }
            return;
        }

        int cx = contentX(), cw = contentW();
        int top = contentTop(), ch = contentH();
        if (sel == Category.SETTINGS) { handleSettingsClick(mx, my); return; }

        List<Module> mods = getModules();
        int dy = top + (int) smoothScroll;
        for (Module m : mods) {
            int mh = getAnimatedModuleHeight(m);
            if (dy + mh >= top && dy <= top + ch) {
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
        int x = contentX(), y = contentTop(), w = contentW(), inputW = w - 100;
        y += 20;
        if (mx >= x + 4 && mx <= x + 4 + inputW && my >= y && my <= y + 30) { editingConfig = true; return; }
        if (mx >= x + 8 + inputW && mx <= x + 8 + inputW + 92 && my >= y && my <= y + 30) {
            editingConfig = false; if (!configName.isEmpty()) { new Config(configName, false).save(); refreshConfigs(); } return;
        }
        y += 42 + 12 + 20;
        for (String cfg : savedConfigs) {
            if (mx >= x + w - 24 && mx <= x + w - 4 && my >= y + 4 && my <= y + 24) {
                new java.io.File("./config/Deception/", cfg + ".json").delete(); refreshConfigs(); return;
            }
            if (mx >= x + 4 && mx <= x + w - 28 && my >= y && my <= y + 28) { configName = cfg; new Config(cfg, false).load(); refreshConfigs(); return; }
            y += 32;
        }
    }

    void handleSettingClick(Property<?> p, int mx, int my, int btn, float px, float py, float pw) {
        if (p instanceof BooleanProperty) { ((BooleanProperty) p).setValue(!((BooleanProperty) p).getValue()); }
        else if (p instanceof ModeProperty) {
            ModeProperty mp = (ModeProperty) p;
            float pillW = Math.min(pw * 0.5f, 200), pillX = px + pw - pillW, pillY = py + (SET_H - 20) / 2f;
            if (mx >= pillX && mx <= pillX + pillW && my >= pillY && my <= pillY + 20) {
                float third = pillW / 3;
                if (mx < pillX + third) mp.previousMode();
                else if (mx > pillX + pillW - third) mp.nextMode();
            } else { if (btn == 0) mp.nextMode(); else mp.previousMode(); }
        } else if (p instanceof FloatProperty || p instanceof IntProperty || p instanceof PercentProperty) {
            float slY = py + SET_H - 8, slX = px + 12, slW = pw - 24;
            if (my >= slY - 3 && my <= slY + 5) {
                float pct = Math.max(0, Math.min(1, (mx - slX) / slW));
                if (p instanceof FloatProperty) { FloatProperty fp = (FloatProperty) p; fp.setValue(fp.getMinimum() + (fp.getMaximum() - fp.getMinimum()) * pct); }
                else if (p instanceof IntProperty) { IntProperty ip = (IntProperty) p; ip.setValue((int) Math.round(ip.getMinimum() + (ip.getMaximum() - ip.getMinimum()) * pct)); }
                else { PercentProperty pp = (PercentProperty) p; pp.setValue((int) Math.round(pp.getMinimum() + (pp.getMaximum() - pp.getMinimum()) * pct)); }
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
