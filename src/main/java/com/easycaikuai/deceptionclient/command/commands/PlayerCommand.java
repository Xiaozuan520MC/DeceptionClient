package com.easycaikuai.deceptionclient.command.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.command.Command;
import com.easycaikuai.deceptionclient.enums.ChatColors;
import com.easycaikuai.deceptionclient.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class PlayerCommand extends Command {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public PlayerCommand() {
        super(new ArrayList<>(Arrays.asList("playerlist", "players")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        ArrayList<String> players = new ArrayList<>();
        for (NetworkPlayerInfo playerInfo : mc.getNetHandler().getPlayerInfoMap()) {
            players.add(playerInfo.getGameProfile().getName().replace("§", "&"));
        }
        if (players.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sNo players&r", Deception.clientName));
        } else {
            ChatUtil.sendRaw(
                    String.format(
                            ChatColors.formatColor("%sPlayers:&r %s"),
                            ChatColors.formatColor(Deception.clientName),
                            String.join(", ", players)
                    )
            );
        }
    }
}
