package com.easycaikuai.deceptionclient.module.modules.player;

import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.HitBlockEvent;
import com.easycaikuai.deceptionclient.events.TickEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.IntProperty;
import com.easycaikuai.deceptionclient.util.ItemUtil;
import com.easycaikuai.deceptionclient.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;

public class AutoTool extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty switchDelay = new IntProperty("Delay", 0, 0, 5);
    public final BooleanProperty switchBack = new BooleanProperty("Switch Back", true);
    public final BooleanProperty sneakOnly = new BooleanProperty("Sneak Only", false);

    private int currentToolSlot = -1;
    private int previousSlot = -1;
    private int tickDelayCounter = 0;
    private int blockBreak = 0;

    public AutoTool() {
        super("AutoTool", false);
    }

    @EventTarget
    public void onHitBlock(HitBlockEvent event) {
        if (!isEnabled()) return;
        blockBreak = 15;
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!isEnabled()) return;

        if (mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && mc.gameSettings.keyBindAttack.isKeyDown()
                && !mc.thePlayer.isUsingItem()
                && (!sneakOnly.getValue() || KeyBindUtil.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()))) {

            if (blockBreak > 0) {
                blockBreak--;
                int slot = ItemUtil.findInventorySlot(
                        mc.thePlayer.inventory.currentItem,
                        mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock()
                );
                if (slot != -1 && mc.thePlayer.inventory.currentItem != slot) {
                    if (this.previousSlot == -1) {
                        this.previousSlot = mc.thePlayer.inventory.currentItem;
                    }
                    mc.thePlayer.inventory.currentItem = this.currentToolSlot = slot;
                }
            }
        } else {
            if (switchBack.getValue() && this.previousSlot != -1) {
                mc.thePlayer.inventory.currentItem = this.previousSlot;
            }
            this.currentToolSlot = -1;
            this.previousSlot = -1;
            this.tickDelayCounter = 0;
            blockBreak = 0;
        }
    }

    @Override
    public void onDisabled() {
        this.currentToolSlot = -1;
        this.previousSlot = -1;
        this.tickDelayCounter = 0;
        blockBreak = 0;
    }
}