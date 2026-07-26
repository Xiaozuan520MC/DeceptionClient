package com.easycaikuai.deceptionclient.module.modules.render;

import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;

public class NoHurtCam extends Module {
    public final PercentProperty multiplier = new PercentProperty("Multiplier", 0);

    public NoHurtCam() {
        super("NoHurtCam", false, true);
    }
}
