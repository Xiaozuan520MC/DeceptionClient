package com.easycaikuai.deceptionclient.management;

import com.easycaikuai.deceptionclient.enums.ChatColors;

import java.awt.*;
import java.io.File;

public class FriendManager extends PlayerFileManager {
    public FriendManager() {
        super(new File("./config/Deception/", "friends.txt"), new Color(ChatColors.DARK_GREEN.toAwtColor()));
    }
}
