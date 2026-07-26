package com.easycaikuai.deceptionclient.ui.clickgui.component.value;

import com.easycaikuai.deceptionclient.gui.font.Fonts;
import com.easycaikuai.deceptionclient.property.Property;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUI;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUIGlobal;
import com.easycaikuai.deceptionclient.util.TimerUtil;
import com.easycaikuai.deceptionclient.util.animation.Easing;
import com.easycaikuai.deceptionclient.util.animation.RiseAnim;
import com.easycaikuai.deceptionclient.util.gui.GUIUtil;
import com.easycaikuai.deceptionclient.util.render.ColorUtil;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;

public class ModeValueComponent extends ValueComponent {
    private boolean expanded = false;
    private final RiseAnim expandAnim = new RiseAnim(Easing.EASE_OUT_EXPO, 200L);
    private final TimerUtil stopwatch = new TimerUtil();
    private static final float ITEM_H = 18.0F;
    private static final float FONT_SIZE = 13.0F;

    public ModeValueComponent(Property<?> property) {
        super(property);
        this.height = 18.0D;
    }

    public void draw(double x, double y, int mouseX, int mouseY, float partialTicks) {
        this.positionX = x;
        this.positionY = y;
        RiseClickGUI gui = RiseClickGUIGlobal.getGUI();
        if (gui == null) return;
        ModeProperty modeValue = (ModeProperty) this.property;
        String name = this.property.getName();

        Fonts.interBold.get(15.0F).drawString(name, (float) x, (float) y + 2.0F, gui.fontDarkColor.getRGB());

        String currentMode = modeValue.getModeString();
        float moduleWidth = gui.scaleW - (float) gui.sidebar.sidebarWidth - 14.0F - 12.0F;
        float currentW = Fonts.interBold.get(15.0F).getStringWidth(currentMode);
        float valueX = (float) (x + moduleWidth - currentW);

        Fonts.interBold.get(15.0F).drawString(currentMode, valueX, (float) y + 2.0F, gui.getAccentColor().getRGB());

        this.height = 18.0D;

        this.expandAnim.run(this.expanded ? 1.0D : 0.0D);
        float expandVal = Math.max(0.0F, Math.min(1.0F, (float) this.expandAnim.getValue()));

        if (expandVal > 0.01D) {
            String[] modes = modeValue.getModes();
            int currentValue = (Integer) modeValue.getValue();
            float listY = (float) y + 20.0F;
            float listPad = 2.0F;
            float listW = moduleWidth - 4.0F;
            float listX = (float) x + listPad;
            float totalH = modes.length * 18.0F;
            float animH = totalH * expandVal;

            RoundedUtils.drawRound((float) x, listY - 2.0F, moduleWidth, animH + 4.0F, 5.0F,
                    ColorUtil.withAlpha(gui.fontColor, 15));

            int fontH = Fonts.interBold.get(13.0F).getHeight();
            float textYOffset = (18.0F - fontH) / 2.0F;

            for (int i = 0; i < modes.length; i++) {
                float itemY = listY + i * 18.0F;
                boolean isSelected = (i == currentValue);
                boolean isHover = GUIUtil.mouseOver(listX, itemY, listW, 18.0D, mouseX, mouseY);

                if (isSelected) {
                    RoundedUtils.drawRound(listX + 2.0F, itemY + 1.0F, listW - 4.0F, 16.0F, 4.0F,
                            ColorUtil.withAlpha(gui.getAccentColor(), 50));
                } else if (isHover) {
                    RoundedUtils.drawRound(listX + 2.0F, itemY + 1.0F, listW - 4.0F, 16.0F, 4.0F,
                            ColorUtil.withAlpha(gui.fontColor, 20));
                }

                int itemColor = isSelected ? gui.getAccentColor().getRGB() : gui.fontDarkColor.getRGB();
                Fonts.interBold.get(13.0F).drawString(modes[i], listX + 8.0F, itemY + textYOffset, itemColor);
            }
            this.height = (20.0F + animH + 4.0F);
        }
        this.stopwatch.reset();
    }

    public void click(int mouseX, int mouseY, int mouseButton) {
        if (this.positionX == 0.0D) return;
        RiseClickGUI gui = RiseClickGUIGlobal.getGUI();
        if (gui == null) return;
        ModeProperty modeValue = (ModeProperty) this.property;
        float moduleWidth = gui.scaleW - (float) gui.sidebar.sidebarWidth - 14.0F - 12.0F;

        if (GUIUtil.mouseOver(this.positionX, this.positionY - 2.0D, moduleWidth, 18.0D, mouseX, mouseY)) {
            if (mouseButton == 0) this.expanded = !this.expanded;
            return;
        }

        if (this.expanded) {
            String[] modes = modeValue.getModes();
            float listPad = 2.0F;
            float listX = (float) this.positionX + listPad;
            float listW = moduleWidth - 4.0F;
            for (int i = 0; i < modes.length; i++) {
                float itemY = (float) this.positionY + 20.0F + i * 18.0F;
                if (GUIUtil.mouseOver(listX, itemY, listW, 18.0D, mouseX, mouseY)) {
                    if (mouseButton == 0) {
                        modeValue.setValue(i);
                        this.expanded = false;
                    }
                    return;
                }
            }
        }
    }

    public void released() {}
}