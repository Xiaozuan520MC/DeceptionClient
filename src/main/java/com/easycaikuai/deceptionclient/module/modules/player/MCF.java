package com.easycaikuai.deceptionclient.module.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.KeyEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.util.ChatUtil;

public class MCF extends Module {
    public final PercentProperty chance = new PercentProperty("Chance", 100);
    private static final Minecraft mc = Minecraft.getMinecraft();

    public MCF() {
        super("MCF", false, true);
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (this.isEnabled() && event.getKey() == -98) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.ENTITY && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
                String hitName = mc.objectMouseOver.entityHit.getName();
                if (!Deception.friendManager.isFriend(hitName)) {
                    Deception.friendManager.add(hitName);
                    ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your friend list&r", Deception.clientName, hitName));
                } else {
                    Deception.friendManager.remove(hitName);
                    ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your friend list&r", Deception.clientName, hitName));
                }
            }
        }
    }
}
