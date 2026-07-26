package com.easycaikuai.deceptionclient.module.modules.movement;

import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.enums.BlinkModules;
import com.easycaikuai.deceptionclient.enums.FloatModules;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.event.types.Priority;
import com.easycaikuai.deceptionclient.events.*;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.module.modules.combat.KillAura;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.IntProperty;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.BlockPos;

import java.util.Random;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty swordMode = new ModeProperty("Sword Mode", 1, new String[]{
            "None", "Vanilla", "PredictionSemi", "Prediction", "Grim", "Watchdog", "NCP", "Intave"
    });
    public final IntProperty swapDelay = new IntProperty("Swap Delay", 0, 0, 3, () -> swordMode.getValue() == 3);
    public final BooleanProperty test = new BooleanProperty("Test", false, () -> swordMode.getValue() == 3);
    public final BooleanProperty c17 = new BooleanProperty("C17 Packet", false, () -> swordMode.getValue() == 3);
    public final BooleanProperty noAttack = new BooleanProperty("No Attack", false, () -> swordMode.getValue() == 3);
    public final IntProperty cancelTick = new IntProperty("Cancel Tick", 1, 0, 2, () -> swordMode.getValue() == 2);
    public final IntProperty cancelTick2 = new IntProperty("Cancel Tick 2", 1, 0, 2, () -> swordMode.getValue() == 2);
    public final PercentProperty swordMotion = new PercentProperty("Sword Motion", 100, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty swordSprint = new BooleanProperty("Sword Sprint", true, () -> this.swordMode.getValue() != 0);
    public final BooleanProperty onlyKillAuraAutoBlock = new BooleanProperty("Only Kill Aura Auto Block", false, () -> this.swordMode.getValue() != 0);
    public final ModeProperty foodMode = new ModeProperty("Food Mode", 0, new String[]{"None", "Vanilla", "Float"});
    public final PercentProperty foodMotion = new PercentProperty("Food Motion", 100, () -> this.foodMode.getValue() != 0);
    public final BooleanProperty foodSprint = new BooleanProperty("Food Sprint", true, () -> this.foodMode.getValue() != 0);
    public final ModeProperty bowMode = new ModeProperty("Bow Mode", 0, new String[]{"None", "Vanilla", "Float"});
    public final PercentProperty bowMotion = new PercentProperty("Bow Motion", 100, () -> this.bowMode.getValue() != 0);
    public final BooleanProperty bowSprint = new BooleanProperty("Bow Sprint", true, () -> this.bowMode.getValue() != 0);

    // Grim settings
    public final BooleanProperty grimSword = new BooleanProperty("Grim Sword", true, () -> swordMode.getValue() == 4);
    public final BooleanProperty grimFood = new BooleanProperty("Grim Food", true, () -> swordMode.getValue() == 4);
    public final BooleanProperty grimBow = new BooleanProperty("Grim Bow", true, () -> swordMode.getValue() == 4);

    // Watchdog settings
    public final BooleanProperty watchdogSlab = new BooleanProperty("Slab Check", true, () -> swordMode.getValue() == 5);

    private int lastSlot = -1;
    private int delay = 0;
    private boolean post = false;
    private int offGroundTicks = 0;
    private boolean watchdogDisable = false;

    public NoSlow() {
        super("NoSlow", false);
    }

    public boolean isSwordActive() {
        int m = this.swordMode.getValue();
        if (m == 0) return false;
        if (m >= 4) return mc.thePlayer.isUsingItem() && ItemUtil.isHoldingSword();
        return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword() && (!this.onlyKillAuraAutoBlock.getValue() || this.isKillAuraAutoBlocking());
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isFloatMode() {
        return this.foodMode.getValue() == 2 && ItemUtil.isEating()
                || this.bowMode.getValue() == 2 && ItemUtil.isUsingBow();
    }

    private boolean isKillAuraAutoBlocking() {
        KillAura aura = (KillAura) Deception.moduleManager.modules.get(KillAura.class);
        if (!aura.isPlayerBlocking() || !aura.isEnabled()) return false;
        return aura.isBlocking();
    }

    public boolean isAnyActive() {
        int m = this.swordMode.getValue();
        if (m >= 4) return mc.thePlayer.isUsingItem() && (isSwordActive() || isFoodActive() || isBowActive());
        if (m != 2 && m != 3) {
            return mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
        } else if (m == 2 && isSwordActive()) {
            KillAura killAura = (KillAura) Deception.moduleManager.getModule(KillAura.class);
            return killAura.isEnabled() && killAura.shouldAutoBlock() && (killAura.blockTick == cancelTick.getValue() || killAura.blockTick == cancelTick2.getValue());
        } else if (m == 3 && isSwordActive()) {
            KillAura killAura = (KillAura) Deception.moduleManager.getModule(KillAura.class);
            if (!noAttack.getValue() || !((killAura.blockTick == 0 && killAura.autoBlock.getValue() == 2) || (killAura.autoBlock.getValue() == 6 && killAura.blockTick == killAura.attackTick.getValue()) || (killAura.autoBlock.getValue() != 6 && killAura.autoBlock.getValue() != 2) || (killAura.autoBlock.getValue() == 5 && killAura.blockTick == 0) && killAura.isEnabled() && killAura.isPlayerBlocking())) {
                return delay == 0;
            }
        }
        return false;
    }

    public boolean canSprint() {
        return this.isSwordActive() && this.swordSprint.getValue()
                || this.isFoodActive() && this.foodSprint.getValue()
                || this.isBowActive() && this.bowSprint.getValue();
    }

    public int getMotionMultiplier() {
        if (ItemUtil.isHoldingSword()) return this.swordMotion.getValue();
        else if (ItemUtil.isEating()) return this.foodMotion.getValue();
        else return ItemUtil.isUsingBow() ? this.bowMotion.getValue() : 100;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) return;
        int m = this.swordMode.getValue();

        // Grim mode: swap items to bypass
        if (m == 4 && ItemUtil.isHoldingSword() && mc.thePlayer.isUsingItem()) {
            if (event.getType() == EventType.PRE) {
                int randomSlot = new Random().nextInt(9);
                while (randomSlot == mc.thePlayer.inventory.currentItem) {
                    randomSlot = new Random().nextInt(9);
                }
                PacketUtil.sendPacket(new C09PacketHeldItemChange(randomSlot));
                PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            }
        }

        // Watchdog mode: simple cancel
        if (m == 5 && mc.thePlayer.isUsingItem()) {
            // Watchdog bypass - cancel slowdown
            if (mc.thePlayer.onGround) {
                offGroundTicks = 0;
            } else {
                offGroundTicks++;
            }
        }

        // Prediction mode (existing)
        if (ItemUtil.isHoldingSword() && mc.thePlayer.isUsingItem()) {
            if (isSwordActive()) {
                if (m == 3) {
                    if (event.getType() == EventType.PRE) {
                        delay--;
                        if (delay < 0) {
                            KillAura killAura = (KillAura) Deception.moduleManager.getModule(KillAura.class);
                            if (!noAttack.getValue() || !((killAura.blockTick == 0 && killAura.autoBlock.getValue() == 2) || (killAura.autoBlock.getValue() == 6 && killAura.blockTick == killAura.attackTick.getValue()) || (killAura.autoBlock.getValue() != 6 && killAura.autoBlock.getValue() != 2) || (killAura.autoBlock.getValue() == 5 && killAura.blockTick == 0) && killAura.isEnabled() && killAura.isPlayerBlocking())) {
                                int randomSlot = new Random().nextInt(9);
                                while (randomSlot == mc.thePlayer.inventory.currentItem) {
                                    randomSlot = new Random().nextInt(9);
                                }
                                if (test.getValue()) {
                                    Deception.blinkManager.setBlinkState(true, BlinkModules.NO_SLOW);
                                }
                                PacketUtil.sendPacket(new C09PacketHeldItemChange(randomSlot));
                                if (c17.getValue()) {
                                    PacketUtil.sendPacket(new C17PacketCustomPayload("woshijiejue", new PacketBuffer(io.netty.buffer.Unpooled.buffer())));
                                }
                                PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                            }
                            post = true;
                            delay = swapDelay.getValue();
                        }
                    }
                }
            }
        } else {
            if (post) {
                if (test.getValue()) {
                    int randomSlot = new Random().nextInt(9);
                    while (randomSlot == mc.thePlayer.inventory.currentItem) {
                        randomSlot = new Random().nextInt(9);
                    }
                    Deception.blinkManager.setBlinkState(false, BlinkModules.NO_SLOW);
                    PacketUtil.sendPacket(new C09PacketHeldItemChange(randomSlot));
                    if (c17.getValue()) {
                        PacketUtil.sendPacket(new C17PacketCustomPayload("woshijiejue", new PacketBuffer(io.netty.buffer.Unpooled.buffer())));
                    }
                    PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                }
                post = false;
            }
        }
    }

    @EventTarget
    public void onMotion(PostMotionEvent event) {
        if (!this.isEnabled()) return;
        if (!ItemUtil.isHoldingSword() || !mc.thePlayer.isUsingItem()) return;
        if (isSwordActive() && this.swordMode.getValue() == 3) {
            if (post) {
                post = false;
                if (test.getValue()) {
                    PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                    Deception.blinkManager.setBlinkState(false, BlinkModules.NO_SLOW);
                }
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isEnabled() && this.isAnyActive()) {
            float multiplier = this.getMotionMultiplier() / 100.0F;
            mc.thePlayer.movementInput.moveForward *= multiplier;
            mc.thePlayer.movementInput.moveStrafe *= multiplier;
            if (!this.canSprint()) {
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @EventTarget(Priority.LOW)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled() && this.isFloatMode()) {
            int item = mc.thePlayer.inventory.currentItem;
            if (this.lastSlot != item && PlayerUtil.isUsingItem()) {
                this.lastSlot = item;
                Deception.floatManager.setFloatState(true, FloatModules.NO_SLOW);
            }
        } else {
            this.lastSlot = -1;
            Deception.floatManager.setFloatState(false, FloatModules.NO_SLOW);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (mc.objectMouseOver != null) {
                switch (mc.objectMouseOver.typeOfHit) {
                    case BLOCK:
                        BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                        if (BlockUtil.isInteractable(blockPos) && !PlayerUtil.isSneaking()) return;
                        break;
                    case ENTITY:
                        Entity entityHit = mc.objectMouseOver.entityHit;
                        if (entityHit instanceof EntityVillager) return;
                        if (entityHit instanceof EntityLivingBase && TeamUtil.isShop((EntityLivingBase) entityHit)) return;
                        break;
                }
            }
            if (this.isFloatMode() && !Deception.floatManager.isPredicted() && mc.thePlayer.onGround) {
                event.setCancelled(true);
                mc.thePlayer.motionY = 0.42F;
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{this.swordMode.getModeString()};
    }
}