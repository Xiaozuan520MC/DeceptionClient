package com.easycaikuai.deceptionclient.ui.clickgui.component;

import com.easycaikuai.deceptionclient.gui.font.Fonts;
import com.easycaikuai.deceptionclient.module.Category;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUI;
import com.easycaikuai.deceptionclient.util.animation.Easing;
import com.easycaikuai.deceptionclient.util.animation.RiseAnim;
import com.easycaikuai.deceptionclient.util.gui.GUIUtil;
import com.easycaikuai.deceptionclient.util.render.ColorUtil;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;
import java.awt.Color;

public class CategoryComponent {
    private RiseAnim animation = new RiseAnim(Easing.LINEAR, 200L);
    public final Category category;
    private long lastTime = 0L;
    private float x;
    private float y;
    private static final String[] ICONS = new String[]{"a", "b", "g", "c", "e"};
    private static final Category[] CATS = new Category[]{Category.COMBAT, Category.MOVEMENT, Category.RENDER, Category.PLAYER, Category.MISC};

    public CategoryComponent(Category category) {
        this.category = category;
    }

    public void render(RiseClickGUI gui, double offset, double sidebarWidth, int opacity) {
        if (System.currentTimeMillis() - this.lastTime > 300L) this.lastTime = System.currentTimeMillis();
        long time = System.currentTimeMillis();

        this.x = gui.positionX + 8.0F;
        float itemTopY = (float) (gui.positionY + offset) + 10.0F;
        this.y = itemTopY;

        this.animation.setDuration(200L);
        boolean isSelected = (gui.getSelectedCategory() == this.category);
        this.animation.run(isSelected ? 255.0D : 0.0D);

        double spacer = 8.0D;
        String iconName = getIcon();
        String catName = this.category.getDisplayName();

        float itemWidth = (float) (sidebarWidth - 16.0D);
        float itemHeight = 24.0F;

        RoundedUtils.drawRound(this.x, itemTopY, itemWidth, itemHeight, 3.0F,
                ColorUtil.withAlpha(gui.getAccentColor(), (int) (Math.min(this.animation.getValue(), opacity) * 0.15D)));

        int color = new Color(gui.fontColor.getRed(), gui.fontColor.getGreen(), gui.fontColor.getBlue(),
                Math.min(isSelected ? 255 : 200, opacity)).getRGB();

        float contentX = this.x + 8.0F;

        int iconH = Fonts.icon1.get(16.0F).getHeight();
        float iconY = itemTopY + (itemHeight - iconH) / 2.0F + 3.0F;
        Fonts.icon1.get(16.0F).drawString(iconName, contentX, iconY, color);

        int textH = Fonts.interBold.get(14.0F).getHeight();
        float textY = itemTopY + (itemHeight - textH) / 2.0F + 3.0F;
        Fonts.interBold.get(14.0F).drawString(catName, contentX + 8.0D + Fonts.icon1.get(16.0F).getStringWidth(iconName), textY, color);

        this.lastTime = time;
    }

    public void click(RiseClickGUI gui, int mouseX, int mouseY, int button) {
        boolean left = (button == 0);
        if (GUIUtil.mouseOver(this.x, this.y, (float) (gui.sidebar.sidebarWidth - 16.0D), 24.0D, mouseX, mouseY) && left) {
            gui.switchScreen(this.category);
        }
    }

    private String getIcon() {
        for (int i = 0; i < CATS.length; i++) {
            if (CATS[i] == this.category) return ICONS[i];
        }
        return "e";
    }
}