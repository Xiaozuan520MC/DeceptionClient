package com.easycaikuai.deceptionclient.ui.clickgui.screen;

import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.module.Category;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUI;
import com.easycaikuai.deceptionclient.ui.clickgui.RiseClickGUIGlobal;
import com.easycaikuai.deceptionclient.ui.clickgui.component.ModuleComponent;
import com.easycaikuai.deceptionclient.util.TimerUtil;
import com.easycaikuai.deceptionclient.util.gui.ScrollUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

public class CategoryScreen {
    private final TimerUtil stopwatch = new TimerUtil();
    public ScrollUtil scrollUtil = new ScrollUtil();
    public ArrayList<ModuleComponent> relevantModules;
    public Category category;
    public double endOfList;
    public double startOfList;

    public CategoryScreen(Category category) {
        this.category = category;
    }

    public void onInit() {
        if (this.category == null) return;
        RiseClickGUI gui = RiseClickGUIGlobal.getGUI();
        if (gui == null) return;

        // Calculate max height
        double yOffset = 0;
        for (ModuleComponent component : this.relevantModules) {
            yOffset += component.height;
            yOffset += 6;
        }
        this.endOfList = yOffset;

        double moduleHeight = gui.scaleH - 30.0D;
        this.scrollUtil.setMax(Math.max(0, this.endOfList - moduleHeight));
    }

    public void onRender(int mouseX, int mouseY, float partialTicks) {
        if (this.category == null) return;
        RiseClickGUI gui = RiseClickGUIGlobal.getGUI();
        if (gui == null) return;

        if (this.relevantModules == null) {
            this.relevantModules = new ArrayList<>();
            for (Module mod : Deception.moduleManager.getModulesByCategory(this.category)) {
                if (!mod.getName().equals("ClickGui")) {
                    this.relevantModules.add(new ModuleComponent(mod));
                }
            }
            this.relevantModules.sort(Comparator.comparing(m -> m.module.getName().toLowerCase()));
            onInit();
            if (this.relevantModules == null) return;
        }

        this.scrollUtil.onRender();

        float moduleWidth = gui.scaleW - (float) gui.sidebar.sidebarWidth - 14.0F;
        float startX = gui.positionX + (float) gui.sidebar.sidebarWidth + 7.0F;
        float startY = gui.positionY + 12.0F + (float) this.scrollUtil.getScroll();

        double yOff = 0;
        for (ModuleComponent mc : this.relevantModules) {
            mc.x = startX;
            mc.y = startY + yOff;
            mc.width = moduleWidth;
            mc.render(gui, mouseX, mouseY, partialTicks);
            yOff += mc.height + 6.0D;
        }
    }

    public void click(int mouseX, int mouseY, int button) {
        if (this.relevantModules == null) return;
        for (ModuleComponent mc : this.relevantModules) {
            mc.click(mouseX, mouseY, button);
        }
    }

    public void released() {
        if (this.relevantModules == null) return;
        for (ModuleComponent mc : this.relevantModules) mc.released();
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (this.relevantModules == null) return;
        for (ModuleComponent mc : this.relevantModules) mc.keyTyped(typedChar, keyCode);
    }
}