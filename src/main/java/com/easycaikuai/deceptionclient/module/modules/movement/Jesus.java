package com.easycaikuai.deceptionclient.module.modules.movement;

import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Jesus extends Module {
    public final PercentProperty chance = new PercentProperty("Chance", 100);
    private static final DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
    public final FloatProperty speed = new FloatProperty("Speed", 2.5F, 0.0F, 3.0F);
    public final BooleanProperty noPush = new BooleanProperty("No Push", true);
    public final BooleanProperty groundOnly = new BooleanProperty("Ground Only", true);

    public Jesus() {
        super("Jesus", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(this.speed.getValue())};
    }
}
