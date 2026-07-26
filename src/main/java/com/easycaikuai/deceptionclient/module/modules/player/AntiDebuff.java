package com.easycaikuai.deceptionclient.module.modules.player;

import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;

public class AntiDebuff extends Module {
    public final PercentProperty chance = new PercentProperty("Chance", 100);
    public final BooleanProperty blindness = new BooleanProperty("Blindness", true);
    public final BooleanProperty nausea = new BooleanProperty("Nausea", true);

    public AntiDebuff() {
        super("AntiDebuff", false);
    }
}
