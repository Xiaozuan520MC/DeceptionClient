package com.easycaikuai.deceptionclient.module.modules.combat;

import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.enums.BlinkModules;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.EventType;
import com.easycaikuai.deceptionclient.event.types.Priority;
import com.easycaikuai.deceptionclient.events.CancelUseEvent;
import com.easycaikuai.deceptionclient.events.PacketEvent;
import com.easycaikuai.deceptionclient.events.RightClickMouseEvent;
import com.easycaikuai.deceptionclient.events.UpdateEvent;
import com.easycaikuai.deceptionclient.mixin.IAccessorEntityPlayer;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.module.modules.misc.BedNuker;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;
import com.easycaikuai.deceptionclient.property.properties.IntProperty;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.util.ItemUtil;
import com.easycaikuai.deceptionclient.util.KeyBindUtil;
import com.easycaikuai.deceptionclient.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import org.lwjgl.input.Mouse;

/**
 * Ported from raven-bs Autoblock.
 *
 * <p>Adaptations for the DeceptionClient framework:
 * <ul>
 *   <li>Raven's outbound-only {@code LagRequest} / {@code ModuleBackedTimeout}
 *       (hold outbound packets, release on attack) maps to
 *       {@link com.easycaikuai.deceptionclient.management.BlinkManager} with
 *       {@link BlinkModules#AUTO_BLOCK}.</li>
 *   <li>{@code CombatTargeting.findTarget} is reimplemented by scanning
 *       {@code mc.theWorld.playerEntities} (distance / team / bot / friend filter).</li>
 *   <li>{@code ReflectionUtils.setItemInUse} maps to the
 *       {@link IAccessorEntityPlayer} mixin accessor (direct field write, no
 *       use-sound side effect); the forced block animation is applied in the
 *       pre-update tick rather than a render tick.</li>
 *   <li>{@code bedAura.isActivelyMining()} maps to {@code BedNuker.isReady()}.</li>
 *   <li>{@code DescriptionSetting} group labels are dropped (no equivalent).</li>
 * </ul>
 */
public class AutoBlock extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final FloatProperty range = new FloatProperty("Range", 4.0F, 2.0F, 6.0F);
    private final IntProperty maxHurtTimeMs = new IntProperty("Maximum Hurt Time", 200, 50, 500);
    private final IntProperty maxHoldMs = new IntProperty("Maximum Hold Time", 150, 50, 500);

    private final BooleanProperty requireLmb = new BooleanProperty("Require Left mouse", true);
    private final BooleanProperty requireRmb = new BooleanProperty("Require right mouse", false);
    private final BooleanProperty onlyWhenDamaged = new BooleanProperty("Damaged", false);
    private final BooleanProperty ignoreTeammates = new BooleanProperty("Ignore teammates", true);

    private final PercentProperty lagChance = new PercentProperty("Lag Chance", 100);
    private final IntProperty lagMaxDuration = new IntProperty("Lag Max Duration", 200, 50, 500);
    private final BooleanProperty preventDelayAttacks = new BooleanProperty("Prevent delaying attacks", true);
    private final BooleanProperty blockAgainImmediately = new BooleanProperty("Block again immediately", true);
    private final BooleanProperty forceBlockAnimation = new BooleanProperty("Force block animation", true);

    private boolean isBlocking;
    private boolean manualBlock;
    private int blockStartTick = -1;
    private EntityPlayer currentTarget;
    private int lastSelfHurtTime;

    private boolean isLagging;
    private int lagStartTick = -1;

    private int tickCounter;

    public AutoBlock() {
        super("AutoBlock", false);
    }

    @Override
    public void onEnabled() {
        tickCounter = 0;
        resetState(false);
    }

    @Override
    public void onDisabled() {
        resetState(true);
    }

    private static int msToTicks(double ms) {
        if (ms <= 0.0) return 0;
        return (int) Math.ceil(ms / 50.0);
    }

    @EventTarget(Priority.HIGHEST)
    public void onRightClick(RightClickMouseEvent e) {
        // Replaces Raven's onMouse (take over right-click while holding a sword)
        // and onRightClickMouse (suppress vanilla use while lagging — a subset,
        // since lagging implies holding a sword here). Lag-period use-item
        // suppression is additionally handled by onCancelUse.
        if (!nullCheck() || !ItemUtil.isHoldingSword() || bedNukerActive()) return;
        e.setCancelled(true);
    }

    @EventTarget(Priority.HIGHEST)
    public void onCancelUse(CancelUseEvent e) {
        // Replaces Raven's onUseItem.
        if (shouldBlockVanillaUse()) {
            e.setCancelled(true);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent e) {
        if (e.getType() != EventType.SEND) return;
        if (bedNukerActive()) {
            releaseLag();
            return;
        }
        if (!isLagging || !preventDelayAttacks.getValue()) return;
        if (!(e.getPacket() instanceof C02PacketUseEntity)) return;
        if (((C02PacketUseEntity) e.getPacket()).getAction() != C02PacketUseEntity.Action.ATTACK) return;

        releaseLag();
        if (blockAgainImmediately.getValue() && ItemUtil.isHoldingSword()) {
            startBlocking(tickCounter);
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent e) {
        if (e.getType() != EventType.PRE) return;
        if (!isEnabled()) return;
        if (!nullCheck() || mc.thePlayer.isDead || mc.currentScreen != null) {
            resetState(true);
            return;
        }
        if (bedNukerActive()) {
            resetState(true);
            return;
        }

        int selfHurtTime = mc.thePlayer.hurtTime;
        boolean hurtAgain = selfHurtTime > lastSelfHurtTime;
        lastSelfHurtTime = selfHurtTime;

        if (!ItemUtil.isHoldingSword()) {
            resetState(false);
            return;
        }

        tickCounter++;
        int currentTick = tickCounter;

        currentTarget = findTarget();
        KillAura killAura = (KillAura) Deception.moduleManager.getModule(KillAura.class);
        boolean killAuraAttacking = killAura != null && killAura.isEnabled()
                && !killAura.requirePress.getValue() && currentTarget != null;
        boolean rmbDown = Mouse.isButtonDown(1);
        boolean lmbDown = Mouse.isButtonDown(0) || killAuraAttacking;

        if (!rmbDown) {
            resetState(true);
            return;
        }

        if (!lmbDown) {
            if (isLagging) releaseLag();
            if (!isBlocking) {
                startBlocking(currentTick);
                manualBlock = true;
            }
            applyForceAnimation();
            return;
        }

        if (manualBlock) {
            stopBlocking(true);
            manualBlock = false;
        }

        boolean hasTarget = currentTarget != null;
        boolean conditionsMet = hasTarget && checkConditions(lmbDown, rmbDown);

        if (isLagging) {
            int lagMaxTicks = msToTicks(lagMaxDuration.getValue());
            boolean lagExpired = lagMaxTicks > 0 && lagStartTick >= 0 && currentTick - lagStartTick >= lagMaxTicks;

            if (lagExpired || !conditionsMet) {
                releaseLag();
                if (lagExpired && blockAgainImmediately.getValue() && conditionsMet) {
                    startBlocking(currentTick);
                }
            }
        }

        if (!conditionsMet) {
            stopBlocking(true);
            applyForceAnimation();
            return;
        }

        if (!isBlocking && !isLagging) {
            boolean shouldStart = onlyWhenDamaged.getValue() ? shouldPredictiveBlock() : true;
            if (shouldStart) {
                startBlocking(currentTick);
            }
        }

        if (isBlocking) {
            int maxHoldTicks = msToTicks(maxHoldMs.getValue());
            boolean timeExpired = maxHoldTicks > 0 && blockStartTick >= 0 && currentTick - blockStartTick >= maxHoldTicks;
            boolean shouldStop = timeExpired;
            if (onlyWhenDamaged.getValue() && hurtAgain) {
                shouldStop = true;
            }
            if (shouldStop) {
                if (shouldStartLag()) {
                    startLag(currentTick);
                }
                stopBlocking(true);
            }
        }

        applyForceAnimation();
    }

    private void applyForceAnimation() {
        if (forceBlockAnimation.getValue() && ItemUtil.isHoldingSword()) {
            setItemInUse(isBlocking || isLagging);
        }
    }

    private boolean checkConditions(boolean lmbDown, boolean rmbDown) {
        if (requireLmb.getValue() && !lmbDown) return false;
        if (requireRmb.getValue() && !rmbDown) return false;
        return true;
    }

    private boolean shouldPredictiveBlock() {
        int ourHurtTime = mc.thePlayer.hurtTime;
        int triggerTick = (int) Math.round(maxHurtTimeMs.getValue() / 50.0);
        triggerTick = Math.max(1, Math.min(10, triggerTick));
        return ourHurtTime == triggerTick;
    }

    private boolean shouldBlockVanillaUse() {
        return isEnabled() && isLagging && nullCheck() && ItemUtil.isHoldingSword() && mc.currentScreen == null;
    }

    private void startBlocking(int currentTick) {
        if (!ItemUtil.isHoldingSword()) return;
        int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBindUtil.setKeyBindState(keyCode, true);
        KeyBindUtil.pressKeyOnce(keyCode);
        isBlocking = true;
        blockStartTick = currentTick;
    }

    private void stopBlocking(boolean forceRelease) {
        if (!isBlocking && !forceRelease) return;
        int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBindUtil.setKeyBindState(keyCode, false);
        isBlocking = false;
        blockStartTick = -1;
    }

    private boolean shouldStartLag() {
        double chance = lagChance.getValue();
        if (chance <= 0) return false;
        if (chance >= 100) return true;
        return Math.random() * 100 < chance;
    }

    private void startLag(int currentTick) {
        if (isLagging) return;
        int lagReferenceTick = blockStartTick >= 0 ? blockStartTick : currentTick;
        int lagMaxTicks = msToTicks(lagMaxDuration.getValue());
        if (lagMaxTicks > 0 && currentTick - lagReferenceTick >= lagMaxTicks) {
            return;
        }
        Deception.blinkManager.setBlinkState(true, BlinkModules.AUTO_BLOCK);
        isLagging = true;
        lagStartTick = lagReferenceTick;
    }

    private void releaseLag() {
        if (!isLagging) return;
        Deception.blinkManager.setBlinkState(false, BlinkModules.AUTO_BLOCK);
        isLagging = false;
        lagStartTick = -1;
    }

    public boolean isActive() {
        return isEnabled() && (isBlocking || isLagging);
    }

    private void setItemInUse(boolean state) {
        if (mc.thePlayer == null) return;
        IAccessorEntityPlayer acc = (IAccessorEntityPlayer) mc.thePlayer;
        if (state && mc.thePlayer.getHeldItem() != null) {
            acc.setItemInUse(mc.thePlayer.getHeldItem());
            acc.setItemInUseCount(mc.thePlayer.getHeldItem().getMaxItemUseDuration());
        } else {
            acc.setItemInUse(null);
            acc.setItemInUseCount(0);
        }
    }

    private EntityPlayer findTarget() {
        double maxRange = range.getValue();
        EntityPlayer best = null;
        double bestDist = maxRange;
        for (EntityPlayer ep : mc.theWorld.playerEntities) {
            if (ep == mc.thePlayer || ep.isDead || ep.getHealth() <= 0.0F) continue;
            if (ignoreTeammates.getValue() && TeamUtil.isSameTeam(ep)) continue;
            if (TeamUtil.isBot(ep)) continue;
            if (TeamUtil.isFriend(ep)) continue;
            double d = mc.thePlayer.getDistanceToEntity(ep);
            if (d <= bestDist) {
                bestDist = d;
                best = ep;
            }
        }
        return best;
    }

    private boolean nullCheck() {
        return mc.thePlayer != null && mc.theWorld != null;
    }

    private boolean bedNukerActive() {
        BedNuker bedNuker = (BedNuker) Deception.moduleManager.getModule(BedNuker.class);
        return bedNuker != null && bedNuker.isEnabled() && bedNuker.isReady();
    }

    private void resetState(boolean releaseUseKey) {
        boolean wasActive = isBlocking || isLagging;
        releaseLag();
        stopBlocking(releaseUseKey);
        manualBlock = false;
        if (forceBlockAnimation.getValue() && wasActive) {
            setItemInUse(false);
        }
        if (Mouse.isButtonDown(1) && mc.currentScreen == null) {
            KeyBindUtil.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
        }
        currentTarget = null;
        lastSelfHurtTime = 0;
    }
}
