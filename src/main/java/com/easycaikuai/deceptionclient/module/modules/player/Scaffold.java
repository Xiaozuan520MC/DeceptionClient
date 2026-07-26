package com.easycaikuai.deceptionclient.module.modules.player;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.WorldSettings.GameType;
import org.lwjgl.opengl.GL11;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.enums.BlinkModules;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.event.types.Priority;
import com.easycaikuai.deceptionclient.events.*;
import com.easycaikuai.deceptionclient.management.RotationState;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.module.modules.misc.BedNuker;
import com.easycaikuai.deceptionclient.module.modules.movement.LongJump;
import com.easycaikuai.deceptionclient.module.modules.render.HUD;
import com.easycaikuai.deceptionclient.property.properties.*;
import com.easycaikuai.deceptionclient.util.*;
import com.easycaikuai.deceptionclient.util.animations.advanced.Direction;
import com.easycaikuai.deceptionclient.util.animations.advanced.impl.DecelerateAnimation;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Scaffold extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final double[] placeOffsets = new double[]{
            0.03125, 0.09375, 0.15625, 0.21875, 0.28125, 0.34375,
            0.40625, 0.46875, 0.53125, 0.59375, 0.65625, 0.71875,
            0.78125, 0.84375, 0.90625, 0.96875
    };

    // ─── LeaderClient 设置 ─────────────────────────────────
    public final ModeProperty mode = new ModeProperty("Mode", 1,
            new String[]{"Normal", "Telly", "Snap"});
    public final ModeProperty rotationMode = new ModeProperty("Rotate Mode", 3,
            new String[]{"None", "Vanilla", "Backwards", "Prediction"});
    public final ModeProperty towerMode = new ModeProperty("Tower Mode", 0,
            new String[]{"None", "Vanilla", "Motion", "Jump"});
    public final ModeProperty moveFix = new ModeProperty("Move Fix", 1,
            new String[]{"None", "Silent"});
    public final IntProperty jumpDelay = new IntProperty("Jump Delay", 2, 0, 5,
            () -> mode.getValue() == 1);
    public final IntProperty placeDelay = new IntProperty("Place Delay", 1, 0, 5);
    public final FloatProperty startRotSpeed = new FloatProperty("Start Rotate Speed", 180.0F, 1.0F, 180.0F);
    public final FloatProperty normalRotSpeed = new FloatProperty("Normal Rotate Speed", 180.0F, 1.0F, 180.0F);
    public final BooleanProperty swing = new BooleanProperty("Swing", true);
    public final BooleanProperty itemSpoof = new BooleanProperty("Item Spoof", false);
    public final BooleanProperty clutch = new BooleanProperty("Clutch", true);
    public final BooleanProperty onlyInVoid = new BooleanProperty("Only Void", false, this.clutch::getValue);
    public final IntProperty forwardTicks = new IntProperty("Forward Ticks", 5, 1, 20,
            () -> mode.getValue() == 2);
    public final IntProperty backTicks = new IntProperty("Back Ticks", 5, 1, 20,
            () -> mode.getValue() == 2);

    // ─── RiseClient Sprint/Tower ──────────────────────────
    public final ModeProperty sprintMode = new ModeProperty("Sprint", 0,
            new String[]{"NONE", "VANILLA", "VULCAN", "WATCHDOG", "VERUS"});
    public final ModeProperty counter = new ModeProperty("Counter", 1, new String[]{"None", "Normal"});
    public final DecelerateAnimation counterAnim = new DecelerateAnimation(250, 1.0D);

    // ─── 内部状态 ─────────────────────────────────────────
    public static int count = 0;
    private int rotationTick = 0;
    private int lastSlot = -1;
    private int blockCount = -1;
    private float yaw = -180.0F;
    private float pitch = 0.0F;
    private boolean canRotate = false;
    private int tellyJumpDelayTimer = 0;
    private int jumpDelayOverride = -1;
    private boolean wasInAir = false;
    private int stage = 0;
    private int startY = 256;
    private boolean shouldKeepY = false;
    private boolean towering = false;
    private boolean clutchActive = false;
    private int clutchTickCounter = 0;
    private EnumFacing targetFacing = null;
    private double savedMotionX, savedMotionY, savedMotionZ;
    private int towerTicks = 0;
    private double lastTowerY = 0.0;
    private int placeDelayCounter = 0;
    private boolean sa;
    private int snapTickCounter = 0;
    private boolean snapForward = true;

    // DPS tracking
    private final Deque<Long> hitTimes = new ConcurrentLinkedDeque<>();
    private float currentDPS = 0;
    private boolean wasAttacking = false;

    public Scaffold() {
        super("Scaffold", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString(), rotationMode.getModeString()};
    }

    private boolean shouldStopSprint() {
        if (isTowering()) return false;
        return stage <= 0 && mode.getValue() != 2;
    }

    private boolean canPlace() {
        BedNuker bn = (BedNuker) Deception.moduleManager.modules.get(BedNuker.class);
        if (bn != null && bn.isEnabled() && bn.isReady()) return false;
        LongJump lj = (LongJump) Deception.moduleManager.modules.get(LongJump.class);
        return lj == null || !lj.isEnabled() || !lj.isAutoMode() || lj.isJumping();
    }

    private EnumFacing getBestFacing(BlockPos blockPos1, BlockPos blockPos3) {
        double offset = 0.0;
        EnumFacing facing = null;
        for (EnumFacing f : EnumFacing.VALUES) {
            if (f == EnumFacing.DOWN) continue;
            BlockPos pos = blockPos1.offset(f);
            if (pos.getY() <= blockPos3.getY()) {
                double dist = pos.distanceSqToCenter(blockPos3.getX() + 0.5, blockPos3.getY() + 0.5, blockPos3.getZ() + 0.5);
                if (facing == null || dist < offset || (dist == offset && f == EnumFacing.UP)) {
                    offset = dist; facing = f;
                }
            }
        }
        return facing;
    }

    private BlockData getBlockData() {
        int startY = MathHelper.floor_double(mc.thePlayer.posY);
        BlockPos targetPos = new BlockPos(
                MathHelper.floor_double(mc.thePlayer.posX),
                (stage != 0 && !shouldKeepY ? Math.min(startY, this.startY) : startY) - 1,
                MathHelper.floor_double(mc.thePlayer.posZ));
        if (!BlockUtil.isReplaceable(targetPos)) return null;
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int x = -4; x <= 4; x++)
            for (int y = -4; y <= 0; y++)
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    if (!BlockUtil.isReplaceable(pos) && !BlockUtil.isInteractable(pos)
                            && mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= mc.playerController.getBlockReachDistance()
                            && (stage == 0 || shouldKeepY || pos.getY() < this.startY))
                        for (EnumFacing f : EnumFacing.VALUES)
                            if (f != EnumFacing.DOWN && BlockUtil.isReplaceable(pos.offset(f)))
                                positions.add(pos);
                }
        if (positions.isEmpty()) return null;
        positions.sort(Comparator.comparingDouble(o ->
                o.distanceSqToCenter(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5)));
        BlockPos bp = positions.get(0);
        EnumFacing ef = getBestFacing(bp, targetPos);
        return ef == null ? null : new BlockData(bp, ef);
    }

    private void place(BlockPos blockPos, EnumFacing facing, Vec3 vec3) {
        if (ItemUtil.isHoldingBlock() && blockCount > 0) {
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                    mc.thePlayer.inventory.getCurrentItem(), blockPos, facing, vec3)) {
                if (mc.playerController.getCurrentGameType() != GameType.CREATIVE) blockCount--;
                if (swing.getValue()) mc.thePlayer.swingItem();
                else PacketUtil.sendPacket(new C0APacketAnimation());
            }
        }
    }

    private float getCurrentYaw() {
        return MoveUtil.adjustYaw(mc.thePlayer.rotationYaw,
                (float) MoveUtil.getForwardValue(), (float) MoveUtil.getLeftValue());
    }

    private boolean isDiagonal(float yaw) {
        float absYaw = Math.abs(yaw % 90.0F);
        return absYaw > 20.0F && absYaw < 70.0F;
    }

    private boolean isTowering() {
        if (!MoveUtil.isForwardPressed()) return false;
        if (PlayerUtil.isAirAbove()) return false;
        if (mc.thePlayer.onGround) {
            if (stage > 0 || mc.gameSettings.keyBindJump.isKeyDown()) return true;
        }
        return tellyJumpDelayTimer > 0;
    }

    private boolean isOnEdge() {
        if (!mc.thePlayer.onGround) return true;
        BlockPos below = new BlockPos(MathHelper.floor_double(mc.thePlayer.posX),
                MathHelper.floor_double(mc.thePlayer.posY) - 1, MathHelper.floor_double(mc.thePlayer.posZ));
        if (BlockUtil.isReplaceable(below)) return true;
        double edgeThreshold = 0.15;
        double xOff = mc.thePlayer.posX - MathHelper.floor_double(mc.thePlayer.posX);
        double zOff = mc.thePlayer.posZ - MathHelper.floor_double(mc.thePlayer.posZ);
        if (xOff < edgeThreshold || xOff > 1.0 - edgeThreshold || zOff < edgeThreshold || zOff > 1.0 - edgeThreshold) {
            int cx = MathHelper.floor_double(mc.thePlayer.posX) + (xOff < edgeThreshold ? -1 : (xOff > 1.0 - edgeThreshold ? 1 : 0));
            int cz = MathHelper.floor_double(mc.thePlayer.posZ) + (zOff < edgeThreshold ? -1 : (zOff > 1.0 - edgeThreshold ? 1 : 0));
            if (cx != MathHelper.floor_double(mc.thePlayer.posX) || cz != MathHelper.floor_double(mc.thePlayer.posZ)) {
                if (BlockUtil.isReplaceable(new BlockPos(cx, MathHelper.floor_double(mc.thePlayer.posY) - 1, cz)))
                    return true;
            }
        }
        return false;
    }

    private boolean isFallingIntoVoid() {
        if (mc.thePlayer == null) return false;
        for (int i = 0; i <= 128; i++) {
            if (mc.theWorld.getBlockState(new BlockPos(
                    MathHelper.floor_double(mc.thePlayer.posX),
                    MathHelper.floor_double(mc.thePlayer.posY) - i,
                    MathHelper.floor_double(mc.thePlayer.posZ))).getBlock().getMaterial().isSolid())
                return false;
        }
        return true;
    }

    private boolean bbUnC() {
        if (mc.thePlayer == null) return false;
        for (int i = 1; i <= 2; i++) {
            if (mc.theWorld.getBlockState(new BlockPos(
                    MathHelper.floor_double(mc.thePlayer.posX),
                    MathHelper.floor_double(mc.thePlayer.posY) - i,
                    MathHelper.floor_double(mc.thePlayer.posZ))).getBlock().getMaterial().isSolid())
                return true;
        }
        return false;
    }

    private void clutchReset() {
        if (clutchActive) Deception.moduleManager.getModule(Stuck.class).setEnabled(false);
        clutchActive = false;
        clutchTickCounter = 0;
    }

    private void updateClutch() {
        if (!clutch.getValue()) {
            if (clutchActive) clutchReset();
            return;
        }
        if (mc.thePlayer.onGround) { if (clutchActive) clutchReset(); return; }
        if (bbUnC()) { if (clutchActive) clutchReset(); return; }
        double fallDistance = mc.thePlayer.fallDistance;
        boolean shouldClutch = fallDistance > 2 && !PlayerUtil.isAirAbove()
                && !mc.thePlayer.isCollidedHorizontally
                && (!onlyInVoid.getValue() || this.isFallingIntoVoid());
        if (shouldClutch && !clutchActive) {
            clutchActive = true;
            savedMotionX = mc.thePlayer.motionX; savedMotionY = mc.thePlayer.motionY; savedMotionZ = mc.thePlayer.motionZ;
            clutchTickCounter = 0;
        }
        if (clutchActive) {
            clutchTickCounter++;
            if (clutchTickCounter % 10 != 0) {
                sa = true;
                if (clutchTickCounter % 10 == 1) {
                    savedMotionX = mc.thePlayer.motionX; savedMotionY = mc.thePlayer.motionY; savedMotionZ = mc.thePlayer.motionZ;
                }
                Deception.moduleManager.getModule(Stuck.class).setEnabled(true);
            } else {
                sa = false;
                Deception.moduleManager.getModule(Stuck.class).setEnabled(false);
            }
            if (clutchTickCounter >= 30) { clutchReset(); clutchActive = false; }
        }
    }

    @EventTarget(Priority.HIGH)
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || event.getType() != EventType.PRE) return;

        boolean tellyMode = mode.getValue() == 1;
        if (rotationTick > 0) rotationTick--;
        if (mc.thePlayer.onGround) {
            if (stage > 0) stage--;
            if (stage < 0) stage++;
            startY = shouldKeepY ? startY : MathHelper.floor_double(mc.thePlayer.posY);
            shouldKeepY = false; towering = false;
            if (wasInAir) {
                tellyJumpDelayTimer = tellyMode ? (jumpDelayOverride >= 0 ? jumpDelayOverride : jumpDelay.getValue()) : 0;
                wasInAir = false;
            }
            if (tellyJumpDelayTimer > 0) tellyJumpDelayTimer--;
        } else { wasInAir = true; }

        if (tellyMode && mc.thePlayer.onGround && MoveUtil.isForwardPressed()
                && !mc.gameSettings.keyBindJump.isKeyDown() && stage == 0) stage = 1;
        if (tellyMode) jumpDelayOverride = mc.gameSettings.keyBindJump.isKeyDown() ? 2 : -1;
        else { jumpDelayOverride = -1; tellyJumpDelayTimer = 0; }

        updateClutch();

        if (mode.getValue() == 2) {
            if (mc.thePlayer.onGround && !isOnEdge()) {
                int cycle = forwardTicks.getValue() + backTicks.getValue();
                if (cycle > 0) {
                    snapTickCounter++;
                    if (snapTickCounter >= cycle) snapTickCounter = 0;
                    snapForward = snapTickCounter < forwardTicks.getValue();
                }
            } else snapForward = false;
        }

        if (canPlace()) {
            // Auto slot selection
            ItemStack stack = mc.thePlayer.getHeldItem();
            int c = stack != null && stack.getItem() instanceof ItemBlock ? stack.stackSize : 0;
            blockCount = Math.min(blockCount, c);
            if (blockCount <= 0) {
                int slot = mc.thePlayer.inventory.currentItem;
                if (blockCount == 0) slot--;
                for (int i = slot; i > slot - 9; i--) {
                    int hb = (i % 9 + 9) % 9;
                    ItemStack cs = mc.thePlayer.inventory.getStackInSlot(hb);
                    if (cs != null && cs.getItem() instanceof ItemBlock) {
                        Block b = ((ItemBlock) cs.getItem()).getBlock();
                        if (!BlockUtil.isInteractable(b) && BlockUtil.isSolid(b)) {
                            mc.thePlayer.inventory.currentItem = hb;
                            blockCount = cs.stackSize;
                            break;
                        }
                    }
                }
            }

            if (mode.getValue() == 2) {
                if (snapForward) { yaw = RotationUtil.quantizeAngle(getCurrentYaw()); pitch = 80.0F; }
                else { yaw = RotationUtil.quantizeAngle(getCurrentYaw() + 180.0F); pitch = 85.0F; }
                canRotate = true;
            }

            float currentYaw = getCurrentYaw();
            float yawDiffTo180 = RotationUtil.wrapAngleDiff(currentYaw - 180.0F, event.getYaw());
            float diagonalYaw = isDiagonal(currentYaw) ? yawDiffTo180
                    : RotationUtil.wrapAngleDiff(currentYaw - 135.0F * ((currentYaw + 180.0F) % 90.0F < 45.0F ? 1.0F : -1.0F), event.getYaw());

            if (!canRotate) {
                switch (rotationMode.getValue()) {
                    case 1:
                        yaw = RotationUtil.quantizeAngle(yaw == -180.0F && pitch == 0.0F ? diagonalYaw : diagonalYaw);
                        if (yaw == -180.0F && pitch == 0.0F) pitch = RotationUtil.quantizeAngle(85.0F);
                        break;
                    case 2:
                        yaw = RotationUtil.quantizeAngle(yaw == -180.0F && pitch == 0.0F ? yawDiffTo180 : yawDiffTo180);
                        if (yaw == -180.0F && pitch == 0.0F) pitch = RotationUtil.quantizeAngle(85.0F);
                        break;
                    case 3:
                        if (yaw == -180.0F && pitch == 0.0F) { yaw = RotationUtil.quantizeAngle(diagonalYaw); pitch = RotationUtil.quantizeAngle(85.0F); }
                }
            }

            BlockData blockData = getBlockData();
            Vec3 hitVec = null;
            if (mode.getValue() == 2 && snapForward) blockData = null;

            if (blockData != null) {
                if (rotationMode.getValue() == 3) {
                    double[] offsets = {0.1, 0.3, 0.5, 0.7, 0.9};
                    double[] x = offsets, y = offsets, z = offsets;
                    switch (blockData.facing()) {
                        case NORTH: z = new double[]{0.02}; break;
                        case EAST:  x = new double[]{0.98}; break;
                        case SOUTH: z = new double[]{0.98}; break;
                        case WEST:  x = new double[]{0.02}; break;
                        case DOWN:  y = new double[]{0.02}; break;
                        case UP:    y = new double[]{0.98};
                    }
                    float bestYaw = -180.0F, bestPitch = 0.0F;
                    double bestDist = Double.MAX_VALUE;
                    for (double dx : x) for (double dy : y) for (double dz : z) {
                        float[] rot = RotationUtil.getRotations(blockData.blockPos().getX() + dx, blockData.blockPos().getY() + dy, blockData.blockPos().getZ() + dz);
                        MovingObjectPosition mop = RotationUtil.rayTrace(rot[0], rot[1], mc.playerController.getBlockReachDistance(), 1.0F);
                        if (mop != null && mop.typeOfHit == MovingObjectType.BLOCK
                                && mop.getBlockPos().equals(blockData.blockPos()) && mop.sideHit == blockData.facing()) {
                            double dist = Math.sqrt(Math.pow(MathHelper.wrapAngleTo180_float(rot[0] - yaw), 2) + Math.abs(rot[1] - pitch));
                            if (dist < bestDist) { bestYaw = rot[0]; bestPitch = rot[1]; bestDist = dist; hitVec = mop.hitVec; }
                        }
                    }
                    if (bestYaw != -180.0F || bestPitch != 0.0F) {
                        yaw = bestYaw + RandomUtil.nextFloat(-0.5F, 0.5F);
                        pitch = bestPitch + RandomUtil.nextFloat(-0.3F, 0.3F);
                        canRotate = true;
                    }
                } else {
                    double[] x = placeOffsets, y = placeOffsets, z = placeOffsets;
                    switch (blockData.facing()) {
                        case NORTH: z = new double[]{0.0}; break;
                        case EAST:  x = new double[]{1.0}; break;
                        case SOUTH: z = new double[]{1.0}; break;
                        case WEST:  x = new double[]{0.0}; break;
                        case DOWN:  y = new double[]{0.0}; break;
                        case UP:    y = new double[]{1.0};
                    }
                    float bestYaw = -180.0F, bestPitch = 0.0F, bestDiff = 0.0F;
                    for (double dx : x) for (double dy : y) for (double dz : z) {
                        double rx = blockData.blockPos().getX() + dx - mc.thePlayer.posX;
                        double ry = blockData.blockPos().getY() + dy - mc.thePlayer.posY - mc.thePlayer.getEyeHeight();
                        double rz = blockData.blockPos().getZ() + dz - mc.thePlayer.posZ;
                        float baseYaw = RotationUtil.wrapAngleDiff(yaw, event.getYaw());
                        float[] rot = RotationUtil.getRotationsTo(rx, ry, rz, baseYaw, pitch);
                        MovingObjectPosition mop = RotationUtil.rayTrace(rot[0], rot[1], mc.playerController.getBlockReachDistance(), 1.0F);
                        if (mop != null && mop.typeOfHit == MovingObjectType.BLOCK
                                && mop.getBlockPos().equals(blockData.blockPos()) && mop.sideHit == blockData.facing()) {
                            float diff = Math.abs(rot[0] - baseYaw) + Math.abs(rot[1] - pitch);
                            if (bestYaw == -180.0F || diff < bestDiff) { bestYaw = rot[0]; bestPitch = rot[1]; bestDiff = diff; hitVec = mop.hitVec; }
                        }
                    }
                    if (bestYaw != -180.0F || bestPitch != 0.0F) { yaw = bestYaw; pitch = bestPitch; canRotate = true; }
                }
            }

            if (canRotate && MoveUtil.isForwardPressed() && Math.abs(MathHelper.wrapAngleTo180_float(yawDiffTo180 - yaw)) < 90.0F) {
                if (rotationMode.getValue() == 2) yaw = RotationUtil.quantizeAngle(yawDiffTo180);
            }

            if (rotationMode.getValue() != 0 && mode.getValue() != 2) {
                float ty = yaw, tp = pitch;
                if (towering && (mc.thePlayer.motionY > 0.0 || mc.thePlayer.posY > startY + 1)) {
                    float yd = MathHelper.wrapAngleTo180_float(yaw - event.getYaw());
                    float tol = rotationTick >= 2 ? startRotSpeed.getValue() : normalRotSpeed.getValue();
                    if (Math.abs(yd) > tol) { ty = RotationUtil.quantizeAngle(event.getYaw() + RotationUtil.clampAngle(yd, tol)); rotationTick = Math.max(rotationTick, 1); }
                }
                if (tellyMode && isTowering() && tellyJumpDelayTimer <= 0) {
                    float yd = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - event.getYaw());
                    ty = RotationUtil.quantizeAngle(event.getYaw() + yd * RandomUtil.nextFloat(0.98F, 0.99F));
                    tp = RotationUtil.quantizeAngle(RandomUtil.nextFloat(30.0F, 80.0F));
                    rotationTick = 3; towering = true;
                } else if (tellyMode && tellyJumpDelayTimer > 0) {
                    ty = yaw != -180.0F ? yaw : RotationUtil.quantizeAngle(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - event.getYaw()) + event.getYaw());
                    tp = (pitch > 10 || pitch < -10) ? pitch : 60.0F;
                }
                event.setRotation(ty, tp, 3);
                if (moveFix.getValue() == 1) event.setPervRotation(ty, 3);
            } else if (mode.getValue() == 2 && rotationMode.getValue() != 0) {
                float ty = yaw, tp = pitch;
                float yd = MathHelper.wrapAngleTo180_float(ty - event.getYaw());
                float tol = rotationTick >= 2 ? startRotSpeed.getValue() : normalRotSpeed.getValue();
                if (Math.abs(yd) > tol) { ty = RotationUtil.quantizeAngle(event.getYaw() + RotationUtil.clampAngle(yd, tol)); rotationTick = Math.max(rotationTick, 1); }
                event.setRotation(ty, tp, 3);
                if (moveFix.getValue() == 1) event.setPervRotation(ty, 3);
            }

            if (blockData != null && hitVec != null && rotationTick <= 0) {
                if (placeDelayCounter > 0) placeDelayCounter--;
                else {
                    MovingObjectPosition fc = RotationUtil.rayTrace(yaw, pitch, mc.playerController.getBlockReachDistance(), 1.0F);
                    if (fc != null && fc.typeOfHit == MovingObjectType.BLOCK
                            && fc.getBlockPos().equals(blockData.blockPos()) && fc.sideHit == blockData.facing()) {
                        place(blockData.blockPos(), blockData.facing(), fc.hitVec);
                        placeDelayCounter = placeDelay.getValue();
                    } else if (canRotate) { place(blockData.blockPos(), blockData.facing(), hitVec); placeDelayCounter = placeDelay.getValue(); }
                }
            }
            if (targetFacing != null) {
                if (rotationTick <= 0) {
                    BlockPos bp = new BlockPos(MathHelper.floor_double(mc.thePlayer.posX), MathHelper.floor_double(mc.thePlayer.posY) - 1, MathHelper.floor_double(mc.thePlayer.posZ));
                    place(bp, targetFacing, BlockUtil.getHitVec(bp, targetFacing, yaw, pitch));
                }
                targetFacing = null;
            }
        }
    }

    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (!isEnabled()) return;
        if (clutchActive && clutchTickCounter % 10 != 0) { event.setForward(0); event.setStrafe(0); return; }
        if (mode.getValue() == 2) return;

        if (!mc.thePlayer.isCollidedHorizontally && mc.thePlayer.hurtTime <= 5
                && !mc.thePlayer.isPotionActive(Potion.jump)
                && mc.gameSettings.keyBindJump.isKeyDown() && ItemUtil.isHoldingBlock()
                && mc.thePlayer.onGround && tellyJumpDelayTimer <= 0 && PlayerUtil.isAirBelow()) {
            handleTower(event);
        }
    }

    private void handleTower(StrafeEvent event) {
        if (towerMode.getValue() == 0) return;
        startY = MathHelper.floor_double(mc.thePlayer.posY);
        towerTicks = 0; lastTowerY = mc.thePlayer.posY;
        switch (towerMode.getValue()) {
            case 1:
                mc.thePlayer.motionY = 0.42F;
                if (MoveUtil.isForwardPressed()) MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                else { MoveUtil.setSpeed(0); event.setForward(0); event.setStrafe(0); }
                break;
            case 2:
                if (!MoveUtil.isForwardPressed() && MoveUtil.getSpeed() < 0.01)
                    MoveUtil.setSpeed(0.005, MoveUtil.getMoveYaw());
                break;
            case 3:
                if (!MoveUtil.isForwardPressed()) MoveUtil.setSpeed(0.005, MoveUtil.getMoveYaw());
                else MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                break;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!isEnabled()) return;
        if (clutchActive && clutchTickCounter % 10 != 0) {
            mc.thePlayer.movementInput.moveForward = 0;
            mc.thePlayer.movementInput.moveStrafe = 0;
            mc.thePlayer.movementInput.jump = false;
            mc.thePlayer.movementInput.sneak = false;
            return;
        }
        if (moveFix.getValue() == 1 && RotationState.isActived()
                && RotationState.getPriority() == 3.0F && MoveUtil.isForwardPressed())
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        if (mode.getValue() == 1 && mc.thePlayer.onGround && stage > 0 && MoveUtil.isForwardPressed() && tellyJumpDelayTimer <= 0)
            mc.thePlayer.movementInput.jump = true;
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!isEnabled()) return;
        if (clutchActive && clutchTickCounter % 10 != 0) {
            mc.thePlayer.motionX = 0; mc.thePlayer.motionY = 0; mc.thePlayer.motionZ = 0; return;
        }
        if (shouldStopSprint()) mc.thePlayer.setSprinting(false);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (!isEnabled()) return;
        // Block count
        count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.stackSize > 0) {
                Item item = stack.getItem();
                if (item instanceof ItemBlock) {
                    Block block = ((ItemBlock) item).getBlock();
                    if (!BlockUtil.isInteractable(block) && BlockUtil.isSolid(block))
                        count += stack.stackSize;
                }
            }
        }

        ScaledResolution sr = new ScaledResolution(mc);

        // Block counter
        if (counter.getValue() == 1) {
            int hs = mc.thePlayer.inventory.currentItem;
            ItemStack held = mc.thePlayer.inventory.getStackInSlot(hs);
            if (held == null || !(held.getItem() instanceof ItemBlock)) {
                hs = -1;
                for (int i = 0; i < 9; i++) {
                    ItemStack s = mc.thePlayer.inventory.getStackInSlot(i);
                    if (s != null && s.getItem() instanceof ItemBlock) { hs = i; break; }
                }
            }
            renderBlockCounter(count, hs);
        }
    }

    private void renderBlockCounter(int count, int blockSlot) {
        counterAnim.setDirection(isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
        if (counterAnim.isDone() && counterAnim.getDirection() == Direction.BACKWARDS) return;
        ScaledResolution sr = new ScaledResolution(mc);
        float output = (float) counterAnim.getOutput();
        if (output <= 0.001F) return;
        ItemStack heldItem = (blockSlot == -1) ? null : mc.thePlayer.inventory.getStackInSlot(blockSlot);
        float blockWH = (heldItem != null) ? 15.0F : -2.0F;
        String text = "§l" + count + "§r block" + (count != 1 ? "s" : "");
        float textW = mc.fontRendererObj.getStringWidth(text);
        float totalW = (textW + blockWH + 3 + 6.0F) * output;
        float h = 20.0F;
        float x = sr.getScaledWidth() / 2.0F - totalW / 2.0F;
        float y = sr.getScaledHeight() / 2.0F + 6.0F;
        RoundedUtils.drawRound(x, y, totalW, h, 4.0F, new Color(20, 18, 18, (int) (80.0F * output)));
        if (heldItem != null) {
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(heldItem, (int) x + 3, (int) (y + 10.0F - blockWH / 2.0F));
            RenderHelper.disableStandardItemLighting();
        }
        mc.fontRendererObj.drawString(text, x + 3.0F + blockWH + 3,
                y + h / 2.0F - mc.fontRendererObj.FONT_HEIGHT / 2.0F + 1.0F, -1, true);
    }

    @EventTarget public void onLeftClick(LeftClickMouseEvent e) { if (isEnabled()) e.setCancelled(true); }
    @EventTarget public void onRightClick(RightClickMouseEvent e) { if (isEnabled()) e.setCancelled(true); }
    @EventTarget public void onHitBlock(HitBlockEvent e) { if (isEnabled()) e.setCancelled(true); }

    @EventTarget
    public void onSwap(SwapItemEvent event) {
        if (isEnabled()) { lastSlot = event.setSlot(lastSlot); event.setCancelled(true); }
    }

    @Override
    public void onEnabled() {
        if (mc.thePlayer != null) lastSlot = mc.thePlayer.inventory.currentItem;
        else lastSlot = -1;
        blockCount = -1; rotationTick = 3; yaw = -180.0F; pitch = 0.0F; canRotate = false;
        towering = false; towerTicks = 0; lastTowerY = 0.0; placeDelayCounter = 0;
        snapTickCounter = 0; snapForward = true;
    }

    @Override
    public void onDisabled() {
        clutchReset();
        if (mc.thePlayer != null && lastSlot != -1) mc.thePlayer.inventory.currentItem = lastSlot;
        snapTickCounter = 0;
    }

    public int getSlot() { return lastSlot; }

    public static class BlockData {
        private final BlockPos blockPos;
        private final EnumFacing facing;
        public BlockData(BlockPos bp, EnumFacing f) { this.blockPos = bp; this.facing = f; }
        public BlockPos blockPos() { return blockPos; }
        public EnumFacing facing() { return facing; }
    }
}