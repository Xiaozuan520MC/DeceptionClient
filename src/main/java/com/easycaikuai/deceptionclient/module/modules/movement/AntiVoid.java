package com.easycaikuai.deceptionclient.module.modules.movement;

import com.google.common.base.CaseFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.AxisAlignedBB;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.enums.BlinkModules;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.Priority;
import com.easycaikuai.deceptionclient.events.KeyEvent;
import com.easycaikuai.deceptionclient.events.PlayerUpdateEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;
import com.easycaikuai.deceptionclient.util.PlayerUtil;
import com.easycaikuai.deceptionclient.util.RandomUtil;

public class AntiVoid extends Module {
    public final PercentProperty chance = new PercentProperty("Chance", 100);
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty mode = new ModeProperty("Mode", 0, new String[]{"Blink"});
    public final FloatProperty distance = new FloatProperty("Distance", 5.0F, 0.0F, 16.0F);
    private boolean isInVoid = false;
    private boolean wasInVoid = false;
    private double[] lastSafePosition = null;

    public AntiVoid() {
        super("AntiVoid", false);
    }

    private void resetBlink() {
        Deception.blinkManager.setBlinkState(false, BlinkModules.ANTI_VOID);
        this.lastSafePosition = null;
    }

    private boolean canUseAntiVoid() {
        LongJump longJump = (LongJump) Deception.moduleManager.modules.get(LongJump.class);
        return !longJump.isJumping();
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(PlayerUpdateEvent event) {
        if (this.isEnabled()) {
            this.isInVoid = !mc.thePlayer.capabilities.allowFlying && PlayerUtil.isInWater();
            if (this.mode.getValue() == 0) {
                if (!this.isInVoid) {
                    this.resetBlink();
                }
                if (this.lastSafePosition != null) {
                    float subWidth = mc.thePlayer.width / 2.0F;
                    float height = mc.thePlayer.height;
                    if (PlayerUtil.checkInWater(
                            new AxisAlignedBB(
                                    this.lastSafePosition[0] - (double) subWidth,
                                    this.lastSafePosition[1],
                                    this.lastSafePosition[2] - (double) subWidth,
                                    this.lastSafePosition[0] + (double) subWidth,
                                    this.lastSafePosition[1] + (double) height,
                                    this.lastSafePosition[2] + (double) subWidth
                            )
                    )) {
                        this.resetBlink();
                    }
                }
                if (!this.wasInVoid && this.isInVoid && this.canUseAntiVoid()) {
                    Deception.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
                    if (Deception.blinkManager.setBlinkState(true, BlinkModules.ANTI_VOID)) {
                        this.lastSafePosition = new double[]{mc.thePlayer.prevPosX, mc.thePlayer.prevPosY, mc.thePlayer.prevPosZ};
                    }
                }
                if (Deception.blinkManager.getBlinkingModule() == BlinkModules.ANTI_VOID
                        && this.lastSafePosition != null
                        && this.lastSafePosition[1] - (double) this.distance.getValue().floatValue() > mc.thePlayer.posY) {
                    Deception.blinkManager
                            .blinkedPackets
                            .offerFirst(
                                    new C04PacketPlayerPosition(
                                            this.lastSafePosition[0], this.lastSafePosition[1] - RandomUtil.nextDouble(10.0, 20.0), this.lastSafePosition[2], false
                                    )
                            );
                    this.resetBlink();
                }
            }
            this.wasInVoid = this.isInVoid;
        }
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (event.getKey() == mc.gameSettings.keyBindUseItem.getKeyCode()) {
            ItemStack currentItem = mc.thePlayer.inventory.getCurrentItem();
            if (currentItem != null && currentItem.getItem() instanceof ItemEnderPearl) {
                this.resetBlink();
            }
        }
    }

    @Override
    public void onEnabled() {
        this.isInVoid = false;
        this.wasInVoid = false;
        this.resetBlink();
    }

    @Override
    public void onDisabled() {
        Deception.blinkManager.setBlinkState(false, BlinkModules.ANTI_VOID);
    }

    @Override
    public void verifyValue(String mode) {
        if (this.isEnabled()) {
            this.onDisabled();
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, this.mode.getModeString())};
    }
}
