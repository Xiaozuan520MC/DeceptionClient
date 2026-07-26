package com.easycaikuai.deceptionclient.ui;

import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.config.Config;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigGui extends GuiScreen {

    private String name = "default";
    private boolean editing = false;
    private final List<String> saved = new ArrayList<>();
    private static final File DIR = new File("./config/Unfair/");

    private void refresh() {
        saved.clear();
        if (DIR.exists()) {
            File[] f = DIR.listFiles((d, n) -> n.endsWith(".json"));
            if (f != null) for (File x : f) saved.add(x.getName().replace(".json", ""));
        }
    }

    @Override
    public void initGui() { refresh(); }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        RoundedUtils.drawRound(20, 20, 220, 300, 10, new Color(10, 14, 24, 230));
        Deception.fontManager.s16.drawString("Config Manager", 28, 28, 0xFFAF52DE);

        // Input
        float y = 54;
        boolean hi = mx >= 26 && mx <= 26 + 160 && my >= y && my <= y + 22;
        RoundedUtils.drawRound(26, y, 160, 22, 6, new Color(hi || editing ? 44 : 24, 28, 48));
        String nd = editing ? name + (System.currentTimeMillis() % 800 > 400 ? "|" : "") : (name.isEmpty() ? "Name..." : name);
        Deception.fontManager.s12.drawString(nd, 32, y + (22 - Deception.fontManager.s12.getHeight()) / 2f + 1, editing ? 0xFFF0F2F5 : 0xFF636366);

        boolean hs = mx >= 190 && mx <= 232 && my >= y + 1 && my <= y + 21;
        RoundedUtils.drawRound(190, y + 1, 42, 20, 5, new Color(hs ? 175 : 40, hs ? 82 : 44, hs ? 222 : 62));
        Deception.fontManager.s12.drawString("Save", 190 + (42 - Deception.fontManager.s12.getStringWidth("Save")) / 2f, y + (20 - Deception.fontManager.s12.getHeight()) / 2f + 1, 0xFFFFFFFF);
        y += 30;

        Deception.fontManager.s12.drawString("Saved:", 26, y, 0xFF636366);
        y += 16;

        if (saved.isEmpty()) { Deception.fontManager.s12.drawString("No saved configs", 28, y + 6, 0xFF636366); return; }

        for (String c : saved) {
            boolean h2 = mx >= 24 && mx <= 216 && my >= y && my <= y + 20;
            boolean a = c.equals(name);
            RoundedUtils.drawRound(24, y, 192, 20, 5, new Color(a ? 30 : 14, a ? 36 : 18, a ? 56 : 34));
            Deception.fontManager.s12.drawString((a ? "> " : "  ") + c, 30, y + (20 - Deception.fontManager.s12.getHeight()) / 2f + 1, a ? 0xFFAF52DE : (h2 ? 0xFFF0F2F5 : 0xFF8A8F9D));
            // X button to delete
            boolean hx = mx >= 218 && mx <= 234 && my >= y && my <= y + 20;
            Deception.fontManager.s12.drawString("X", 222, y + (20 - Deception.fontManager.s12.getHeight()) / 2f + 1, hx ? 0xFFFF453A : 0xFF636366);
            y += 22;
        }
    }

    @Override protected void mouseClicked(int mx, int my, int btn) throws IOException {
        float y = 54;
        if (mx >= 26 && mx <= 186 && my >= y && my <= y + 22) { editing = true; return; }
        if (mx >= 190 && mx <= 232 && my >= y + 1 && my <= y + 21) {
            editing = false; if (!name.isEmpty()) { new Config(name, false).save(); refresh(); } return;
        }
        y += 30 + 16;
        for (String c : saved) {
            if (mx >= 24 && mx <= 216 && my >= y && my <= y + 20) { name = c; new Config(c, false).load(); refresh(); return; }
            y += 22;
        }
    }

    @Override protected void keyTyped(char c, int k) throws IOException {
        if (editing) {
            if (k == Keyboard.KEY_ESCAPE || k == Keyboard.KEY_RETURN) { editing = false; return; }
            if (k == Keyboard.KEY_BACK || k == Keyboard.KEY_DELETE) { if (!name.isEmpty()) name = name.substring(0, name.length() - 1); return; }
            if (c >= 32 && c < 127 && name.length() < 25) name += c; return;
        }
        if (k == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(null); return; }
    }

    @Override public boolean doesGuiPauseGame() { return true; }
}
