package com.easycaikuai.deceptionclient.ui.clickgui.component.value;

import com.easycaikuai.deceptionclient.gui.font.Fonts;
import com.easycaikuai.deceptionclient.property.Property;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;
import com.easycaikuai.deceptionclient.property.properties.IntProperty;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUI;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUIGlobal;
import com.easycaikuai.deceptionclient.util.TimerUtil;
import com.easycaikuai.deceptionclient.util.gui.GUIUtil;
import com.easycaikuai.deceptionclient.util.render.ColorUtil;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;

import java.text.DecimalFormat;

public class NumberValueComponent extends ValueComponent {
    private final TimerUtil stopwatch = new TimerUtil();
    private boolean dragging = false;

    public NumberValueComponent(Property<?> property) {
        super(property);
    }

    public void draw(double x, double y, int mouseX, int mouseY, float partialTicks) {
        this.positionX = x;
        this.positionY = y;
        RiseClickGUI gui = RiseClickGUIGlobal.getGUI();
        if (gui == null) return;

        Fonts.interBold.get(15.0F).drawString(this.property.getName(), (float) x, (float) y + 2.0F, gui.fontDarkColor.getRGB());

        float min, max, current;
        String displayValue;

        if (this.property instanceof FloatProperty) {
            FloatProperty prop = (FloatProperty) this.property;
            min = prop.getMinimum();
            max = prop.getMaximum();
            current = prop.getValue();
            displayValue = new DecimalFormat("0.0").format(current);
        } else if (this.property instanceof IntProperty) {
            IntProperty prop = (IntProperty) this.property;
            min = prop.getMinimum();
            max = prop.getMaximum();
            current = prop.getValue();
            displayValue = String.valueOf((int) current);
        } else if (this.property instanceof PercentProperty) {
            PercentProperty prop = (PercentProperty) this.property;
            min = 0;
            max = 100;
            current = prop.getValue();
            displayValue = current + "%";
        } else {
            return;
        }

        float moduleWidth = gui.scaleW - (float) gui.sidebar.sidebarWidth - 14.0F - 12.0F;
        float sliderW = Math.min(moduleWidth * 0.6F, 120.0F);
        float sliderX = (float) x + moduleWidth - sliderW;
        float sliderY = (float) y + 6.0F;
        float sliderH = 4.0F;

        // Value label
        float valueW = Fonts.interBold.get(15.0F).getStringWidth(displayValue);
        Fonts.interBold.get(15.0F).drawString(displayValue, sliderX - valueW - 4.0F, (float) y + 2.0F, gui.getAccentColor().getRGB());

        // Slider background
        RoundedUtils.drawRound(sliderX, sliderY, sliderW, sliderH, 2.0F, ColorUtil.withAlpha(gui.fontColor, 30));

        // Slider fill
        float range = max - min;
        float fill = (range > 0) ? ((current - min) / range) : 0.0F;
        float fillW = sliderW * fill;
        RoundedUtils.drawRound(sliderX, sliderY, fillW, sliderH, 2.0F, gui.getAccentColor());

        // Thumb
        float thumbX = sliderX + fillW;
        RoundedUtils.drawRound(thumbX - 3.0F, sliderY - 3.0F, 6.0F, sliderH + 6.0F, 3.0F, gui.getAccentColor());

        if (this.dragging) {
            float newFill = (mouseX - sliderX) / sliderW;
            newFill = Math.max(0.0F, Math.min(1.0F, newFill));
            float newVal = min + newFill * range;
            if (this.property instanceof FloatProperty) {
                ((FloatProperty) this.property).setValue(newVal);
            } else if (this.property instanceof IntProperty) {
                ((IntProperty) this.property).setValue(Math.round(newVal));
            } else if (this.property instanceof PercentProperty) {
                ((PercentProperty) this.property).setValue(Math.round(newVal));
            }
        }

        this.stopwatch.reset();
    }

    public void click(int mouseX, int mouseY, int mouseButton) {
        if (this.positionX == 0.0D) return;
        RiseClickGUI gui = RiseClickGUIGlobal.getGUI();
        if (gui == null) return;

        float moduleWidth = gui.scaleW - (float) gui.sidebar.sidebarWidth - 14.0F - 12.0F;
        float sliderW = Math.min(moduleWidth * 0.6F, 120.0F);
        float sliderX = (float) this.positionX + moduleWidth - sliderW;
        float sliderY = (float) this.positionY + 6.0F;

        if (GUIUtil.mouseOver(sliderX, sliderY - 3.0D, sliderW, 10.0D, mouseX, mouseY)) {
            if (mouseButton == 0) this.dragging = true;
        }
    }

    public void released() {
        this.dragging = false;
    }
}