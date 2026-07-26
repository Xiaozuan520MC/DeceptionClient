package com.easycaikuai.deceptionclient.module.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.TickEvent;
import com.easycaikuai.deceptionclient.mixin.IAccessorEntityLivingBase;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.util.KeyBindUtil;

public class Sprint extends Module {
    public final PercentProperty chance = new PercentProperty("Chance", 100);
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final BooleanProperty foxFix = new BooleanProperty("FOV Fix", true);
    private boolean wasSprinting = false;

    public Sprint() {
        super("Sprint", true, false);
    }

    public boolean shouldApplyFovFix(IAttributeInstance attribute) {
        if (!this.foxFix.getValue()) {
            return false;
        } else {
            AttributeModifier attributeModifier = ((IAccessorEntityLivingBase) mc.thePlayer).getSprintingSpeedBoostModifier();
            return attribute.getModifier(attributeModifier.getID()) == null && this.wasSprinting;
        }
    }

    public boolean shouldKeepFov(boolean boolean2) {
        return this.foxFix.getValue() && !boolean2 && this.wasSprinting;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled()) {
            switch (event.getType()) {
                case PRE:
                    KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
                    break;
                case POST:
                    this.wasSprinting = mc.thePlayer.isSprinting();
            }
        }
    }

    @Override
    public void onDisabled() {
        this.wasSprinting = false;
        KeyBindUtil.updateKeyState(mc.gameSettings.keyBindSprint.getKeyCode());
    }
}
