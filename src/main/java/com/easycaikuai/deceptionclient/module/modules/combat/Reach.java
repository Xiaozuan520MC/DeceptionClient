package com.easycaikuai.deceptionclient.module.modules.combat;

import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.events.PickEvent;
import com.easycaikuai.deceptionclient.events.RaytraceEvent;
import com.easycaikuai.deceptionclient.events.TickEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;

public class Reach extends Module {
    private static final DecimalFormat df = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    public final FloatProperty range = new FloatProperty("Range", 3.1F, 3.0F, 6.0F);
    public final PercentProperty chance = new PercentProperty("Chance", 100);
    private final Random theRandom = new Random();
    private boolean expanding = true;

    public Reach() {
        super("Reach", false);
    }

    @EventTarget
    public void onPick(PickEvent event) {
        if (this.isEnabled() && this.expanding) {
            event.setRange(this.range.getValue().doubleValue());
        }
    }

    @EventTarget
    public void onRaytrace(RaytraceEvent event) {
        if (this.isEnabled() && this.expanding) {
            event.setRange(Math.max(event.getRange(), this.range.getValue().doubleValue() + 0.5));
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.expanding = this.theRandom.nextDouble() <= (double) this.chance.getValue() / 100.0;
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(this.range.getValue())};
    }
}
