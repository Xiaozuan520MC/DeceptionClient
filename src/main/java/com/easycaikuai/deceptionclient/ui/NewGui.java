package com.easycaikuai.deceptionclient.ui;

import net.minecraft.client.gui.Gui;
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

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class NewGui extends GuiScreen {

    private static final int PW = 440, PH = 500;
    private int pX, pY;

    private static final Category[] CATS = {
        Category.COMBAT, Category.MOVEMENT, Category.RENDER,
        Category.PLAYER, Category.MISC, Category.SETTINGS
    };
    private int sel = 0;
    private final Set<String> exp = new HashSet<>();
    private float sc = 0;
    private int mx, my;
    private Property<?> drag = null;
    private boolean dragPanel = false;
    private int dx, dy;
    private String rebindMod = null;

    private String cfgName = "default";
    private boolean cfgEdit = false;
    private List<String> cfgList = new ArrayList<>();
    private static final File CFG = new File("./config/Deception/");

    // ─── 动画系统 ───────────────────────────────────────────

    /** 开启动画：面板从中心缩放淡入 */
    private final RiseAnim openAnim = new RiseAnim(Easing.EASE_OUT_QUAD, 350);

    /** 分类切换动画：高亮平滑移动到目标位置 */
    private final RiseAnim catAnim = new RiseAnim(Easing.EASE_OUT_CIRC, 250);
    private float catAnimTarget = 0;

    /** 模块展开/折叠动画（高度） */
    private final Map<String, ContinualAnimation> expandAnims = new HashMap<>();

    /** 开关动画 */
    private static class ToggleAnim {
        float target, current;
    }
    private final Map<String, ToggleAnim> toggleAnims = new HashMap<>();

    /** 悬停动画（分类按钮） */
    private final Map<String, RiseAnim> hoverAnims = new HashMap<>();

    /** 平滑滚动 */
    private float smoothScroll = 0f;

    private void refreshCfgs() {
        cfgList.clear();
        if (CFG.exists()) { File[] f = CFG.listFiles((d,n)->n.endsWith(".json")); if (f != null) for (File x : f) cfgList.add(x.getName().replace(".json","")); }
        cfgList.sort(String::compareToIgnoreCase);
    }

    @Override public void initGui() {
        ScaledResolution sr = new ScaledResolution(mc);
        pX = (sr.getScaledWidth() - PW) / 2;
        pY = (sr.getScaledHeight() - PH) / 2;
        refreshCfgs();

        // 重置开启动画
        openAnim.setValue(0);
        openAnim.run(1);

        // 重置分类动画
        int segX2 = pX + 16, segW2 = PW - 32, itemW2 = segW2 / CATS.length;
        catAnimTarget = segX2 + sel * itemW2 + 2;
        catAnim.setValue(catAnimTarget);
        catAnim.run(catAnimTarget);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        this.mx = mx; this.my = my;

        // ── 平滑滚动 ──
        smoothScroll += (sc - smoothScroll) * 0.25f;
        if (Math.abs(sc - smoothScroll) < 0.5f) smoothScroll = sc;

        drawDefaultBackground();

        // ── 开启动画 ──
        openAnim.run(1);
        double openProgress = openAnim.getValue();
        float openScale = 0.93f + 0.07f * (float) openProgress;
        int openAlpha = (int) (255 * openProgress);

        // 更新分类动画
        catAnim.run(catAnimTarget);
        float catAnimX = (float) catAnim.getValue();

        // 计算分类高亮动画位置
        int segX = pX + 16, segW = PW - 32, itemW = segW / CATS.length;

        GL11.glPushMatrix();
        GL11.glTranslatef(pX + PW / 2f, pY + PH / 2f, 0);
        GL11.glScalef(openScale, openScale, 1);
        GL11.glTranslatef(-pX - PW / 2f, -pY - PH / 2f, 0);

        int x = pX, y = pY, w = PW, h = PH;

        // 背景
        int bgColor = (openAlpha << 24) | (0x080C16 & 0xFFFFFF);
        Gui.drawRect(x, y, x + w, y + h, bgColor);

        // 顶栏
        Deception.fontManager.s16.drawString("Deception", x + 16, y + 16, 0xFFAF52DE);
        if (Deception.version != null)
            Deception.fontManager.s12.drawString(Deception.version, x + 18 + Deception.fontManager.s16.getStringWidth("Deception"), y + 19, 0xFF5A5F6D);

        // 分类按钮（带动画）
        int segY = y + 48;
        Gui.drawRect(segX, segY, segX + segW, segY + 30, 0xFF121626);

        // 高亮条（动画位置）
        int highlightW = itemW - 4;
        Gui.drawRect((int) catAnimX, segY + 2, (int) (catAnimX + highlightW), segY + 28, 0xFFAF52DE);
        Gui.drawRect((int) catAnimX + 2, segY + 28, (int) (catAnimX + highlightW - 2), segY + 29, 0xFFAF52DE);

        for (int i = 0; i < CATS.length; i++) {
            String name = CATS[i].name(); name = name.charAt(0) + name.substring(1).toLowerCase();
            boolean s2 = i == sel;
            boolean h2 = mx >= segX + i * itemW && mx <= segX + (i + 1) * itemW && my >= segY && my <= segY + 30;

            // 未选中的悬停效果
            if (!s2 && h2) Gui.drawRect(segX + i * itemW + 2, segY + 2, segX + (i + 1) * itemW - 2, segY + 28, 0x20FFFFFF);

            // 底部指示条（选中时用动画高亮代替，非选中时显示默认）
            if (!s2) Gui.drawRect(segX + i * itemW + 2, segY + 28, segX + (i + 1) * itemW - 2, segY + 29, 0x202A3050);

            Deception.fontManager.s12.drawString(name, segX + i * itemW + (itemW - Deception.fontManager.s12.getStringWidth(name)) / 2f, segY + (30 - Deception.fontManager.s12.getHeight()) / 2f + 1, s2 ? 0xFFFFFFFF : 0xFF636366);
        }

        int cx = x + 16, cw = w - 32, cy2 = segY + 38, ch = h - (cy2 - y) - 16;
        if (CATS[sel] == Category.SETTINGS) { drawSettings(cx, cy2, cw, ch); GL11.glPopMatrix(); return; }

        List<Module> mods = new ArrayList<>();
        for (Module m : Deception.moduleManager.modules.values())
            if (m.getCategory() == CATS[sel]) mods.add(m);
        mods.sort(Comparator.comparing(Module::getName));
        if (mods.isEmpty()) { Deception.fontManager.s14.drawString("No modules", cx, cy2 + 20, 0xFF5A5F6D); GL11.glPopMatrix(); return; }

        // 使用动画高度
        int[] mhs = new int[mods.size()]; int total = 0;
        for (int i = 0; i < mods.size(); i++) {
            mhs[i] = getAnimatedModuleHeight(mods.get(i));
            total += mhs[i] + 4;
        } total += 6;
        int maxS = Math.max(0, total - ch + 8);
        if (smoothScroll > 0) sc = 0;
        if (smoothScroll < -maxS) sc = -maxS;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int sf = new ScaledResolution(mc).getScaleFactor();
        GL11.glScissor((int)(cx * sf), (int)((mc.displayHeight - (cy2 + ch) * sf)), (int)(cw * sf), Math.max(0, (int)(ch * sf)));

        int dy2 = cy2 + (int)smoothScroll;
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            boolean on = m.isEnabled(), expd = exp.contains(m.getName());
            boolean hov = mx >= cx && mx <= cx + cw && my >= dy2 && my <= dy2 + 30;

            // 悬停颜色过渡
            String hKey = "mod_" + m.getName();
            RiseAnim hovAnim = hoverAnims.computeIfAbsent(hKey, k -> {
                RiseAnim a = new RiseAnim(Easing.EASE_OUT_QUAD, 120);
                a.setValue(0); a.run(0);
                return a;
            });
            hovAnim.run(hov ? 1 : 0);
            double hf = hovAnim.getValue();

            int bgOff = on ? 0xE01A2640 : 0xE0101628;
            int bgHov = on ? 0xE0223060 : 0xE0142038;
            int bg = lerpColor(bgOff, bgHov, (float) hf);
            Gui.drawRect(cx, dy2, cx + cw, dy2 + mhs[i], bg);

            // 左侧竖条（带动画）
            int barColor = lerpColor(0xFF3A3F4D, 0xFFAF52DE, on ? 1f : 0.3f);
            Gui.drawRect(cx + 2, dy2 + 6, cx + 4, dy2 + mhs[i] - 6, barColor);

            String pre = expd ? "▼ " : "▶ ";
            Deception.fontManager.s14.drawString(pre + m.getName(), cx + 12, dy2 + (30 - Deception.fontManager.s14.getHeight()) / 2f + 1, on ? 0xFFF0F2F5 : 0xFF8A8F9D);
            String[] suf = m.getSuffix();
            if (suf != null && suf.length > 0 && suf[0] != null && !suf[0].isEmpty())
                Deception.fontManager.s12.drawString(suf[0], cx + 14 + Deception.fontManager.s14.getStringWidth(pre + m.getName()), dy2 + (30 - Deception.fontManager.s12.getHeight()) / 2f + 1, 0xFF5A5F6D);

            if (expd) {
                int sy2 = dy2 + 34;
                boolean rb = rebindMod != null && rebindMod.equals(m.getName());
                Gui.drawRect(cx + 6, sy2, (int)(cx + cw / 2f - 2), sy2 + 18, rb ? 0xFFAF52DE : 0xFF0A0E1A);
                String kn = rb ? "Press Key..." : (m.getKey() == 0 ? "None" : Keyboard.getKeyName(m.getKey()));
                Deception.fontManager.s12.drawString("Bind: " + kn, cx + 12, sy2 + (18 - Deception.fontManager.s12.getHeight()) / 2f + 1, rb ? 0xFFFFFFFF : 0xFF8A8F9D);
                sy2 += 22;
                List<Property<?>> props = Deception.propertyManager.properties.get(m.getClass());
                if (props != null) for (Property<?> p : props) { drawProp(p, cx + 8, sy2, cw - 16); sy2 += 22; }
            }
            dy2 += mhs[i] + 4;
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();

        if (total > ch) {
            float th = Math.max(14, (float)ch / total * ch);
            float ty = cy2 + (float)-smoothScroll / total * ch;
            Gui.drawRect(cx + cw - 2, cy2, cx + cw, (int)(cy2 + ch), 0xFF141A28);
            Gui.drawRect(cx + cw - 2, (int)ty, cx + cw, (int)(ty + th), 0xFFAF52DE);
        }
    }

    /** 获取模块动画高度（展开/折叠平滑过渡） */
    int getAnimatedModuleHeight(Module m) {
        String key = m.getName();
        boolean isExpanded = exp.contains(key);

        int baseH = 32;
        int expandedH = baseH;
        if (isExpanded) {
            expandedH += 4 + 20;
            List<Property<?>> pp = Deception.propertyManager.properties.get(m.getClass());
            if (pp != null) expandedH += pp.size() * 22;
        }

        ContinualAnimation anim = expandAnims.get(key);
        if (anim == null) {
            anim = new ContinualAnimation();
            expandAnims.put(key, anim);
        }

        int target = isExpanded ? expandedH : baseH;
        anim.animate(target, 200);
        return Math.max(baseH, (int) anim.getOutput());
    }

    void drawProp(Property<?> p, int x, int y, int w) {
        if (p instanceof BooleanProperty) {
            boolean v = ((BooleanProperty)p).getValue();
            Deception.fontManager.s12.drawString(p.getName(), x + 4, y + (20 - Deception.fontManager.s12.getHeight()) / 2f, 0xFFF0F2F5);
            // 带动画的开关
            drawAnimatedSwitch(x + w - 28, y + 4, 24, 12, v, "prop_" + p.getName());
        } else if (p instanceof ModeProperty) {
            ModeProperty mp = (ModeProperty)p;
            Deception.fontManager.s12.drawString(p.getName(), x + 4, y + (20 - Deception.fontManager.s12.getHeight()) / 2f, 0xFFF0F2F5);
            String mo = mp.getModeString();
            float mw = Deception.fontManager.s12.getStringWidth(mo);
            Gui.drawRect((int)(x + w - mw - 14), y + 2, (int)(x + w - 4), y + 18, 0xFFAF52DE);
            Deception.fontManager.s12.drawString(mo, x + w - mw - 9, y + (18 - Deception.fontManager.s12.getHeight()) / 2f + 1, 0xFFFFFFFF);
        } else if (p instanceof FloatProperty || p instanceof IntProperty || p instanceof PercentProperty) {
            double min,max,val; String d;
            if (p instanceof FloatProperty) { FloatProperty fp = (FloatProperty)p; min=fp.getMinimum(); max=fp.getMaximum(); val=fp.getValue(); d=String.format("%.1f",val); }
            else if (p instanceof IntProperty) { IntProperty ip = (IntProperty)p; min=ip.getMinimum(); max=ip.getMaximum(); val=ip.getValue(); d=String.valueOf((int)val); }
            else { PercentProperty pp = (PercentProperty)p; min=pp.getMinimum(); max=pp.getMaximum(); val=pp.getValue(); d=(int)val+"%"; }
            Deception.fontManager.s12.drawString(p.getName(), x + 4, y + 2, 0xFFF0F2F5);
            Deception.fontManager.s12.drawString(d, x + 6 + Deception.fontManager.s12.getStringWidth(p.getName()), y + 2, 0xFF8A8F9D);
            float slW = w - 50, slX = x + w - slW - 6;
            float pct = (float)((val - min) / Math.max(max - min, 0.001));
            Gui.drawRect((int)slX, (int)(y + 12), (int)(slX + slW), (int)(y + 14), 0xFF1A2030);
            Gui.drawRect((int)slX, (int)(y + 12), (int)(slX + slW * pct), (int)(y + 14), 0xFFAF52DE);
            Gui.drawRect((int)(slX + slW * pct - 3), (int)(y + 10), (int)(slX + slW * pct + 3), (int)(y + 16), 0xFFFFFFFF);
        }
    }

    /** 带动画平滑过渡的开关（简约风格） */
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

        // 轨道颜色过渡
        int track = lerpColor(0xFF39393D, 0xFF34C759, t);
        Gui.drawRect((int) x, (int) y, (int) (x + w), (int) (y + h), track);

        // 旋钮平滑滑动
        float ks = h - 4;
        float kx = x + 2 + (w - ks - 4) * t;
        Gui.drawRect((int) kx, (int) (y + 2), (int) (kx + ks), (int) (y + 2 + ks), 0xFFFFFFFF);
    }

    void drawSettings(int x, int y, int w, int h) {
        Deception.fontManager.s14.drawString("Settings", x, y, 0xFFAF52DE);
        y += 24;
        int iw = w - 110;
        boolean hi = mx >= x + 4 && mx <= x + 4 + iw && my >= y && my <= y + 24;
        Gui.drawRect(x + 4, y, x + 4 + iw, y + 24, hi || cfgEdit ? 0xFF242C46 : 0xFF121626);
        String nd = cfgEdit ? cfgName + (System.currentTimeMillis() % 800 > 400 ? "|" : "") : (cfgName.isEmpty() ? "Name..." : cfgName);
        Deception.fontManager.s12.drawString(nd, x + 10, y + (24 - Deception.fontManager.s12.getHeight()) / 2f + 1, cfgEdit || !cfgName.isEmpty() ? 0xFFF0F2F5 : 0xFF5A5F6D);
        boolean hs = mx >= x + 8 + iw && mx <= x + 8 + iw + 96 && my >= y && my <= y + 24;
        Gui.drawRect(x + 8 + iw, y + 2, x + 8 + iw + 96, y + 22, hs ? 0xFFAF52DE : 0xFF282E48);
        Deception.fontManager.s12.drawString("Save", x + 8 + iw + (96 - Deception.fontManager.s12.getStringWidth("Save"))/2f, y + (22 - Deception.fontManager.s12.getHeight())/2f + 1, 0xFFFFFFFF);
        y += 32;
        Deception.fontManager.s12.drawString("Saved", x + 4, y, 0xFF5A5F6D);
        y += 14;
        if (cfgList.isEmpty()) { Deception.fontManager.s12.drawString("None", x + 8, y + 6, 0xFF5A5F6D); return; }
        for (String c : cfgList) {
            boolean h2 = mx >= x + 2 && mx <= x + w - 26 && my >= y && my <= y + 22;
            boolean a = c.equals(cfgName);
            Gui.drawRect(x + 2, y, x + w - 26, y + 22, a ? 0x30AF52DE : 0x10141828);
            Deception.fontManager.s12.drawString((a ? "▸ ":"  ") + c, x + 10, y + (22 - Deception.fontManager.s12.getHeight())/2f + 1, a ? 0xFFAF52DE : (h2 ? 0xFFF0F2F5 : 0xFF8A8F9D));
            boolean hx = mx >= x + w - 24 && mx <= x + w - 4 && my >= y && my <= y + 22;
            Gui.drawRect(x + w - 24, y + 4, x + w - 4, y + 18, hx ? 0xFF3C1420 : 0xFF1C1A28);
            Deception.fontManager.s12.drawString("X", x + w - 24 + (20 - Deception.fontManager.s12.getStringWidth("X"))/2f, y + (18 - Deception.fontManager.s12.getHeight())/2f + 1, hx ? 0xFFFF453A : 0xFF636366);
            y += 26;
        }
    }

    @Override protected void mouseClicked(int mx, int my, int btn) {
        int x = pX, y = pY, w = PW;
        if (my >= y + 10 && my <= y + 42 && mx >= x && mx <= x + w) { dragPanel = true; dx = mx - x; dy = my - y; return; }

        int segY = y + 48, segX = x + 16, segW = w - 32, itemW = segW / CATS.length;
        if (my >= segY && my <= segY + 30) {
            int idx = (mx - segX) / itemW;
            if (idx >= 0 && idx < CATS.length) {
                if (idx != sel) {
                    // 切换分类时触发动画
                    catAnimTarget = segX + idx * itemW + 2;
                    catAnim.run(catAnimTarget);
                }
                sel = idx; sc = 0; cfgEdit = false; return;
            }
        }

        int cx = x + 16, cw = w - 32, cy2 = segY + 38, ch2 = PH - (cy2 - y) - 16;
        if (CATS[sel] == Category.SETTINGS) { handleSettings(mx, my); return; }

        List<Module> mods = getFilteredMods();
        if (mods.isEmpty()) return;
        int[] mhs = new int[mods.size()];
        for (int i = 0; i < mods.size(); i++) mhs[i] = getAnimatedModuleHeight(mods.get(i));
        int dy2 = cy2 + (int)smoothScroll;
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            boolean expd = exp.contains(m.getName());
            if (my >= dy2 && my <= dy2 + 30 && mx >= cx && mx <= cx + cw) {
                if (btn == 1) { if (expd) exp.remove(m.getName()); else exp.add(m.getName()); } else m.toggle(); return;
            }
            if (expd && mx >= cx && mx <= cx + cw && my >= dy2 + 30 && my <= dy2 + mhs[i]) {
                int sy2 = dy2 + 34;
                if (my >= sy2 && my <= sy2 + 18 && mx >= cx + 6 && mx <= cx + cw/2f - 2) { rebindMod = m.getName(); return; }
                sy2 += 22;
                List<Property<?>> props = Deception.propertyManager.properties.get(m.getClass());
                if (props != null) for (Property<?> p : props) {
                    if (my >= sy2 && my <= sy2 + 20 && mx >= cx + 8 && mx <= cx + cw - 8) { handleProp(p,mx,my,btn,cx+8,sy2,cw-16); return; }
                    sy2 += 22;
                }
            }
            dy2 += mhs[i] + 4;
        }
    }

    private List<Module> getFilteredMods() {
        List<Module> r = new ArrayList<>();
        for (Module m : Deception.moduleManager.modules.values())
            if (m.getCategory() == CATS[sel]) r.add(m);
        r.sort(Comparator.comparing(Module::getName));
        return r;
    }

    void handleSettings(int mx, int my) {
        int x = pX + 20, y = pY + 112, w = PW - 44, iw = w - 110;
        if (mx >= x + 4 && mx <= x + 4 + iw && my >= y && my <= y + 24) { cfgEdit = true; return; }
        if (mx >= x + 8 + iw && mx <= x + 8 + iw + 96 && my >= y + 2 && my <= y + 22) { cfgEdit = false; if (!cfgName.isEmpty()) { new Config(cfgName,false).save(); refreshCfgs(); } return; }
        y += 46;
        for (String c : cfgList) {
            if (mx >= x + w - 24 && mx <= x + w - 4 && my >= y && my <= y + 22) { new File("./config/Deception/",c+".json").delete(); refreshCfgs(); return; }
            if (mx >= x + 2 && mx <= x + w - 26 && my >= y && my <= y + 22) { cfgName = c; new Config(c,false).load(); refreshCfgs(); return; }
            y += 26;
        }
    }

    void handleProp(Property<?> p, int mx, int my, int btn, float px, float py, float pw) {
        if (p instanceof BooleanProperty) { ((BooleanProperty)p).setValue(!((BooleanProperty)p).getValue()); }
        else if (p instanceof ModeProperty) { if (btn == 0) ((ModeProperty)p).nextMode(); else ((ModeProperty)p).previousMode(); }
        else if (p instanceof FloatProperty || p instanceof IntProperty || p instanceof PercentProperty) {
            drag = p; float slW = pw - 50, slX = px + pw - slW - 6;
            float pct = Math.max(0, Math.min(1, (mx - slX) / slW));
            if (p instanceof FloatProperty) { FloatProperty fp = (FloatProperty)p; fp.setValue(fp.getMinimum() + (fp.getMaximum() - fp.getMinimum()) * pct); }
            else if (p instanceof IntProperty) { IntProperty ip = (IntProperty)p; ip.setValue((int)(ip.getMinimum() + (ip.getMaximum() - ip.getMinimum()) * pct)); }
            else { PercentProperty pp = (PercentProperty)p; pp.setValue((int)(pp.getMinimum() + (pp.getMaximum() - pp.getMinimum()) * pct)); }
        }
    }

    @Override protected void mouseClickMove(int mx, int my, int btn, long t) {
        if (dragPanel) { pX = mx - dx; pY = my - dy; return; }
        if (drag == null || btn != 0) return;
        float slW = Math.min(PW - 80, 110), slX = pX + 24 + (PW - 40) - slW - 6;
        float pct = Math.max(0, Math.min(1, (mx - slX) / slW));
        if (drag instanceof FloatProperty) { FloatProperty fp = (FloatProperty)drag; fp.setValue(fp.getMinimum() + (fp.getMaximum() - fp.getMinimum()) * pct); }
        else if (drag instanceof IntProperty) { IntProperty ip = (IntProperty)drag; ip.setValue((int)(ip.getMinimum() + (ip.getMaximum() - ip.getMinimum()) * pct)); }
        else if (drag instanceof PercentProperty) { PercentProperty pp = (PercentProperty)drag; pp.setValue((int)(pp.getMinimum() + (pp.getMaximum() - pp.getMinimum()) * pct)); }
    }
    @Override protected void mouseReleased(int mx, int my, int btn) { dragPanel = false; drag = null; }
    @Override protected void keyTyped(char c, int k) throws IOException {
        if (rebindMod != null) {
            if (k == Keyboard.KEY_ESCAPE) { Module m2 = getMod(rebindMod); if (m2 != null) m2.setKey(0); rebindMod = null; return; }
            if (k == Keyboard.KEY_DELETE || k == Keyboard.KEY_BACK) k = 0;
            Module m2 = getMod(rebindMod); if (m2 != null) m2.setKey(k); rebindMod = null; return;
        }
        if (cfgEdit) {
            if (k == Keyboard.KEY_ESCAPE || k == Keyboard.KEY_RETURN) { cfgEdit = false; return; }
            if (k == Keyboard.KEY_BACK || k == Keyboard.KEY_DELETE) { if (!cfgName.isEmpty()) cfgName = cfgName.substring(0,cfgName.length()-1); return; }
            if (c >= 32 && c < 127 && cfgName.length() < 25) cfgName += c; return;
        }
        if (k == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
    }
    private Module getMod(String n) { for (Module m : Deception.moduleManager.modules.values()) if (m.getName().equalsIgnoreCase(n)) return m; return null; }
    @Override public void handleMouseInput() throws IOException { super.handleMouseInput(); int d = Mouse.getEventDWheel(); if (d != 0) sc += d > 0 ? 24 : -24; }
    @Override public boolean doesGuiPauseGame() { return true; }
    @Override public void onGuiClosed() { cfgEdit = false; }

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
}