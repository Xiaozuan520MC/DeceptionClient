package com.easycaikuai.deceptionclient.ui.clickgui.component.value;

import com.easycaikuai.deceptionclient.property.Property;

public abstract class ValueComponent {
    public double height = 14.0D;
    public double positionX;
    public double positionY;
    public Property<?> property;

    public abstract void draw(double d, double e, int i, int j, float f);
    public abstract void click(int i, int j, int k);

    public ValueComponent(Property<?> property) {
        this.property = property;
    }

    public abstract void released();

    public double getHeight() { return this.height; }
    public Property<?> getProperty() { return this.property; }
}