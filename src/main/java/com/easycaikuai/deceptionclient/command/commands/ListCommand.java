package com.easycaikuai.deceptionclient.command.commands;

import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.command.Command;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class ListCommand extends Command {
    public ListCommand() {
        super(new ArrayList<>(Arrays.asList("list", "l", "modules", "deception")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (!Deception.moduleManager.modules.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sModules:&r", Deception.clientName));
            for (Module module : Deception.moduleManager.modules.values()) {
                ChatUtil.sendFormatted(String.format("%s»&r %s&r", module.isHidden() ? "&8" : "&7", module.formatModule()));
            }
        }
    }
}
