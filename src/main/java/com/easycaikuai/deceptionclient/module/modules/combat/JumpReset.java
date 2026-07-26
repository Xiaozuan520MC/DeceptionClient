package com.easycaikuai.deceptionclient.module.modules.combat;

import net.minecraft.potion.Potion;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.KnockbackEvent;
import com.easycaikuai.deceptionclient.events.LivingUpdateEvent;
import com.easycaikuai.deceptionclient.mixin.IAccessorEntity;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.util.MoveUtil;

import static com.easycaikuai.deceptionclient.config.Config.mc;

public class JumpReset extends Module {
    private boolean jumpFlag = false;

    public JumpReset() {
        super("JumpReset", false);
    }

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (this.isEnabled()) {
            if (mc.thePlayer.hurtTime >= 7) {
                this.jumpFlag = true;
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround && MoveUtil.isForwardPressed() && !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb() && mc.thePlayer.isSprinting()) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }
}