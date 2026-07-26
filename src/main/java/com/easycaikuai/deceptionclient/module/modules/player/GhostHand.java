package com.easycaikuai.deceptionclient.module.modules.player;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.PercentProperty;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.util.ItemUtil;
import com.easycaikuai.deceptionclient.util.TeamUtil;

public class GhostHand extends Module {
    public final PercentProperty chance = new PercentProperty("Chance", 100);
    public final BooleanProperty teamsOnly = new BooleanProperty("Team Only", true);
    public final BooleanProperty ignoreWeapons = new BooleanProperty("Ignore Weapons", false);

    public GhostHand() {
        super("GhostHand", false);
    }

    public boolean shouldSkip(Entity entity) {
        return entity instanceof EntityPlayer
                && !TeamUtil.isBot((EntityPlayer) entity)
                && (!this.teamsOnly.getValue() || TeamUtil.isSameTeam((EntityPlayer) entity))
                && (!this.ignoreWeapons.getValue() || !ItemUtil.hasRawUnbreakingEnchant());
    }
}
