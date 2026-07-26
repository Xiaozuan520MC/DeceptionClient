package com.easycaikuai.deceptionclient.module.modules.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.AttackEvent;
import com.easycaikuai.deceptionclient.events.LoadWorldEvent;
import com.easycaikuai.deceptionclient.events.UpdateEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;
import com.easycaikuai.deceptionclient.util.SoundUtil;

public class KillSound extends Module {
    private static final String[] SOUNDS = {"Zako", "Zhang Xue Feng", "FAHHHH"};

    public final ModeProperty audio = new ModeProperty("Audio", 0, SOUNDS);

    private EntityLivingBase target;
    private boolean played;

    public KillSound() {
        super("KillSound", false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{audio.getModeString()};
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!isEnabled()) return;
        Entity entity = event.getTarget();
        if (entity instanceof EntityLivingBase) {
            target = (EntityLivingBase) entity;
            played = false;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || target == null || played) return;
        if (target.isDead || target.getHealth() <= 0.0f) {
            String soundName = SOUNDS[audio.getValue()];
            SoundUtil.playSound(new ResourceLocation("minecraft", "deceptionclient/sounds/" + soundName).toString());
            played = true;
            target = null;
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        reset();
    }

    @Override
    public void onDisabled() {
        reset();
    }

    private void reset() {
        target = null;
        played = false;
    }
}