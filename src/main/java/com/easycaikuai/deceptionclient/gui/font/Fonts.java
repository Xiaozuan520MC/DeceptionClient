package com.easycaikuai.deceptionclient.gui.font;

import java.awt.Font;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public enum Fonts {
    interBold("inter/Inter_Bold"),
    interRegular("inter/Inter_Regular"),
    interMedium("inter/Inter_Medium"),
    interSemiBold("inter/Inter_SemiBold"),
    sfui("others/sfui"),
    Tahoma("others/Exhi"),
    icon1("Icon-1");

    private final Map<Float, FontRenderer> fontMap = new HashMap<>();
    private final String file;

    Fonts(String file) {
        this.file = file;
    }

    public FontRenderer get(float size) {
        return this.fontMap.computeIfAbsent(size, font -> {
            try {
                return create(this.file, size, true);
            } catch (Exception e) {
                throw new RuntimeException("Unable to load font: " + this, e);
            }
        });
    }

    public static FontRenderer create(String file, float size, boolean antiAlias) throws Exception {
        InputStream stream = Fonts.class.getResourceAsStream("/assets/minecraft/deceptionclient/font/" + file + ".ttf");
        if (stream == null) throw new RuntimeException("Font file not found: " + file);
        Font font = Font.createFont(Font.TRUETYPE_FONT, stream);
        font = font.deriveFont(size);
        stream.close();
        return new FontRenderer(font, antiAlias);
    }
}