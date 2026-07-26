package com.easycaikuai.deceptionclient.module.modules.misc;

import com.easycaikuai.deceptionclient.module.Module;

public class AntiObfuscate extends Module {
    public AntiObfuscate() {
        super("AntiObfuscate", false, true);
    }

    public String stripObfuscated(String input) {
        return input.replaceAll("§k", "");
    }
}
