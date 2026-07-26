package com.easycaikuai.deceptionclient.module.modules.movement;

import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.StrafeEvent;
import com.easycaikuai.deceptionclient.events.UpdateEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;
import com.easycaikuai.deceptionclient.util.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;

public class Fly extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{
            "Vanilla", "AirWalk", "Old NCP", "Verus", "Air Jump"
    });
    public final FloatProperty speed = new FloatProperty("Speed", 1.0F, 0.1F, 9.5F);
    public final FloatProperty vSpeed = new FloatProperty("Vertical Speed", 1.0F, 0.1F, 5.0F);

    public Fly() {
        super("Fly", false);
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!isEnabled()) return;
        String m = mode.getModeString();

        if (m.equals("Vanilla") || m.equals("AirWalk")) {
            MoveUtil.setSpeed(speed.getValue(), MoveUtil.getMoveYaw());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled()) return;
        String m = mode.getModeString();

        if (m.equals("Vanilla")) {
            mc.thePlayer.motionY = 0.0D
                    + (mc.gameSettings.keyBindJump.isKeyDown() ? vSpeed.getValue() : 0.0D)
                    - (mc.gameSettings.keyBindSneak.isKeyDown() ? vSpeed.getValue() : 0.0D);
        }

        if (m.equals("AirWalk")) {
            mc.thePlayer.motionY = 0.0D;
            mc.thePlayer.onGround = true;
            if (mc.gameSettings.keyBindJump.isKeyDown()) mc.thePlayer.motionY = vSpeed.getValue();
            if (mc.gameSettings.keyBindSneak.isKeyDown()) mc.thePlayer.motionY = -vSpeed.getValue();
        }

        if (m.equals("Old NCP")) {
            if (mc.thePlayer.ticksExisted % 3 == 0) {
                double y = mc.thePlayer.posY - 1.0E-10D;
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, y, mc.thePlayer.posZ, true));
            }
            mc.thePlayer.motionY = 0.0D
                    + (mc.gameSettings.keyBindJump.isKeyDown() ? 0.5D : 0.0D)
                    - (mc.gameSettings.keyBindSneak.isKeyDown() ? 0.5D : 0.0D);
            mc.thePlayer.capabilities.isFlying = false;
        }

        if (m.equals("Verus")) {
            mc.thePlayer.motionY = -0.0001D;
            if (mc.gameSettings.keyBindJump.isKeyDown()) mc.thePlayer.motionY = 0.42D;
            if (mc.gameSettings.keyBindSneak.isKeyDown()) mc.thePlayer.motionY = -0.42D;
            mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.1D, mc.thePlayer.posZ);
        }

        if (m.equals("Air Jump")) {
            if (mc.thePlayer.motionY < 0.0D && mc.thePlayer.ticksExisted % 2 == 0) {
                mc.thePlayer.motionY = 0.42D;
            }
            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.42D;
            }
            if (mc.gameSettings.keyBindJump.isKeyDown()) mc.thePlayer.motionY = vSpeed.getValue();
            if (mc.gameSettings.keyBindSneak.isKeyDown()) mc.thePlayer.motionY = -vSpeed.getValue();
        }
    }

    @Override
    public void onDisabled() {
        mc.thePlayer.motionX = 0.0D;
        mc.thePlayer.motionZ = 0.0D;
        mc.thePlayer.capabilities.isFlying = false;
        mc.thePlayer.capabilities.setFlySpeed(0.05F);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }
}