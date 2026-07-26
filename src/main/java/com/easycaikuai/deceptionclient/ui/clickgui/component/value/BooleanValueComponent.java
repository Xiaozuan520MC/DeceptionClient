package com.easycaikuai.deceptionclient.ui.clickgui.component.value;

import com.easycaikuai.deceptionclient.gui.font.Fonts;
import com.easycaikuai.deceptionclient.property.Property;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUIGlobal;
import com.easycaikuai.deceptionclient.util.TimerUtil;
import com.easycaikuai.deceptionclient.util.gui.GUIUtil;
import com.easycaikuai.deceptionclient.util.render.ColorUtil;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;
import java.awt.Color;

public class BooleanValueComponent extends ValueComponent {
    private final TimerUtil stopwatch = new TimerUtil();
    private double scale;

    public BooleanValueComponent(Property<?> property) {
        super(property);
    }

    public void draw(double x, double y, int mouseX, int mouseY, float partialTicks) {
        this.positionX = x;
        this.positionY = y;
        BooleanProperty booleanValue = (BooleanProperty) this.property;

        Fonts.interBold.get(15.0F).drawString(this.property.getName(), (float) this.positionX, (float) this.positionY,
                RiseClickGUIGlobal.getGUI().fontDarkColor.getRGB());

        double positionX = this.positionX + Fonts.interBold.get(15.0F).getStringWidth(this.property.getName()) + 3.0D;

        if (booleanValue.getValue()) {
            this.scale = Math.min(5.0D, this.scale + ((float) this.stopwatch.getElapsedTime() / 20.0F));
        } else {
            this.scale = Math.max(0.0D, this.scale - ((float) this.stopwatch.getElapsedTime() / 20.0F));
        }

        RoundedUtils.drawRound((float) (positionX - 2.5D + 5.0D), (float) (this.positionY - 2.5D + 2.5D), 5.0F, 5.0F, 2.5F,
                ColorUtil.withAlpha(RiseClickGUIGlobal.getGUI().fontColor, 40));

        if (this.scale != 0.0D) {
            Color accentColor = RiseClickGUIGlobal.getGUI().getAccentColor();
            RoundedUtils.drawRound((float) (positionX - this.scale / 2.0D + 4.0D), (float) (this.positionY - this.scale / 2.0D + 2.5D), (float) this.scale, (float) this.scale, (float) (this.scale / 2.0D), accentColor);
        }

        this.stopwatch.reset();
    }

    public void click(int mouseX, int mouseY, int mouseButton) {
        if (this.positionX == 0.0D) return;
        BooleanProperty booleanValue = (BooleanProperty) this.property;
        if (GUIUtil.mouseOver(this.positionX, this.positionY - 3.5D, 250.0D, this.height, mouseX, mouseY))
            booleanValue.setValue(!booleanValue.getValue());
    }

    public void released() {}
}