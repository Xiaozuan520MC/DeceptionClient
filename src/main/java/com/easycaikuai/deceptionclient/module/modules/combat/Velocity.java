package com.easycaikuai.deceptionclient.module.modules.combat;

import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.events.*;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.*;
import com.easycaikuai.deceptionclient.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static boolean extraAttacked = false;

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{
            "Vanilla", "Prediction", "Grim", "Vulcan", "Legit", "Ground", "Intave"
    });

    // Vanilla settings
    public final PercentProperty chance = new PercentProperty("Chance", 100, () -> mode.getValue() == 0);
    public final PercentProperty horizontal = new PercentProperty("Horizontal", 0, () -> mode.getValue() == 0);
    public final PercentProperty vertical = new PercentProperty("Vertical", 0, () -> mode.getValue() == 0);
    public final PercentProperty explosionHorizontal = new PercentProperty("Explosions Horizontal", 100, () -> mode.getValue() == 0);
    public final PercentProperty explosionVertical = new PercentProperty("Explosions Vertical", 100, () -> mode.getValue() == 0);

    // Prediction settings
    public final BooleanProperty reduce = new BooleanProperty("Reduce", true, () -> mode.getValue() == 1);
    public final ModeProperty reduceMode = new ModeProperty("ReduceMode", 0, new String[]{"Attack", "ReleaseWhenCanAttack", "ReleaseBeforeCanAttack"}, () -> mode.getValue() == 1 && reduce.getValue());
    public final BooleanProperty jump = new BooleanProperty("Jump", true, () -> mode.getValue() == 1);
    public final BooleanProperty delay = new BooleanProperty("Delay", false, () -> mode.getValue() == 1);
    public final IntProperty delayTicks = new IntProperty("Delay Ticks", 1, 1, 5, () -> mode.getValue() == 1 && delay.getValue());

    // Vulcan settings
    public final PercentProperty vulcanHorizontal = new PercentProperty("Horizontal", 0, () -> mode.getValue() == 3);
    public final PercentProperty vulcanVertical = new PercentProperty("Vertical", 0, () -> mode.getValue() == 3);

    // Legit settings
    public final PercentProperty legitChance = new PercentProperty("Chance", 100, () -> mode.getValue() == 4);

    // Ground settings
    public final IntProperty groundDelay = new IntProperty("Delay", 1, 0, 20, () -> mode.getValue() == 5);

    private boolean velocityActive, attacked, slowDown;

    public Velocity() {
        super("Velocity", false, false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || event.getType() != EventType.RECEIVE) return;

        String currentMode = mode.getModeString();

        if (currentMode.equals("Vanilla")) {
            handleVanilla(event);
        } else if (currentMode.equals("Grim")) {
            handleGrim(event);
        } else if (currentMode.equals("Vulcan")) {
            handleVulcan(event);
        } else if (currentMode.equals("Legit")) {
            handleLegit(event);
        }
    }

    @EventTarget
    public void onPreMotion(PreMotionEvent event) {
        if (!isEnabled()) return;
        String currentMode = mode.getModeString();

        if (currentMode.equals("Grim") && velocityActive) {
            PacketUtil.sendPacket(new C07PacketPlayerDigging(
                    mc.objectMouseOver != null && mc.thePlayer.isSwingInProgress && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                            ? C07PacketPlayerDigging.Action.START_DESTROY_BLOCK
                            : C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                    new BlockPos(mc.thePlayer), EnumFacing.UP));
            velocityActive = false;
        }

        if (currentMode.equals("Ground")) {
            if (mc.thePlayer.hurtTime > 0) {
                mc.thePlayer.onGround = true;
            }
        }

        if (currentMode.equals("Intave")) {
            if (attacked && !slowDown && mc.thePlayer.isSprinting()) {
                mc.thePlayer.motionX *= 0.6D;
                mc.thePlayer.motionZ *= 0.6D;
                mc.thePlayer.setSprinting(false);
            }
            attacked = false;
            slowDown = false;
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!isEnabled()) return;
        if (mode.getModeString().equals("Intave")) {
            attacked = true;
        }
    }

    private void handleVanilla(PacketEvent event) {
        Packet<?> p = event.getPacket();
        if (p instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity v = (S12PacketEntityVelocity) p;
            if (v.getEntityID() == mc.thePlayer.getEntityId()) {
                int h = horizontal.getValue();
                int ve = vertical.getValue();
                if (h == 0 && ve == 0) {
                    event.setCancelled(true);
                    return;
                }
                // Cancel original packet and apply modified velocity directly
                event.setCancelled(true);
                double mx = v.getMotionX() / 8000.0D * h / 100.0D;
                double my = v.getMotionY() / 8000.0D * ve / 100.0D;
                double mz = v.getMotionZ() / 8000.0D * h / 100.0D;
                mc.thePlayer.motionX += mx;
                mc.thePlayer.motionY += my;
                mc.thePlayer.motionZ += mz;
            }
        } else if (p instanceof S27PacketExplosion) {
            S27PacketExplosion e = (S27PacketExplosion) p;
            if (explosionHorizontal.getValue() == 0 || explosionVertical.getValue() == 0) {
                event.setCancelled(true);
            }
        }
    }

    private void handleGrim(PacketEvent event) {
        Packet<?> p = event.getPacket();
        if (p instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus s = (S19PacketEntityStatus) p;
            if (s.getEntity(mc.theWorld) == mc.thePlayer && s.getOpCode() == 2) {
                velocityActive = true;
            }
        }
        if (p instanceof S12PacketEntityVelocity && velocityActive) {
            S12PacketEntityVelocity v = (S12PacketEntityVelocity) p;
            if (v.getEntityID() == mc.thePlayer.getEntityId()) {
                event.setCancelled(true);
                velocityActive = false;
            }
        }
    }

    private void handleVulcan(PacketEvent event) {
        Packet<?> p = event.getPacket();
        if (p instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity v = (S12PacketEntityVelocity) p;
            if (v.getEntityID() == mc.thePlayer.getEntityId()) {
                double h = vulcanHorizontal.getValue();
                double ve = vulcanVertical.getValue();
                if (h == 0 && ve == 0) {
                    event.setCancelled(true);
                } else {
                    event.setCancelled(true);
                    mc.thePlayer.motionX += v.getMotionX() / 8000.0D * h / 100.0D;
                    mc.thePlayer.motionY += v.getMotionY() / 8000.0D * ve / 100.0D;
                    mc.thePlayer.motionZ += v.getMotionZ() / 8000.0D * h / 100.0D;
                }
            }
        }
    }

    private void handleLegit(PacketEvent event) {
        Packet<?> p = event.getPacket();
        if (p instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity v = (S12PacketEntityVelocity) p;
            if (v.getEntityID() == mc.thePlayer.getEntityId() && v.getMotionY() > 0 && mc.thePlayer.onGround) {
                if (Math.random() * 100 < legitChance.getValue()) {
                    event.setCancelled(true);
                    mc.thePlayer.motionY = 0.42F;
                }
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }
}