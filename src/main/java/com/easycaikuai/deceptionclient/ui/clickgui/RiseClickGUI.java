package com.easycaikuai.deceptionclient.ui.clickgui;

import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.gui.font.FontRenderer;
import com.easycaikuai.deceptionclient.gui.font.Fonts;
import com.easycaikuai.deceptionclient.module.Category;
import com.easycaikuai.deceptionclient.module.modules.render.HUD;
import com.easycaikuai.deceptionclient.ui.clickgui.component.ModuleComponent;
import com.easycaikuai.deceptionclient.ui.clickgui.component.SidebarCategory;
import com.easycaikuai.deceptionclient.ui.clickgui.screen.CategoryScreen;
import com.easycaikuai.deceptionclient.util.RenderUtil;
import com.easycaikuai.deceptionclient.util.TimerUtil;
import com.easycaikuai.deceptionclient.util.animation.Easing;
import com.easycaikuai.deceptionclient.util.animation.RiseAnim;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RiseClickGUI extends GuiScreen {
    public float positionX;
    public float positionY;
    public float scaleW = 400.0F;
    public float scaleH = 300.0F;

    // RiseClickGUI 深色风格配色 — 主题色默认紫
    public Color backgroundColor = new Color(30, 30, 35, 235);
    public Color sidebarColor = new Color(25, 25, 30, 235);
    public Color logoColor = new Color(235, 235, 240);
    public Color fontColor = new Color(235, 235, 240);
    public Color fontDarkColor = new Color(235, 235, 240, 220);
    public Color fontDarkerColor = new Color(235, 235, 240, 40);
    public Color moduleBgColor = new Color(35, 35, 42, 180);
    public Color separatorColor = new Color(255, 255, 255, 25);

    public Color getAccentColor() {
        HUD hud = (HUD) Deception.moduleManager.modules.get(HUD.class);
        if (hud != null && hud.isEnabled()) {
            return hud.getColor(System.currentTimeMillis());
        }
        return new Color(175, 82, 222); // 紫色主题
    }

    public Color getAccentColor(int alpha) {
        Color c = getAccentColor();
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    public SidebarCategory sidebar = new SidebarCategory();
    public CategoryScreen selectedScreen;
    private Category selectedCategory = Category.COMBAT;
    public float draggingOffsetX;
    public float draggingOffsetY;
    public boolean dragging;
    public TimerUtil timeInCategory = new TimerUtil();
    public TimerUtil stopwatch = new TimerUtil();
    public ConcurrentLinkedQueue<ModuleComponent> moduleList = new ConcurrentLinkedQueue<>();
    public float lastMouseX;
    public float lastMouseY;
    public double animationTime;
    public double opacity;
    public int round = 7;
    public float moduleDefaultScaleY = 38.0F;
    public RiseAnim scaleAnimation = new RiseAnim(Easing.EASE_OUT_EXPO, 300L);
    public RiseAnim opacityAnimation = new RiseAnim(Easing.EASE_OUT_EXPO, 300L);

    public FontRenderer nunitoSmall = Fonts.interRegular.get(15.0F);
    public FontRenderer nunitoLarge = Fonts.interRegular.get(20.0F);
    public FontRenderer iconFont = Fonts.icon1.get(17.0F);
    public FontRenderer productSans = Fonts.interBold.get(32.0F);
    public FontRenderer productSansSmall = Fonts.interBold.get(16.0F);

    public RiseClickGUI() {
        this.selectedScreen = null;
    }

    public Category getSelectedCategory() {
        return this.selectedCategory;
    }

    public void switchScreen(Category category) {
        this.selectedCategory = category;
        this.selectedScreen = new CategoryScreen(category);
        this.timeInCategory.reset();
    }

    @Override
    public void onGuiClosed() {
        RiseClickGUIGlobal.instance = null;
    }

    @Override
    public void initGui() {
        super.initGui();
        ScaledResolution sr = new ScaledResolution(mc);
        this.positionX = (sr.getScaledWidth() - this.scaleW) / 2.0F;
        this.positionY = (sr.getScaledHeight() - this.scaleH) / 2.0F;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        ScaledResolution sr = new ScaledResolution(mc);
        animationTime += 0.5D * (RenderUtil.deltaTime() * 0.5D);
        if (animationTime > 1.0D) animationTime = 1.0D;

        opacityAnimation.run(1.0D);
        scaleAnimation.run(1.0D);
        opacity = opacityAnimation.getValue();
        double scale = scaleAnimation.getValue();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Draw screen background (overlay)
        drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), new Color(0, 0, 0, (int) (80 * opacity)).getRGB());

        // Scale and position
        float centerX = sr.getScaledWidth() / 2.0F;
        float centerY = sr.getScaledHeight() / 2.0F;
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(scale, scale, 1.0F);
        GlStateManager.translate(-centerX, -centerY, 0);

        // Drag handling
        if (dragging) {
            this.positionX = mouseX - this.draggingOffsetX;
            this.positionY = mouseY - this.draggingOffsetY;
        }
        if (this.positionX + this.scaleW > sr.getScaledWidth()) this.positionX = sr.getScaledWidth() - this.scaleW;
        if (this.positionX < 0) this.positionX = 0;
        if (this.positionY + this.scaleH > sr.getScaledHeight()) this.positionY = sr.getScaledHeight() - this.scaleH;
        if (this.positionY < 0) this.positionY = 0;

        // Main background
        drawRoundRect(this.positionX, this.positionY, this.scaleW, this.scaleH, this.round, new Color(this.backgroundColor.getRed(), this.backgroundColor.getGreen(), this.backgroundColor.getBlue(), (int) (this.backgroundColor.getAlpha() * opacity)));

        // Sidebar
        sidebar.updateOpacity((int) (255 * opacity));
        sidebar.preRenderClickGUI(this);

        // Category screen
        if (this.selectedScreen != null) {
            this.selectedScreen.onRender(mouseX, mouseY, partialTicks);
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            if (mouseX >= this.positionX && mouseX <= this.positionX + 20.0F && mouseY >= this.positionY && mouseY <= this.positionY + 15.0F) {
                this.dragging = true;
                this.draggingOffsetX = mouseX - this.positionX;
                this.draggingOffsetY = mouseY - this.positionY;
            }
        }
        sidebar.click(this, mouseX, mouseY, mouseButton);
        if (this.selectedScreen != null) {
            this.selectedScreen.click(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        this.dragging = false;
        if (this.selectedScreen != null) this.selectedScreen.released();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (this.selectedScreen != null) this.selectedScreen.keyTyped(typedChar, keyCode);
    }

    private void drawRoundRect(float x, float y, float w, float h, int radius, Color color) {
        com.easycaikuai.deceptionclient.util.shader.RoundedUtils.drawRound(x, y, w, h, radius, color);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}