package com.easycaikuai.deceptionclient.management;

import com.easycaikuai.deceptionclient.enums.ChatColors;

import java.awt.*;
import java.io.File;

public class TargetManager extends PlayerFileManager {
    public TargetManager() {
        super(new File("./config/Deception/", "enemies.txt"), new Color(ChatColors.DARK_RED.toAwtColor()));
    }
}
