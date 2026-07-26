package com.easycaikuai.deceptionclient.ui.clickgui.component;

import com.easycaikuai.deceptionclient.gui.font.Fonts;
import com.easycaikuai.deceptionclient.module.Category;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUI;
import com.easycaikuai.deceptionclient.util.render.ColorUtil;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;
import java.awt.Color;

public class SidebarCategory {
    private CategoryComponent[] categories;
    public double sidebarWidth = 120.0D;
    private double opacity;
    private long lastTime = 0L;

    public SidebarCategory() {
        Category[] cats = {Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC};
        this.categories = new CategoryComponent[cats.length];
        for (int i = 0; i < cats.length; i++) {
            this.categories[i] = new CategoryComponent(cats[i]);
        }
    }

    public void preRenderClickGUI(RiseClickGUI gui) {
        int alpha = (int) Math.min(this.opacity, gui.sidebarColor.getAlpha());
        if (alpha < 0) alpha = 0;
        else if (alpha > 255) alpha = 255;
        Color color = new Color(gui.sidebarColor.getRed(), gui.sidebarColor.getGreen(), gui.sidebarColor.getBlue(), alpha);

        RoundedUtils.drawRound(gui.positionX, gui.positionY, (float) this.sidebarWidth, gui.scaleH, 7.0F, color);

        // Logo
        float logoX = gui.positionX + 10.0F;
        float logoY = gui.positionY + 12.0F;
        Fonts.interBold.get(20.0F).drawString("Deception", logoX, logoY, new Color(gui.logoColor.getRed(), gui.logoColor.getGreen(), gui.logoColor.getBlue(), alpha).getRGB());

        // Separator
        float sepY = logoY + 24.0F;
        RoundedUtils.drawRound(gui.positionX + 8.0F, sepY, (float) this.sidebarWidth - 16.0F, 1.0F, 0.5F, ColorUtil.withAlpha(gui.separatorColor, Math.min(alpha, gui.separatorColor.getAlpha())));

        // Category items
        double offset = 40.0D;
        for (CategoryComponent cat : this.categories) {
            cat.render(gui, offset, this.sidebarWidth, alpha);
            offset += 34.0D;
        }
    }

    public void click(RiseClickGUI gui, int mouseX, int mouseY, int button) {
        for (CategoryComponent cat : this.categories) {
            cat.click(gui, mouseX, mouseY, button);
        }
    }

    public void updateOpacity(int targetOpacity) {
        long time = System.currentTimeMillis();
        long delta = time - this.lastTime;
        this.lastTime = time;
        if (delta > 50L) delta = 50L;
        if (this.opacity < targetOpacity) {
            this.opacity += delta * 0.4D;
            if (this.opacity > targetOpacity) this.opacity = targetOpacity;
        } else if (this.opacity > targetOpacity) {
            this.opacity -= delta * 0.4D;
            if (this.opacity < targetOpacity) this.opacity = targetOpacity;
        }
    }
}