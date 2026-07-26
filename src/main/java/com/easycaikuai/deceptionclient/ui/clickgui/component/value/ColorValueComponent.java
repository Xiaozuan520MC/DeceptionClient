package com.easycaikuai.deceptionclient.ui.clickgui.component.value;

import com.easycaikuai.deceptionclient.gui.font.Fonts;
import com.easycaikuai.deceptionclient.property.Property;
import com.easycaikuai.deceptionclient.property.properties.ColorProperty;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUI;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUIGlobal;
import com.easycaikuai.deceptionclient.util.gui.GUIUtil;
import com.easycaikuai.deceptionclient.util.render.ColorUtil;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;

import java.awt.Color;

public class ColorValueComponent extends ValueComponent {
    public ColorValueComponent(Property<?> property) {
        super(property);
    }

    public void draw(double x, double y, int mouseX, int mouseY, float partialTicks) {
        this.positionX = x;
        this.positionY = y;
        RiseClickGUI gui = RiseClickGUIGlobal.getGUI();
        ColorProperty colorProp = (ColorProperty) this.property;

        Fonts.interBold.get(15.0F).drawString(this.property.getName(), (float) x, (float) y + 2.0F, gui.fontDarkColor.getRGB());

        float moduleWidth = gui.scaleW - (float) gui.sidebar.sidebarWidth - 14.0F - 12.0F;
        float previewSize = 16.0F;
        float previewX = (float) x + moduleWidth - previewSize;

        RoundedUtils.drawRound(previewX, (float) y + 1.0F, previewSize, previewSize, 3.0F, new Color(colorProp.getValue()));
        RoundedUtils.drawRound(previewX, (float) y + 1.0F, previewSize, previewSize, 3.0F, ColorUtil.withAlpha(gui.fontColor, 30));
    }

    public void click(int mouseX, int mouseY, int mouseButton) {}
    public void released() {}
}