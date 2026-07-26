package com.easycaikuai.deceptionclient.module.modules.movement;

import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.event.types.Priority;
import com.easycaikuai.deceptionclient.events.*;
import com.easycaikuai.deceptionclient.mixin.IAccessorMinecraft;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.util.MoveUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;

public class Speed extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{
            "Vanilla", "Strafe", "NCP", "Vulcan", "Watchdog", "Grim"
    });
    public final FloatProperty speed = new FloatProperty("Speed", 1.0F, 0.1F, 10.0F, () -> mode.getValue() == 0);
    public final FloatProperty timer = new FloatProperty("Timer", 1.0F, 0.1F, 3.0F, () -> mode.getValue() != 0);
    public final PercentProperty strafe = new PercentProperty("Strafe", 100, () -> mode.getValue() == 1);

    private double moveSpeed;
    private int airTicks;

    public Speed() {
        super("Speed", false);
    }

    @EventTarget(Priority.LOW)
    public void onStrafe(StrafeEvent event) {
        if (!isEnabled()) return;
        String m = mode.getModeString();
        boolean moving = MoveUtil.getSpeed() > 0;

        if (m.equals("Vanilla") && moving) {
            MoveUtil.setSpeed(speed.getValue(), MoveUtil.getMoveYaw());
        }

        if (m.equals("Strafe") && moving) {
            ((IAccessorMinecraft)mc).getTimer().timerSpeed = timer.getValue();
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                moveSpeed = MoveUtil.getBaseMoveSpeed() * 2.05F;
                airTicks = 0;
            } else {
                airTicks++;
                moveSpeed = moveSpeed - moveSpeed / 159.0D;
                if (airTicks > 5) {
                    moveSpeed = MoveUtil.getBaseMoveSpeed() - 0.025D;
                }
            }
            if (moveSpeed < MoveUtil.getBaseMoveSpeed()) {
                moveSpeed = MoveUtil.getBaseMoveSpeed();
            }
            if (mc.thePlayer.moveForward > 0 && !mc.thePlayer.isCollidedHorizontally) {
                MoveUtil.setSpeed(Math.max(moveSpeed, MoveUtil.getSpeed()), MoveUtil.getMoveYaw());
            }
        }

        if (m.equals("NCP") && moving) {
            ((IAccessorMinecraft)mc).getTimer().timerSpeed = timer.getValue();
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                    moveSpeed = MoveUtil.getBaseMoveSpeed() * 2.15D;
                } else {
                    moveSpeed = MoveUtil.getBaseMoveSpeed() * 2.05D;
                }
            } else {
                moveSpeed -= moveSpeed / 139.0D;
            }
            moveSpeed = Math.max(moveSpeed, MoveUtil.getBaseMoveSpeed());
            MoveUtil.setSpeed(moveSpeed, MoveUtil.getMoveYaw());
        }

        if (m.equals("Vulcan") && moving) {
            ((IAccessorMinecraft)mc).getTimer().timerSpeed = timer.getValue();
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                moveSpeed = MoveUtil.getBaseMoveSpeed() * 1.5D;
                airTicks = 0;
            } else {
                airTicks++;
                moveSpeed = moveSpeed - moveSpeed / 159.0D;
                if (moveSpeed <= MoveUtil.getBaseMoveSpeed()) {
                    moveSpeed = MoveUtil.getBaseMoveSpeed();
                }
            }
            MoveUtil.setSpeed(moveSpeed, MoveUtil.getMoveYaw());
        }

        if (m.equals("Watchdog") && moving) {
            ((IAccessorMinecraft)mc).getTimer().timerSpeed = timer.getValue();
            if (mc.thePlayer.onGround) {
                mc.thePlayer.jump();
                moveSpeed = MoveUtil.getBaseMoveSpeed() * 1.6D;
            } else if (!mc.thePlayer.onGround) {
                moveSpeed -= moveSpeed / 99.0D;
            }
            moveSpeed = Math.max(moveSpeed, MoveUtil.getBaseMoveSpeed());
            MoveUtil.setSpeed(moveSpeed, MoveUtil.getMoveYaw());
        }

        if (m.equals("Grim") && moving) {
            if (mc.thePlayer.onGround && mc.thePlayer.ticksExisted % 3 == 0) {
                mc.thePlayer.jump();
                moveSpeed = MoveUtil.getBaseMoveSpeed() * 1.35D;
            } else if (!mc.thePlayer.onGround) {
                airTicks++;
                if (airTicks > 1) {
                    moveSpeed = MoveUtil.getBaseMoveSpeed();
                }
            }
            ((IAccessorMinecraft)mc).getTimer().timerSpeed = timer.getValue();
            MoveUtil.setSpeed(moveSpeed, MoveUtil.getMoveYaw());
        }
    }

    @Override
    public void onDisabled() {
        ((IAccessorMinecraft)mc).getTimer().timerSpeed = 1.0F;
        moveSpeed = 0.0D;
        airTicks = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }
}