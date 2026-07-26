package com.easycaikuai.deceptionclient.module.modules.render;

import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.Render2DEvent;
import com.easycaikuai.deceptionclient.mixin.IShaderGroupAccessor;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class MotionBlur extends Module {
    public static final FloatProperty blurAmount = new FloatProperty("Blur Amount", 2.0F, 1.0F, 10.0F);
    private static final Minecraft mc = Minecraft.getMinecraft();

    public MotionBlur() {
        super("MotionBlur", false, false);
    }

    private Framebuffer blurBufferMain = null;
    private Framebuffer blurBufferInto = null;

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (mc.theWorld != null) {
            if (isEnabled()) {
                if (mc.entityRenderer.getShaderGroup() == null) {
                    mc.entityRenderer.loadShader(new ResourceLocation("minecraft", "shaders/post/motion_blur.json"));
                }
                float uniform = 1.0F - Math.min(blurAmount.getValue() / 10.0F, 0.9F);
                ShaderGroup shaderGroup = mc.entityRenderer.getShaderGroup();
                if (shaderGroup != null) {
                    IShaderGroupAccessor accessor = (IShaderGroupAccessor) shaderGroup;
                    List<Shader> shaders = accessor.getListShaders();
                    shaders.get(0).getShaderManager().getShaderUniform("Phosphor").set(uniform, 0.0F, 0.0F);
                }
            } else if (mc.entityRenderer.getShaderGroup() != null) {
                mc.entityRenderer.stopUseShader();
            }
        }
    }

    private static Framebuffer checkFramebufferSizes(Framebuffer framebuffer, int width, int height) {
        if (framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
            if (framebuffer == null) {
                framebuffer = new Framebuffer(width, height, true);
            } else {
                framebuffer.createBindFramebuffer(width, height);
            }
            framebuffer.setFramebufferFilter(GL11.GL_NEAREST);
        }
        return framebuffer;
    }

    public static void drawTexturedRectNoBlend(float x, float y, float width, float height, float uMin, float uMax, float vMin, float vMax, int filter) {
        GlStateManager.enableTexture2D();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, (y + height), 0.0D).tex(uMin, vMax).endVertex();
        worldrenderer.pos((x + width), (y + height), 0.0D).tex(uMax, vMax).endVertex();
        worldrenderer.pos((x + width), y, 0.0D).tex(uMax, vMin).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex(uMin, vMin).endVertex();
        tessellator.draw();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    }

    @SuppressWarnings("unused")
    public void onBlurScreen() {
        if (OpenGlHelper.isFramebufferEnabled()) {
            int width = mc.getFramebuffer().framebufferWidth;
            int height = mc.getFramebuffer().framebufferHeight;
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0D, width, height, 0.0D, 2000.0D, 4000.0D);
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();
            GlStateManager.translate(0.0F, 0.0F, -2000.0F);
            this.blurBufferMain = checkFramebufferSizes(this.blurBufferMain, width, height);
            this.blurBufferInto = checkFramebufferSizes(this.blurBufferInto, width, height);
            this.blurBufferInto.bindFramebuffer(false);
            this.blurBufferInto.framebufferClear();
            OpenGlHelper.glBlendFunc(770, 771, 0, 1);
            GlStateManager.disableLighting();
            GlStateManager.disableBlend();
            GlStateManager.depthMask(false);
            mc.getFramebuffer().bindFramebuffer(true);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexturedRectNoBlend(0.0F, 0.0F, width, height, 0.0F, 1.0F, 0.0F, 1.0F, GL11.GL_NEAREST);
            GlStateManager.enableBlend();
            this.blurBufferMain.bindFramebuffer(true);
            GlStateManager.color(1.0F, 1.0F, 1.0F, blurAmount.getValue() * 10.0F / 100.0F - 0.1F);
            drawTexturedRectNoBlend(0.0F, 0.0F, width, height, 0.0F, 1.0F, 1.0F, 0.0F, GL11.GL_NEAREST);
            mc.getFramebuffer().framebufferClear();
            this.blurBufferInto.bindFramebuffer(true);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableBlend();
            OpenGlHelper.glBlendFunc(770, 771, 1, 771);
            drawTexturedRectNoBlend(0.0F, 0.0F, width, height, 0.0F, 1.0F, 0.0F, 1.0F, GL11.GL_NEAREST);
            Framebuffer tempBuff = this.blurBufferMain;
            this.blurBufferMain = this.blurBufferInto;
            this.blurBufferInto = tempBuff;
        }
    }
}