package com.easycaikuai.deceptionclient.module.modules.movement;

import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.events.Render2DEvent;
import com.easycaikuai.deceptionclient.events.StrafeEvent;
import com.easycaikuai.deceptionclient.events.UpdateEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;
import com.easycaikuai.deceptionclient.util.MoveUtil;
import com.easycaikuai.deceptionclient.util.PlayerUtil;
import com.easycaikuai.deceptionclient.util.RandomUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Ported from raven-bs Fly.
 *
 * <p>Adaptations for the DeceptionClient framework:
 * <ul>
 *   <li>Horizontal speed for the Fast / Fast 2 modes is applied in
 *       {@link #onStrafe(StrafeEvent)} via {@link MoveUtil#setSpeed(double, float)}
 *       instead of in the update tick, so it actually overrides the vanilla
 *       strafe movement (StrafeEvent is the framework's movement hook).</li>
 *   <li>The Walk / Keep-Y mode originally relied on Raven's CollisionEvent to
 *       inject a fake-floor bounding box. DeceptionClient has no collision
 *       event, so it is reproduced with a motion-based approach
 *       ({@code motionY = 0; onGround = true;} + optional height lock).</li>
 *   <li>BPS is drawn manually with the vanilla font renderer.</li>
 * </ul>
 */
public class Fly extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Vanilla", "Fast", "Fast 2", "Walk"});
    public final FloatProperty horizontalSpeed = new FloatProperty("Horizontal speed", 2.0F, 1.0F, 9.0F, () -> mode.getValue() != 3);
    public final FloatProperty verticalSpeed = new FloatProperty("Vertical speed", 2.0F, 1.0F, 9.0F, () -> mode.getValue() != 3);
    public final BooleanProperty showBPS = new BooleanProperty("Show BPS", false);
    public final BooleanProperty stopMotion = new BooleanProperty("Stop motion", false);
    public final BooleanProperty keepY = new BooleanProperty("Keep-Y", false, () -> mode.getValue() == 3);

    private boolean canFly;
    private int maxY;

    public Fly() {
        super("Fly", false);
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer == null) return;
        this.canFly = mc.thePlayer.capabilities.isFlying;
        this.maxY = mc.thePlayer.getPosition().getY();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }

    @EventTarget
    public void onStrafe(StrafeEvent e) {
        if (!isEnabled()) return;
        int m = mode.getValue();
        if (m == 1) { // Fast
            MoveUtil.setSpeed(0.85 * horizontalSpeed.getValue(), MoveUtil.getMoveYaw());
        } else if (m == 2) { // Fast 2
            MoveUtil.setSpeed(0.4 * horizontalSpeed.getValue(), MoveUtil.getMoveYaw());
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (e.getType() != EventType.PRE) return;
        if (!isEnabled() || mc.thePlayer == null) return;
        switch (mode.getValue()) {
            case 0: // Vanilla
                mc.thePlayer.motionY = 0.0D;
                mc.thePlayer.capabilities.setFlySpeed((float) (0.05000000074505806 * horizontalSpeed.getValue()));
                mc.thePlayer.capabilities.isFlying = true;
                break;
            case 1: // Fast
                mc.thePlayer.onGround = true;
                if (mc.currentScreen == null) {
                    // Source had a typo (Utils.jumpDown() twice); the down branch is
                    // treated as sneak so vertical descent actually works.
                    if (PlayerUtil.isJumping()) {
                        mc.thePlayer.motionY = 0.3 * verticalSpeed.getValue();
                    } else if (PlayerUtil.isSneaking()) {
                        mc.thePlayer.motionY = -0.3 * verticalSpeed.getValue();
                    } else {
                        mc.thePlayer.motionY = 0.0D;
                    }
                } else {
                    mc.thePlayer.motionY = 0.0D;
                }
                mc.thePlayer.capabilities.setFlySpeed(0.2f);
                mc.thePlayer.capabilities.isFlying = true;
                // horizontal handled in onStrafe
                break;
            case 2: { // Fast 2
                double nextDouble = RandomUtil.nextDouble(1.0E-7, 1.2E-7);
                if (mc.thePlayer.ticksExisted % 2 == 0) {
                    nextDouble = -nextDouble;
                }
                if (!mc.thePlayer.onGround) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + nextDouble, mc.thePlayer.posZ);
                }
                mc.thePlayer.motionY = 0.0D;
                // horizontal handled in onStrafe
                break;
            }
            case 3: // Walk (motion-based Keep-Y; no CollisionEvent in this framework)
                mc.thePlayer.motionY = 0.0D;
                mc.thePlayer.onGround = true;
                if (keepY.getValue()) {
                    if (PlayerUtil.isSneaking()) {
                        maxY = mc.thePlayer.getPosition().getY();
                    } else if (mc.thePlayer.posY < maxY) {
                        mc.thePlayer.setPosition(mc.thePlayer.posX, (double) maxY, mc.thePlayer.posZ);
                    }
                }
                break;
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent e) {
        if (!showBPS.getValue()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.currentScreen != null || mc.gameSettings.showDebugInfo) return;
        double bps = MoveUtil.getSpeed() * 20.0D;
        String text = String.format("BPS: %.2f", bps);
        ScaledResolution sr = new ScaledResolution(mc);
        float x = sr.getScaledWidth() / 2.0F - mc.fontRendererObj.getStringWidth(text) / 2.0F;
        float y = sr.getScaledHeight() / 2.0F + 30.0F;
        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFFFFFFF);
    }

    @Override
    public void onDisabled() {
        if (mc.thePlayer == null) return;
        if (mc.thePlayer.capabilities.allowFlying) {
            mc.thePlayer.capabilities.isFlying = this.canFly;
        } else {
            mc.thePlayer.capabilities.isFlying = false;
        }
        this.canFly = false;
        switch (mode.getValue()) {
            case 0: // Vanilla
            case 1: // Fast
                mc.thePlayer.capabilities.setFlySpeed(0.05F);
                break;
        }
        if (stopMotion.getValue()) {
            mc.thePlayer.motionZ = 0;
            mc.thePlayer.motionY = 0;
            mc.thePlayer.motionX = 0;
        }
    }
}
