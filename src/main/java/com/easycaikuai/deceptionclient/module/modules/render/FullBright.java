package com.easycaikuai.deceptionclient.module.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.events.TickEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;

public class FullBright extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Gamma", "Effect"});
    private float prevGamma = Float.NaN;
    private boolean appliedNightVision = false;

    public FullBright() {
        super("Fullbright", true, false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            switch (this.mode.getValue()) {
                case 0:
                    mc.gameSettings.gammaSetting = 1000.0F;
                    break;
                case 1:
                    mc.thePlayer.addPotionEffect(new PotionEffect(Potion.nightVision.id, 25940, 0));
            }
        }
    }

    @Override
    public void onEnabled() {
        switch (this.mode.getValue()) {
            case 0:
                this.prevGamma = mc.gameSettings.gammaSetting;
                break;
            case 1:
                this.appliedNightVision = true;
        }
    }

    @Override
    public void onDisabled() {
        if (!Float.isNaN(this.prevGamma)) {
            mc.gameSettings.gammaSetting = this.prevGamma;
            this.prevGamma = Float.NaN;
        }
        if (this.appliedNightVision) {
            if (mc.thePlayer != null) {
                mc.thePlayer.removePotionEffectClient(Potion.nightVision.id);
            }
            this.appliedNightVision = false;
        }
    }

    @Override
    public void verifyValue(String mode) {
        if (this.isEnabled()) {
            this.onDisabled();
            this.onEnabled();
        }
    }
}
