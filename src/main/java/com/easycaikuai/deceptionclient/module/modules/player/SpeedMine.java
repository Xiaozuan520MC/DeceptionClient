package com.easycaikuai.deceptionclient.module.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.events.TickEvent;
import com.easycaikuai.deceptionclient.mixin.IAccessorPlayerControllerMP;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.IntProperty;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;

public class SpeedMine extends Module {
    public final PercentProperty chance = new PercentProperty("Chance", 100);
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final PercentProperty speed = new PercentProperty("Speed", 15);
    public final IntProperty delay = new IntProperty("Delay", 0, 0, 4);

    public SpeedMine() {
        super("SpeedMine", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (!mc.playerController.isInCreativeMode()) {
                if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
                    ((IAccessorPlayerControllerMP) mc.playerController)
                            .setBlockHitDelay(Math.min(((IAccessorPlayerControllerMP) mc.playerController).getBlockHitDelay(), this.delay.getValue() + 1));
                    if (((IAccessorPlayerControllerMP) mc.playerController).getIsHittingBlock()) {
                        float curBlockDamageMP = ((IAccessorPlayerControllerMP) mc.playerController).getCurBlockDamageMP();
                        float damage = 0.3F * (this.speed.getValue().floatValue() / 100.0F);
                        if (curBlockDamageMP < damage) {
                            ((IAccessorPlayerControllerMP) mc.playerController).setCurBlockDamageMP(damage);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.format("%d%%", this.speed.getValue())};
    }
}
