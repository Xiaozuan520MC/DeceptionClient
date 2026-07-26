package com.easycaikuai.deceptionclient.module.modules.combat;

import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.TickEvent;
import com.easycaikuai.deceptionclient.mixin.IAccessorKeyBinding;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class AutoGapple extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Normal", "Instant"});
    public final FloatProperty health = new FloatProperty("Health", 15.0F, 1.0F, 20.0F);
    public final BooleanProperty eatInCombat = new BooleanProperty("Eat In Combat", false);

    private boolean isEating = false;
    private int eatTickCount = 0;
    private int originalHotbarSlot = -1;

    public AutoGapple() {
        super("Gapple", false, false);
    }

    @Override
    public void onEnabled() {
        isEating = false;
        eatTickCount = 0;
        originalHotbarSlot = -1;
        super.onEnabled();
    }

    @Override
    public void onDisabled() {
        if (isEating) {
            ((IAccessorKeyBinding) mc.gameSettings.keyBindUseItem).setPressed(false);
        }
        isEating = false;
        eatTickCount = 0;
        originalHotbarSlot = -1;
        super.onDisabled();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        float currentHealth = mc.thePlayer.getHealth();
        if (currentHealth > health.getValue()) return;

        boolean instant = mode.getValue() == 1;

        if (instant) {
            handleInstantEat();
        } else if (!isEating) {
            int gappleSlot = findGappleInHotbar();
            if (gappleSlot != -1) {
                originalHotbarSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = gappleSlot;
                mc.playerController.updateController();
                ((IAccessorKeyBinding) mc.gameSettings.keyBindUseItem).setPressed(true);
                isEating = true;
                eatTickCount = 0;
            }
        } else {
            ItemStack heldItem = mc.thePlayer.getHeldItem();
            if (heldItem == null || heldItem.getItem() != Items.golden_apple) {
                ((IAccessorKeyBinding) mc.gameSettings.keyBindUseItem).setPressed(false);
                isEating = false;
                if (originalHotbarSlot != -1 && mc.thePlayer.inventory.currentItem != originalHotbarSlot) {
                    mc.thePlayer.inventory.currentItem = originalHotbarSlot;
                }
                originalHotbarSlot = -1;
                eatTickCount = 0;
                return;
            }

            eatTickCount++;
            if (eatTickCount >= 32) {
                ((IAccessorKeyBinding) mc.gameSettings.keyBindUseItem).setPressed(false);
                isEating = false;
                if (originalHotbarSlot != -1 && mc.thePlayer.inventory.currentItem != originalHotbarSlot) {
                    mc.thePlayer.inventory.currentItem = originalHotbarSlot;
                }
                originalHotbarSlot = -1;
                eatTickCount = 0;
            }
        }
    }

    private void handleInstantEat() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() == Items.golden_apple) {
                int oldSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = i;
                mc.playerController.updateController();

                // 发送使用物品包
                mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                // 立即发送停止使用包 — 服务端收到后即完成食用
                mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));

                // 客户端同步：停止使用物品动画
                mc.thePlayer.stopUsingItem();

                mc.thePlayer.inventory.currentItem = oldSlot;
                break;
            }
        }
    }

    private int findGappleInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() == Items.golden_apple) {
                return i;
            }
        }
        return -1;
    }
}