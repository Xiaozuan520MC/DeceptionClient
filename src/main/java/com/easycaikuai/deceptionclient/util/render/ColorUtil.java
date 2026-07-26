package com.easycaikuai.deceptionclient.util.render;

import java.awt.Color;

public class ColorUtil {
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public static Color withAlpha(Color color, float alpha) {
        return withAlpha(color, (int) alpha);
    }
}