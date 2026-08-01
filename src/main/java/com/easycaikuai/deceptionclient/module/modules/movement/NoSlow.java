package com.easycaikuai.deceptionclient.module.modules.movement;

import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.events.LivingUpdateEvent;
import com.easycaikuai.deceptionclient.events.MoveInputEvent;
import com.easycaikuai.deceptionclient.events.UpdateEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.util.ItemUtil;
import com.easycaikuai.deceptionclient.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 * Ported from raven-bs NoSlow.
 *
 * DeceptionClient's use-item slowdown is defeated via {@code MixinEntityPlayerSP},
 * which redirects {@code isUsingItem()} to return {@code false} while
 * {@link #isAnyActive()} is {@code true} (so the vanilla 0.2x slowdown is skipped).
 * This module then re-applies the configurable {@code slowed}% multiplier in
 * {@link #onLivingUpdate(LivingUpdateEvent)} to reproduce Raven's {@code getSlowed()}.
 */
public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final String[] NOSLOW_MODES = new String[]{"Vanilla", "Beta"};

    public final ModeProperty mode = new ModeProperty("Mode", 0, NOSLOW_MODES);
    public final PercentProperty slowed = new PercentProperty("Slow %", 80, 0, 80, null);
    public final BooleanProperty disableBow = new BooleanProperty("Disable bow", false);
    public final BooleanProperty disablePotions = new BooleanProperty("Disable potions", false);
    public final BooleanProperty swordOnly = new BooleanProperty("Sword only", false);
    public final BooleanProperty vanillaSword = new BooleanProperty("Vanilla sword", false);

    public boolean noSlowing;
    private boolean setJump;

    public NoSlow() {
        super("NoSlow", false);
    }

    @Override
    public void onDisabled() {
        noSlowing = false;
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (e.getType() != EventType.PRE) return;
        if (!isEnabled()) return;
        if (vanillaSword.getValue() && ItemUtil.isHoldingSword()) {
            return;
        }
        boolean apply = getSlowed() != 0.2f;
        if (!apply || !mc.thePlayer.isUsingItem()) {
            return;
        }
        if (mode.getValue() == 1) { // Beta
            PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1));
            PacketUtil.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            PacketUtil.sendPacket(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent e) {
        if (!isAnyActive()) return;
        float mult = getSlowed();
        mc.thePlayer.movementInput.moveForward *= mult;
        mc.thePlayer.movementInput.moveStrafe *= mult;
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent e) {
        if (setJump) {
            mc.thePlayer.movementInput.jump = true;
            setJump = false;
        }
    }

    /**
     * Whether the module is actively taking over the use-item slowdown.
     * Consumed by {@code MixinEntityPlayerSP#isUsing} to skip the vanilla slowdown.
     */
    public boolean isAnyActive() {
        return isEnabled()
                && mc.thePlayer != null
                && mc.thePlayer.isUsingItem()
                && getSlowed() != 0.2f;
    }

    /**
     * The fraction of horizontal movement to keep while using an item.
     * Matches Raven's getSlowed(): {@code (100 - slowed%) / 100}, or {@code 0.2}
     * (vanilla) when the module should not intervene.
     */
    public float getSlowed() {
        if (mc.thePlayer == null || !isEnabled() || mc.thePlayer.getHeldItem() == null) {
            return 0.2f;
        }
        if (swordOnly.getValue() && !(mc.thePlayer.getHeldItem().getItem() instanceof ItemSword)) {
            return 0.2f;
        }
        if (mc.thePlayer.getHeldItem().getItem() instanceof ItemBow && disableBow.getValue()) {
            return 0.2f;
        }
        if (mc.thePlayer.getHeldItem().getItem() instanceof ItemPotion
                && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage())
                && disablePotions.getValue()) {
            return 0.2f;
        }
        return (100.0F - slowed.getValue()) / 100.0F;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{mode.getModeString()};
    }
}
