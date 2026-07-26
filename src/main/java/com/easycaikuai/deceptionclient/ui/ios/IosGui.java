package com.easycaikuai.deceptionclient.ui.ios;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.module.Category;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.Property;
import com.easycaikuai.deceptionclient.property.properties.*;
import com.easycaikuai.deceptionclient.util.animation.Easing;
import com.easycaikuai.deceptionclient.util.animation.RiseAnim;
import com.easycaikuai.deceptionclient.util.animations.advanced.ContinualAnimation;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;

import java.io.IOException;
import java.awt.Color;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * iOS 风格 GUI v2 — 带动画增强
 *
 * 设计语言: iOS 14+ Settings / Control Center
 *   - 紫色品牌标 "Deception"
 *   - 独立圆角卡片（每模块一张，展开时自然增长）
 *   - 全圆角：没有任何直角矩形
 *   - iOS UISwitch 开关（带动画）
 *   - Segment Control 分类切换
 *   - 流畅的开启动画、分类切换、卡片展开
 */
public class IosGui extends GuiScreen {

    // ─── 尺寸 ─────────────────────────────────────────────
    private static final int BANNER_H      = 72;
    private static final int SEGMENT_H     = 36;
    private static final int CARD_GAP      = 8;
    private static final int CELL_H        = 52;
    private static final int SETTING_H     = 44;
    private static final int CARD_RADIUS   = 14;
    private static final int PILL_RADIUS   = 8;
    private static final int SWITCH_W      = 54;
    private static final int SWITCH_H      = 30;

    // ─── iOS 调色板 ───────────────────────────────────────
    private static final int PURPLE        = 0xFFAF52DE;
    private static final int PURPLE_DIM    = 0x60AF52DE;
    private static final int BG_DARK       = 0xCC0C0C0E;
    private static final int CARD_BG       = 0xE81C1C1E;
    private static final int CARD_HOVER    = 0x1528282A;
    private static final int SEPARATOR     = 0x3038383C;
    private static final int TEXT_PRIMARY   = 0xFFF5F5F5;
    private static final int TEXT_SECONDARY = 0xFF8E8E93;
    private static final int TEXT_DIM       = 0xFF636366;
    private static final int ACCENT_BLUE    = 0xFF0A84FF;
    private static final int ACCENT_GREEN   = 0xFF34C759;
    private static final int ACCENT_RED     = 0xFFFF453A;
    private static final int TOGGLE_OFF     = 0xFF39393D;
    private static final int TAB_BG         = 0xFF2C2C2E;
    private static final int SEG_INACTIVE   = 0x1AFFFFFF;
    private static final int SCROLLBAR_BG    = 0x3038383C;
    private static final int SCROLLBAR_THUMB = 0xFF636366;

    // ─── 状态 ─────────────────────────────────────────────
    private Category selectedCategory = Category.COMBAT;
    private final Set<String> expandedModules = new HashSet<>();
    private float scrollOffset = 0f;
    private float smoothScroll = 0f;
    private int mx, my;
    private boolean draggingSlider = false;
    private Property<?> dragProp = null;

    // 开关动画
    private static class ToggleAnim {
        float target, current;
    }
    private final Map<String, ToggleAnim> toggleAnims = new HashMap<>();

    // ─── 动画系统 ───────────────────────────────────────────

    /** 开启动画：面板从中心缩放淡入 */
    private final RiseAnim openAnim = new RiseAnim(Easing.EASE_OUT_QUINT, 500);

    /** 分类切换动画：高亮 pill 平滑移动到目标 */
    private final RiseAnim categoryAnim = new RiseAnim(Easing.EASE_OUT_CIRC, 300);
    private float categoryTargetX = 0;

    /** 模块展开/折叠动画（高度） */
    private final Map<String, ContinualAnimation> expandAnims = new HashMap<>();

    /** 卡片淡入动画（按索引延迟） */
    private final Map<String, RiseAnim> cardFadeAnims = new HashMap<>();

    // ═══════════════════════════════════════════════════════
    //  initGui
    // ═══════════════════════════════════════════════════════
    @Override
    public void initGui() {
        // 初始化分类动画位置
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int edgeGap = Math.max(sw / 14, 12);
        int cw = sw - edgeGap * 2;
        int cx = edgeGap;
        int segW = Math.min(cw, 400);
        int segX = cx + cw / 2 - segW / 2;
        int itemW = segW / 5;

        Category[] cats = {Category.COMBAT, Category.MOVEMENT,
                Category.RENDER, Category.PLAYER, Category.MISC};
        int selIdx = 0;
        for (int i = 0; i < cats.length; i++)
            if (cats[i] == selectedCategory) { selIdx = i; break; }

        categoryTargetX = segX + selIdx * itemW + 2;
        categoryAnim.setValue(categoryTargetX);
        categoryAnim.run(categoryTargetX);
    }

    // ═══════════════════════════════════════════════════════
    //  drawScreen
    // ═══════════════════════════════════════════════════════
    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        this.mx = mx;
        this.my = my;

        // ── 平滑滚动 ──
        smoothScroll += (scrollOffset - smoothScroll) * 0.28f;
        if (Math.abs(scrollOffset - smoothScroll) < 0.5f) smoothScroll = scrollOffset;

        // ── 开启动画 ──
        openAnim.run(1);
        double openProgress = openAnim.getValue();

        // ── 分类切换动画 ──
        categoryAnim.run(categoryTargetX);
        float catAnimX = (float) categoryAnim.getValue();

        drawDefaultBackground();

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        // 主背景（带淡入）
        int bgAlpha = Math.min(255, (int) (220 * openProgress));
        int bgColor = (bgAlpha << 24) | (BG_DARK & 0xFFFFFF);
        drawRect(0, 0, sw, sh, bgColor);

        // 应用开启动画缩放
        float openScale = 0.92f + 0.08f * (float) openProgress;
        GL11.glPushMatrix();
        GL11.glTranslatef(sw / 2f, sh / 2f, 0);
        GL11.glScalef(openScale, openScale, 1);
        GL11.glTranslatef(-sw / 2f, -sh / 2f, 0);

        int edgeGap  = Math.max(sw / 14, 12);
        int cw       = sw - edgeGap * 2;     // content width
        int cx       = edgeGap;               // content x
        int topArea  = BANNER_H + 8 + SEGMENT_H + 8;

        drawBanner(sw, cw, cx);
        drawSegmented(sw, cw, cx, catAnimX);
        drawModuleCards(cx, topArea, cw, sh - topArea - 10);

        GL11.glPopMatrix();
    }

    // ═══════════════════════════════════════════════════════
    //  顶部品牌横幅 —— 紫色 Deception
    // ═══════════════════════════════════════════════════════
    private void drawBanner(int sw, int cw, int cx) {
        // 紫色渐变横幅 (用纯色 + 圆角模拟)
        int by = 6;
        RoundedUtils.drawRound(cx, by, cw, BANNER_H, CARD_RADIUS, new Color(0xFF1A0A2E));
        RoundedUtils.drawRound(cx + 1, by + 1, cw - 2, BANNER_H - 2, CARD_RADIUS - 1,
                new Color(0xFF2D1050));

        // 紫色光晕 (右侧装饰)
        RoundedUtils.drawRound(cx + cw - 80, by - 10, 80, BANNER_H + 20, 40,
                new Color(PURPLE_DIM, true));

        // "Deception" 大字
        String brand = "Deception";
        float bScale = 1.8f;
        int bW = (int)(Deception.fontManager.s24.getStringWidth(brand) * bScale);
        int bX = cx + cw / 2 - bW / 2;
        int bY = by + (BANNER_H - (int)(Deception.fontManager.s24.getHeight() * bScale)) / 2;

        // 发光文字效果：绘制两层
        GL11.glPushMatrix();
        GL11.glTranslatef(bX, bY, 0);
        GL11.glScalef(bScale, bScale, 1);
        Deception.fontManager.s24.drawString(brand, 0, 0, 0x60C084FC);
        Deception.fontManager.s24.drawString(brand, 0, 0, 0xFFC084FC);
        GL11.glPopMatrix();

        // 版本号
        String ver = "v" + (Deception.version != null ? Deception.version : "dev");
        Deception.fontManager.s12.drawString(ver,
                cx + cw - Deception.fontManager.s12.getStringWidth(ver) - 10,
                by + BANNER_H - Deception.fontManager.s12.getHeight() - 6,
                TEXT_DIM);
    }

    // ═══════════════════════════════════════════════════════
    //  分类分段控件（带动画）
    // ═══════════════════════════════════════════════════════
    private void drawSegmented(int sw, int cw, int cx, float catAnimX) {
        int sy = BANNER_H + 12;
        int segW = Math.min(cw, 400);
        int segX = cx + cw / 2 - segW / 2;

        Category[] cats = {
                Category.COMBAT, Category.MOVEMENT, Category.RENDER,
                Category.PLAYER, Category.MISC
        };
        int count = cats.length;
        int itemW = segW / count;

        // 背景 pill
        RoundedUtils.drawRound(segX, sy, segW, SEGMENT_H, SEGMENT_H / 2f,
                new Color(TAB_BG));

        // 选中高亮 pill（动画位置）
        RoundedUtils.drawRound(
                catAnimX, sy + 2,
                itemW - 4, SEGMENT_H - 4,
                (SEGMENT_H - 4) / 2f, new Color(PURPLE));

        // 标签
        for (int i = 0; i < count; i++) {
            String label = cats[i].name();
            boolean sel = cats[i] == selectedCategory;
            int lw = Deception.fontManager.s12.getStringWidth(label);
            int lx = segX + i * itemW + (itemW - lw) / 2;
            int ly = sy + (SEGMENT_H - Deception.fontManager.s12.getHeight()) / 2 + 1;
            Deception.fontManager.s12.drawString(label, lx, ly,
                    sel ? 0xFFFFFFFF : TEXT_SECONDARY);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  模块卡片列表（每模块独立圆角卡片）
    // ═══════════════════════════════════════════════════════
    private void drawModuleCards(int cx, int topY, int cw, int maxH) {
        List<Module> modules = getModulesForCategory(selectedCategory);
        if (modules.isEmpty()) return;

        // ── 预计算每张卡的高度（使用动画高度） ──
        int[] cardHs = new int[modules.size()];
        int totalContent = 0;
        for (int i = 0; i < modules.size(); i++) {
            int h = getAnimatedCardHeight(modules.get(i));
            cardHs[i] = h;
            totalContent += h + CARD_GAP;
        }
        totalContent -= CARD_GAP; // 最后一项无需 gap

        // ── 滚动边界 ──
        int maxScroll = Math.max(0, totalContent - maxH + 10);
        if (scrollOffset > 0) scrollOffset = 0;
        if (scrollOffset < -maxScroll) scrollOffset = -maxScroll;

        // ── 裁剪 ──
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr = new ScaledResolution(mc);
        int sf = sr.getScaleFactor();
        GL11.glScissor(
                (int)(cx * sf),
                (int)((sr.getScaledHeight() - (topY + maxH)) * sf),
                (int)(cw * sf),
                (int)(maxH * sf));

        int drawY = topY + (int)smoothScroll;

        // ── 逐张绘制卡片 ──
        for (int i = 0; i < modules.size(); i++) {
            Module mod  = modules.get(i);
            boolean exp = expandedModules.contains(mod.getName());
            int cardH   = cardHs[i];

            if (drawY + cardH >= topY && drawY <= topY + maxH) {
                // 卡片淡入动画（按索引延迟）
                String fadeKey = "card_" + mod.getName();
                RiseAnim fadeAnim = cardFadeAnims.computeIfAbsent(fadeKey, k -> {
                    RiseAnim a = new RiseAnim(Easing.EASE_OUT_QUAD, 300);
                    a.setValue(0);
                    a.run(1);
                    return a;
                });
                fadeAnim.run(1);
                double fade = fadeAnim.getValue();

                // 每张卡片依次延迟出现
                double delayFactor = Math.min(1, Math.max(0, fade * 1.5 - i * 0.08));
                float cardAlpha = (float) Math.min(1, delayFactor);
                float cardSlide = 1 - (float) delayFactor;

                GL11.glPushMatrix();
                // 从下方滑入效果
                GL11.glTranslatef(0, cardSlide * 12, 0);

                if (cardAlpha > 0.01f) {
                    drawModuleCard(mod, cx, drawY, cw, cardH, exp, cardAlpha);
                }
                GL11.glPopMatrix();
            }
            drawY += cardH + CARD_GAP;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // ── 滚动条 ──
        if (totalContent > maxH) {
            int sbX = cx + cw - 3;
            drawScrollbar(sbX, topY, 3, maxH, totalContent, (int)smoothScroll);
        }
    }

    /** 获取卡片动画高度（展开/折叠平滑过渡） */
    private int getAnimatedCardHeight(Module mod) {
        String key = mod.getName();
        boolean isExpanded = expandedModules.contains(key);

        int baseH = CELL_H;
        int expandedH = baseH;
        if (isExpanded) {
            List<Property<?>> props = Deception.propertyManager.properties.get(mod.getClass());
            if (props != null) expandedH += 4 + props.size() * SETTING_H;
        }

        ContinualAnimation anim = expandAnims.get(key);
        if (anim == null) {
            anim = new ContinualAnimation();
            expandAnims.put(key, anim);
        }

        int target = isExpanded ? expandedH : baseH;
        anim.animate(target, 300);
        return Math.max(baseH, (int) anim.getOutput());
    }

    // ═══════════════════════════════════════════════════════
    //  单张模块卡片
    // ═══════════════════════════════════════════════════════
    private void drawModuleCard(Module mod, int x, int y, int w, int h, boolean expanded, float alpha) {
        // ── 卡片背景（带淡入透明度） ──
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + CELL_H;
        int cardColor = hover ? 0xE828282A : CARD_BG;
        Color cardColorObj = new Color(cardColor, true);
        // 应用透明度
        cardColorObj = new Color(
                cardColorObj.getRed(), cardColorObj.getGreen(),
                cardColorObj.getBlue(), (int) (cardColorObj.getAlpha() * alpha)
        );
        RoundedUtils.drawRound(x, y, w, h, CARD_RADIUS, cardColorObj);

        // 发光上边框
        RoundedUtils.drawRound(x + 4, y, w - 8, 1, 0.5f, new Color(0x15FFFFFF, true));

        // ── 模块名称 ──
        String name = mod.getName();
        int nameColor = mod.isEnabled() ? TEXT_PRIMARY : TEXT_DIM;
        Deception.fontManager.s14.drawString(name, x + 14,
                y + (CELL_H - Deception.fontManager.s14.getHeight()) / 2f + 1, nameColor);

        // ── 后缀 ──
        String[] suffix = mod.getSuffix();
        if (suffix != null && suffix.length > 0 && suffix[0] != null && !suffix[0].isEmpty()) {
            Deception.fontManager.s12.drawString(suffix[0],
                    x + 16 + Deception.fontManager.s14.getStringWidth(name),
                    y + (CELL_H - Deception.fontManager.s12.getHeight()) / 2f + 1, TEXT_DIM);
        }

        // ── iOS Switch ──
        drawSwitch(x + w - SWITCH_W - 12, y + (CELL_H - SWITCH_H) / 2f,
                SWITCH_W, SWITCH_H, mod.isEnabled(), mod.getName());

        // ── 展开箭头 (仅折叠时显示) ──
        if (!expanded) {
            String chevron = "›";
            Deception.fontManager.s16.drawString(chevron,
                    x + w - SWITCH_W - 24,
                    y + (CELL_H - Deception.fontManager.s16.getHeight()) / 2f + 1, TEXT_DIM);
        }

        // ── 展开设置区 ──
        if (expanded) {
            List<Property<?>> props =
                    Deception.propertyManager.properties.get(mod.getClass());
            if (props != null && !props.isEmpty()) {
                // 分隔线
                RoundedUtils.drawRound(x + 14, y + CELL_H, w - 28, 1, 0.5f,
                        new Color(SEPARATOR, true));

                int sy = y + CELL_H + 6;
                int sx = x + 16;
                int sw = w - 32;
                for (Property<?> prop : props) {
                    drawSettingRow(prop, sx, sy, sw);
                    sy += SETTING_H;
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  iOS UISwitch
    // ═══════════════════════════════════════════════════════
    private void drawSwitch(float x, float y, float w, float h, boolean on, String key) {
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

        // 轨道
        int track = lerpColor(TOGGLE_OFF, ACCENT_GREEN, t);
        RoundedUtils.drawRound(x, y, w, h, r, new Color(track, true));

        // 旋钮
        float ks = h - 4;
        float kx = x + 2 + (w - ks - 4) * t;
        float ky = y + 2;

        RoundedUtils.drawRound(kx + 1, ky + 1, ks, ks, ks / 2f,
                new Color(0x40000000, true));
        RoundedUtils.drawRound(kx, ky, ks, ks, ks / 2f,
                new Color(0xFFFFFFFF));
    }

    // ═══════════════════════════════════════════════════════
    //  设置行
    // ═══════════════════════════════════════════════════════
    private void drawSettingRow(Property<?> prop, float x, float y, float w) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + SETTING_H;
        if (hover) {
            RoundedUtils.drawRound(x, y, w, SETTING_H, 6, new Color(0x15FFFFFF, true));
        }

        if (prop instanceof BooleanProperty) {
            // 名称 + 开关
            Deception.fontManager.s14.drawString(prop.getName(), x + 4,
                    y + (SETTING_H - Deception.fontManager.s14.getHeight()) / 2f + 1, TEXT_PRIMARY);
            drawSwitch(x + w - SWITCH_W, y + (SETTING_H - SWITCH_H) / 2f,
                    SWITCH_W, SWITCH_H,
                    ((BooleanProperty) prop).getValue(),
                    "sp_" + prop.getName());

        } else if (prop instanceof ModeProperty) {
            ModeProperty mp = (ModeProperty) prop;
            Deception.fontManager.s14.drawString(prop.getName(), x + 4,
                    y + (SETTING_H - Deception.fontManager.s14.getHeight()) / 2f + 1, TEXT_PRIMARY);

            // 分段选择器样式
            String[] modes = mp.getModes();
            int sel = mp.getValue();
            float pw = Math.min(w * 0.5f, 180);
            float px = x + w - pw;
            float py = y + (SETTING_H - 24) / 2f;

            RoundedUtils.drawRound(px, py, pw, 24, 12, new Color(TAB_BG));

            int displayCount = Math.min(modes.length, 4);
            float segW2 = pw / displayCount;
            if (sel >= 0 && sel < modes.length) {
                int selGroup = Math.min(sel, displayCount - 1);
                RoundedUtils.drawRound(px + selGroup * segW2 + 2, py + 2,
                        segW2 - 4, 20, 10, new Color(PURPLE));
            }

            for (int i = 0; i < displayCount; i++) {
                String label = modes[i].length() > 5
                        ? modes[i].substring(0, 4) + ".." : modes[i];
                int lw = Deception.fontManager.s12.getStringWidth(label);
                float lx = px + i * segW2 + (segW2 - lw) / 2f;
                float ly = py + (24 - Deception.fontManager.s12.getHeight()) / 2f + 1;
                Deception.fontManager.s12.drawString(label, lx, ly,
                        i == sel ? 0xFFFFFFFF : TEXT_SECONDARY);
            }

        } else if (prop instanceof FloatProperty
                || prop instanceof IntProperty
                || prop instanceof PercentProperty) {

            double min, max, val;
            String display;
            if (prop instanceof FloatProperty) {
                FloatProperty fp = (FloatProperty) prop;
                min = fp.getMinimum(); max = fp.getMaximum(); val = fp.getValue();
                display = String.format("%.1f", val);
            } else if (prop instanceof IntProperty) {
                IntProperty ip = (IntProperty) prop;
                min = ip.getMinimum(); max = ip.getMaximum(); val = ip.getValue();
                display = String.valueOf((int) val);
            } else {
                PercentProperty pp = (PercentProperty) prop;
                min = pp.getMinimum(); max = pp.getMaximum(); val = pp.getValue();
                display = (int) val + "%";
            }

            // 名称
            Deception.fontManager.s14.drawString(prop.getName(), x + 4, y + 2, TEXT_PRIMARY);
            Deception.fontManager.s12.drawString(display,
                    x + 6 + Deception.fontManager.s14.getStringWidth(prop.getName()),
                    y + 3, TEXT_SECONDARY);

            // iOS 滑块
            float slY = y + SETTING_H - 14;
            float slW = w - 8;
            double range = Math.max(max - min, 0.001);
            float pct = (float) Math.min(1, Math.max(0, (val - min) / range));

            // 轨道背景
            RoundedUtils.drawRound(x + 4, slY, slW, 4, 2, new Color(TOGGLE_OFF));
            // 填充
            RoundedUtils.drawRound(x + 4, slY, slW * pct, 4, 2, new Color(PURPLE));
            // 旋钮 (白色 + 紫色圈)
            float kr = 7;
            float kx2 = x + 4 + slW * pct;
            float ky2 = slY + 2;
            RoundedUtils.drawRound(kx2 - kr, ky2 - kr, kr * 2, kr * 2, kr,
                    new Color(0xFFFFFFFF));
            RoundedUtils.drawRound(kx2 - kr + 2, ky2 - kr + 2, kr * 2 - 4, kr * 2 - 4,
                    kr - 2, new Color(PURPLE));

        } else if (prop instanceof ColorProperty) {
            ColorProperty cp = (ColorProperty) prop;
            Deception.fontManager.s14.drawString(prop.getName(), x + 4,
                    y + (SETTING_H - Deception.fontManager.s14.getHeight()) / 2f + 1, TEXT_PRIMARY);

            int colorVal = cp.getValue();
            int dotSize = 20;
            int dx = (int)(x + w - dotSize - 4);
            int dy = (int)(y + (SETTING_H - dotSize) / 2f);
            RoundedUtils.drawRound(dx, dy, dotSize, dotSize, dotSize / 2f,
                    new Color(colorVal, true));
            RoundedUtils.drawRoundOutline(dx, dy, dotSize, dotSize,
                    dotSize / 2f, 1.5f, new Color(0x00000000, true),
                    new Color(0x40FFFFFF, true));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  滚动条
    // ═══════════════════════════════════════════════════════
    private void drawScrollbar(int x, int y, int w, int h, int contentH, int scroll) {
        float thumbH = Math.max(18, (float) h / contentH * h);
        float thumbY = y + (float) -scroll / contentH * h;
        RoundedUtils.drawRound(x, y, w, h, w / 2f, new Color(SCROLLBAR_BG, true));
        RoundedUtils.drawRound(x, thumbY, w, thumbH, w / 2f, new Color(SCROLLBAR_THUMB, true));
    }

    // ═══════════════════════════════════════════════════════
    //  工具
    // ═══════════════════════════════════════════════════════
    private List<Module> getModulesForCategory(Category cat) {
        return Deception.moduleManager.modules.values().stream()
                .filter(m -> m.getCategory() == cat && !m.isHidden())
                .sorted(Comparator.comparing(Module::getName))
                .collect(Collectors.toList());
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
    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();

        int edgeGap = Math.max(sw / 10, 16);
        int cw = sw - edgeGap * 2;
        int cx = edgeGap;
        int topArea = BANNER_H + 8 + SEGMENT_H + 8;
        int maxH = sh - topArea - 10;

        // ── 分段控件点击 ──
        int sy = BANNER_H + 12;
        int segW = Math.min(cw, 400);
        int segX = cx + cw / 2 - segW / 2;
        Category[] cats = {Category.COMBAT, Category.MOVEMENT,
                Category.RENDER, Category.PLAYER, Category.MISC};
        int count = cats.length;
        int itemW = segW / count;

        if (my >= sy && my <= sy + SEGMENT_H
                && mx >= segX && mx <= segX + segW) {
            int idx = (mx - segX) / itemW;
            if (idx >= 0 && idx < count) {
                if (cats[idx] != selectedCategory) {
                    // 切换分类时触发动画
                    categoryTargetX = segX + idx * itemW + 2;
                    categoryAnim.run(categoryTargetX);
                }
                selectedCategory = cats[idx];
                scrollOffset = 0;
            }
            return;
        }

        // ── 模块卡片点击 ──
        List<Module> modules = getModulesForCategory(selectedCategory);
        int drawY = topArea + (int) smoothScroll;

        for (int i = 0; i < modules.size(); i++) {
            Module mod = modules.get(i);
            boolean exp = expandedModules.contains(mod.getName());

            // 预计算卡片高度
            int cardH = getAnimatedCardHeight(mod);

            if (mx >= cx && mx <= cx + cw && my >= drawY && my <= drawY + cardH) {
                // Switch 区域
                int swX = cx + cw - SWITCH_W - 12;
                int swY = drawY + (CELL_H - SWITCH_H) / 2;
                if (mx >= swX && mx <= swX + SWITCH_W
                        && my >= swY && my <= swY + SWITCH_H) {
                    mod.toggle();
                    return;
                }

                // 模块行点击
                if (my >= drawY && my <= drawY + CELL_H) {
                    if (btn == 1) {
                        if (exp) expandedModules.remove(mod.getName());
                        else expandedModules.add(mod.getName());
                    } else {
                        mod.toggle();
                    }
                    return;
                }

                // 设置区点击
                if (exp) {
                    List<Property<?>> props =
                            Deception.propertyManager.properties.get(mod.getClass());
                    if (props != null) {
                        int sy2 = drawY + CELL_H + 6;
                        int sx2 = cx + 16;
                        int sw2 = cw - 32;
                        for (Property<?> prop : props) {
                            if (my >= sy2 && my <= sy2 + SETTING_H) {
                                handleSettingClick(prop, mx, my, btn,
                                        sx2, sy2, sw2);
                                return;
                            }
                            sy2 += SETTING_H;
                        }
                    }
                }
            }
            drawY += cardH + CARD_GAP;
        }
    }

    private void handleSettingClick(Property<?> prop, int mx, int my, int btn,
                                     float sx, float sy, float sw) {
        if (prop instanceof BooleanProperty) {
            ((BooleanProperty) prop).setValue(!((BooleanProperty) prop).getValue());

        } else if (prop instanceof ModeProperty) {
            ModeProperty mp = (ModeProperty) prop;
            float pw = Math.min(sw * 0.5f, 180);
            float px = sx + sw - pw;
            float py = sy + (SETTING_H - 24) / 2f;

            if (mx >= px && mx <= px + pw && my >= py && my <= py + 24) {
                int len = mp.getModes().length;
                int dc = Math.min(len, 4);
                float segW2 = pw / dc;
                int idx = (int) ((mx - px) / segW2);
                if (idx >= 0 && idx < len) mp.setValue(idx);
            } else {
                if (btn == 0) mp.nextMode();
                else mp.previousMode();
            }

        } else if (prop instanceof FloatProperty) {
            FloatProperty fp = (FloatProperty) prop;
            if (btn == 0 && my >= sy + SETTING_H - 18 && my <= sy + SETTING_H) {
                draggingSlider = true;
                dragProp = prop;
                float slW = sw - 8;
                float pct = Math.max(0, Math.min(1, (mx - sx - 4) / slW));
                fp.setValue(fp.getMinimum() + (fp.getMaximum() - fp.getMinimum()) * pct);
            }

        } else if (prop instanceof IntProperty) {
            IntProperty ip = (IntProperty) prop;
            if (btn == 0 && my >= sy + SETTING_H - 18 && my <= sy + SETTING_H) {
                draggingSlider = true;
                dragProp = prop;
                float slW = sw - 8;
                float pct = Math.max(0, Math.min(1, (mx - sx - 4) / slW));
                double range = ip.getMaximum() - ip.getMinimum();
                ip.setValue((int) (ip.getMinimum() + range * pct));
            }

        } else if (prop instanceof PercentProperty) {
            PercentProperty pp = (PercentProperty) prop;
            if (btn == 0 && my >= sy + SETTING_H - 18 && my <= sy + SETTING_H) {
                draggingSlider = true;
                dragProp = prop;
                float slW = sw - 8;
                float pct = Math.max(0, Math.min(1, (mx - sx - 4) / slW));
                double range = pp.getMaximum() - pp.getMinimum();
                pp.setValue((int) (pp.getMinimum() + range * pct));
            }
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        if (!draggingSlider || dragProp == null) return;
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int eg = Math.max(sw / 10, 16);
        int cw = sw - eg * 2;
        float slW = cw - 48;
        float pct = Math.max(0, Math.min(1, (mx - eg - 20) / slW));

        if (dragProp instanceof FloatProperty) {
            FloatProperty fp = (FloatProperty) dragProp;
            fp.setValue(fp.getMinimum() + (fp.getMaximum() - fp.getMinimum()) * pct);
        } else if (dragProp instanceof IntProperty) {
            IntProperty ip = (IntProperty) dragProp;
            double range = ip.getMaximum() - ip.getMinimum();
            ip.setValue((int) (ip.getMinimum() + range * pct));
        } else if (dragProp instanceof PercentProperty) {
            PercentProperty pp = (PercentProperty) dragProp;
            double range = pp.getMaximum() - pp.getMinimum();
            pp.setValue((int) (pp.getMinimum() + range * pct));
        }
    }

    @Override
    protected void mouseReleased(int mx, int my, int btn) {
        draggingSlider = false;
        dragProp = null;
    }

    // ═══════════════════════════════════════════════════════
    //  键盘 & 滚轮
    // ═══════════════════════════════════════════════════════
    @Override
    protected void keyTyped(char c, int k) throws IOException {
        if (k == Keyboard.KEY_ESCAPE || k == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) scrollOffset += d > 0 ? 36 : -36;
    }

    @Override
    public boolean doesGuiPauseGame() { return true; }

    @Override
    public void onGuiClosed() { /* noop */ }
}