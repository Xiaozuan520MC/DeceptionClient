package com.easycaikuai.deceptionclient.module.modules.movement;

import net.minecraft.client.Minecraft;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.events.SafeWalkEvent;
import com.easycaikuai.deceptionclient.events.UpdateEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.module.modules.player.Scaffold;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;
import com.easycaikuai.deceptionclient.util.ItemUtil;
import com.easycaikuai.deceptionclient.util.MoveUtil;
import com.easycaikuai.deceptionclient.util.PlayerUtil;

public class SafeWalk extends Module {
    public final PercentProperty chance = new PercentProperty("Chance", 100);
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final FloatProperty motion = new FloatProperty("Motion", 1.0F, 0.5F, 1.0F);
    public final FloatProperty speedMotion = new FloatProperty("Speed Motion", 1.0F, 0.5F, 1.5F);
    public final BooleanProperty air = new BooleanProperty("Air", false);
    public final BooleanProperty directionCheck = new BooleanProperty("Direction Check", true);
    public final BooleanProperty pitCheck = new BooleanProperty("Pitch Check", true);
    public final BooleanProperty requirePress = new BooleanProperty("Require Press", false);
    public final BooleanProperty blocksOnly = new BooleanProperty("Blocks Only", true);

    public SafeWalk() {
        super("SafeWalk", false);
    }

    private boolean canSafeWalk() {
        Scaffold scaffold = (Scaffold) Deception.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled()) {
            return false;
        } else if (this.directionCheck.getValue() && mc.gameSettings.keyBindForward.isKeyDown()) {
            return false;
        } else if (this.pitCheck.getValue() && mc.thePlayer.rotationPitch < 69.0F) {
            return false;
        } else if (this.blocksOnly.getValue() && !ItemUtil.isHoldingBlock()) {
            return false;
        } else {
            return (!this.requirePress.getValue() || mc.gameSettings.keyBindUseItem.isKeyDown()) && (mc.thePlayer.onGround && PlayerUtil.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -1.0)
                    || this.air.getValue() && PlayerUtil.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -2.0));
        }
    }

    @EventTarget
    public void onMove(SafeWalkEvent event) {
        if (this.isEnabled()) {
            if (this.canSafeWalk()) {
                event.setSafeWalk(true);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.thePlayer.onGround && MoveUtil.isForwardPressed() && this.canSafeWalk()) {
                if (MoveUtil.getSpeedLevel() <= 0) {
                    if (this.motion.getValue() != 1.0F) {
                        MoveUtil.setSpeed(MoveUtil.getSpeed() * (double) this.motion.getValue());
                    }
                } else if (this.speedMotion.getValue() != 1.0F) {
                    MoveUtil.setSpeed(MoveUtil.getSpeed() * (double) this.speedMotion.getValue());
                }
            }
        }
    }
}
