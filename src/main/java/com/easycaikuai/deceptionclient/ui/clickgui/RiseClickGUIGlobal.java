package com.easycaikuai.deceptionclient.ui.clickgui;

public class RiseClickGUIGlobal {
    public static RiseClickGUI instance;

    public static RiseClickGUI getGUI() {
        if (instance == null) {
            instance = new RiseClickGUI();
        }
        return instance;
    }

    public static RiseClickGUI getStandardClickGUI() {
        return getGUI();
    }
}