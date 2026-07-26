package com.easycaikuai.deceptionclient.module.modules.render;

import com.easycaikuai.deceptionclient.Deception;
import com.easycaikuai.deceptionclient.enums.ChatColors;
import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.event.types.Priority;
import com.easycaikuai.deceptionclient.events.Render2DEvent;
import com.easycaikuai.deceptionclient.events.Render3DEvent;
import com.easycaikuai.deceptionclient.events.ResizeEvent;
import com.easycaikuai.deceptionclient.mixin.IAccessorEntityRenderer;
import com.easycaikuai.deceptionclient.mixin.IAccessorRenderManager;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;
import com.easycaikuai.deceptionclient.util.ColorUtil;
import com.easycaikuai.deceptionclient.util.RenderUtil;
import com.easycaikuai.deceptionclient.util.TeamUtil;
import com.easycaikuai.deceptionclient.util.shader.GlowShader;
import com.easycaikuai.deceptionclient.util.shader.OutlineShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.player.EntityPlayer;

import javax.vecmath.Vector4d;
import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

public class ESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty mode = new ModeProperty("Mode", 2, new String[]{"None", "2d", "3d", "Outline", "FakeCorner", "Fake2d"});
    public final ModeProperty color = new ModeProperty("Color", 0, new String[]{"Default", "Teams", "Hud"});
    public final ModeProperty healthBar = new ModeProperty("Health Bar", 0, new String[]{"None", "2d", "Raven"});
    public final BooleanProperty players = new BooleanProperty("Players", true);
    public final BooleanProperty friends = new BooleanProperty("Friends", true);
    public final BooleanProperty enemies = new BooleanProperty("Enemies", true);
    public final BooleanProperty self = new BooleanProperty("Self", false);
    public final BooleanProperty bots = new BooleanProperty("Bots", false);
    public final BooleanProperty skeleton = new BooleanProperty("Skeleton", false);
    private final OutlineShader outlineRenderer = new OutlineShader();
    private final GlowShader glowShader = new GlowShader();
    private Framebuffer framebuffer = null;
    private boolean outline = true;
    private boolean glow = true;

    public ESP() {
        super("ESP", false, true);
    }

    private boolean shouldRenderPlayer(EntityPlayer entityPlayer) {
        if (entityPlayer.deathTime > 0) {
            return false;
        } else if (mc.getRenderViewEntity().getDistanceToEntity(entityPlayer) > 512.0F) {
            return false;
        } else if (!entityPlayer.ignoreFrustumCheck && !RenderUtil.isInViewFrustum(entityPlayer.getEntityBoundingBox(), 0.1F)) {
            return false;
        } else if (entityPlayer != mc.thePlayer && entityPlayer != mc.getRenderViewEntity()) {
            if (TeamUtil.isBot(entityPlayer)) {
                return this.bots.getValue();
            } else if (TeamUtil.isFriend(entityPlayer)) {
                return this.friends.getValue();
            } else {
                return TeamUtil.isTarget(entityPlayer) ? this.enemies.getValue() : this.players.getValue();
            }
        } else {
            return this.self.getValue() && mc.gameSettings.thirdPersonView != 0;
        }
    }

    private Color getEntityColor(EntityPlayer entityPlayer) {
        if (TeamUtil.isFriend(entityPlayer)) {
            return Deception.friendManager.getColor();
        } else if (TeamUtil.isTarget(entityPlayer)) {
            return Deception.targetManager.getColor();
        } else {
            switch (this.color.getValue()) {
                case 0:
                    return TeamUtil.getTeamColor(entityPlayer, 1.0F);
                case 1:
                    int teamColor = TeamUtil.isSameTeam(entityPlayer) ? ChatColors.BLUE.toAwtColor() : ChatColors.RED.toAwtColor();
                    return new Color(teamColor);
                case 2:
                    int hudColor = ((HUD) Deception.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                    return new Color(hudColor);
                default:
                    return new Color(-1);
            }
        }
    }

    public boolean isOutlineEnabled() {
        return this.outline;
    }

    public boolean isGlowEnabled() {
        return this.glow;
    }

    @EventTarget
    public void onResize(ResizeEvent event) {
        if (this.framebuffer != null) {
            this.framebuffer.deleteFramebuffer();
        }
        this.framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
    }

    @EventTarget(Priority.HIGH)
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled()) return;

        List<EntityPlayer> renderedEntities = TeamUtil.getLoadedEntitiesSorted().stream()
                .filter(entity -> entity instanceof EntityPlayer && this.shouldRenderPlayer((EntityPlayer) entity))
                .map(EntityPlayer.class::cast)
                .collect(Collectors.toList());

        if (renderedEntities.isEmpty()) return;

        // Outline mode (mode 3) - Glow + Outline shader
        if (this.mode.getValue() == 3) {
            GlStateManager.pushMatrix();
            GlStateManager.pushAttrib();
            if (this.framebuffer == null) {
                this.framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, false);
            }
            this.framebuffer.bindFramebuffer(false);
            ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(event.getPartialTicks(), 0);
            boolean shadow = mc.gameSettings.entityShadows;
            mc.gameSettings.entityShadows = false;
            this.outline = false;
            this.glow = false;
            this.glowShader.use();
            for (EntityPlayer player : renderedEntities) {
                Color entityColor = this.getEntityColor(player);
                this.glowShader.W(entityColor);
                boolean invisible = player.isInvisible();
                player.setInvisible(false);
                mc.getRenderManager().renderEntityStatic(player, event.getPartialTicks(), true);
                player.setInvisible(invisible);
            }
            this.glowShader.stop();
            this.glow = true;
            this.outline = true;
            mc.gameSettings.entityShadows = shadow;
            mc.entityRenderer.disableLightmap();
            mc.entityRenderer.setupOverlayRendering();
            mc.getFramebuffer().bindFramebuffer(false);
            this.outlineRenderer.use();
            RenderUtil.drawFramebuffer(this.framebuffer);
            this.outlineRenderer.stop();
            this.framebuffer.framebufferClear();
            mc.getFramebuffer().bindFramebuffer(false);
            GlStateManager.popAttrib();
            GlStateManager.popMatrix();
        }

        // 2D mode (mode 1) + 2D Health Bar (mode 1)
        if (this.mode.getValue() == 1 || this.healthBar.getValue() == 1) {
            RenderUtil.enableRenderState();
            double scaleFactor = new ScaledResolution(mc).getScaleFactor();
            double scale = scaleFactor / Math.pow(scaleFactor, 2.0);
            GlStateManager.pushMatrix();
            GlStateManager.scale(scale, scale, scale);
            for (EntityPlayer player : renderedEntities) {
                ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(event.getPartialTicks(), 0);
                Vector4d screenPosition = RenderUtil.projectToScreen(player, scaleFactor);
                mc.entityRenderer.setupOverlayRendering();
                if (screenPosition != null) {
                    float x = (float) screenPosition.x;
                    float y = (float) screenPosition.y;
                    float z = (float) screenPosition.z;
                    float w = (float) screenPosition.w;

                    if (this.mode.getValue() == 1) {
                        int playerColor = this.getEntityColor(player).getRGB();
                        // Outer glow (slightly darker/wider)
                        int glowAlpha = (playerColor >> 24) & 0xFF;
                        int glowR = ((playerColor >> 16) & 0xFF) * 2 / 3;
                        int glowG = ((playerColor >> 8) & 0xFF) * 2 / 3;
                        int glowB = (playerColor & 0xFF) * 2 / 3;
                        int glowColor = (Math.max(glowAlpha - 80, 0) << 24) | (glowR << 16) | (glowG << 8) | glowB;
                        RenderUtil.drawESPBox2D(x, y, z, w, 3.0F, glowColor);
                        // Inner outline
                        RenderUtil.drawESPBox2D(x, y, z, w, 1.5F, playerColor);
                    }

                    if (this.healthBar.getValue() == 1) {
                        float heal = player.getHealth() + player.getAbsorptionAmount();
                        float percent = Math.min(Math.max(heal / player.getMaxHealth(), 0.0F), 1.0F);
                        float box = (z - x) * 0.08F;
                        Color healthColor = ColorUtil.getHealthBlend(percent);
                        RenderUtil.drawLine(x - box, y, x - box, w, 3.0F, ColorUtil.darker(healthColor, 0.2F).getRGB());
                        RenderUtil.drawLine(x - box, w, x - box, w + (y - w) * percent, 1.5F, healthColor.getRGB());
                    }
                }
            }
            GlStateManager.popMatrix();
            RenderUtil.disableRenderState();
        }
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled()) return;

        List<EntityPlayer> players = TeamUtil.getLoadedEntitiesSorted().stream()
                .filter(entity -> entity instanceof EntityPlayer && this.shouldRenderPlayer((EntityPlayer) entity))
                .map(EntityPlayer.class::cast)
                .collect(Collectors.toList());

        if (players.isEmpty()) return;

        RenderUtil.enableRenderState();
        for (EntityPlayer player : players) {
            if (player.deathTime > 0 || player.isInvisible()) continue;

            // 3D bounding box (mode 2) - Colored
            if (this.mode.getValue() == 2) {
                Color entityColor = this.getEntityColor(player);
                RenderUtil.drawEntityBoundingBox(player, entityColor.getRed(), entityColor.getGreen(), entityColor.getBlue(), entityColor.getAlpha(), 1.5F, 0.1F);
                GlStateManager.resetColor();
            }

            // FakeCorner (mode 4)
            if (this.mode.getValue() == 4) {
                Color entityColor = this.getEntityColor(player);
                RenderUtil.drawCornerESP(player, entityColor.getRed() / 255.0F, entityColor.getGreen() / 255.0F, entityColor.getBlue() / 255.0F);
            }

            // Fake2d (mode 5)
            if (this.mode.getValue() == 5) {
                Color entityColor = this.getEntityColor(player);
                RenderUtil.drawFake2DESP(player, entityColor.getRed() / 255.0F, entityColor.getGreen() / 255.0F, entityColor.getBlue() / 255.0F);
            }

            // Skeleton
            if (this.skeleton.getValue()) {
                renderSkeleton(player, event.getPartialTicks());
            }

            // 3D Raven health bar (mode 2)
            if (this.healthBar.getValue() == 2) {
                double hx = RenderUtil.lerpDouble(player.posX, player.lastTickPosX, event.getPartialTicks())
                        - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
                double hy = RenderUtil.lerpDouble(player.posY, player.lastTickPosY, event.getPartialTicks())
                        - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()
                        - 0.1F;
                double hz = RenderUtil.lerpDouble(player.posZ, player.lastTickPosZ, event.getPartialTicks())
                        - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
                GlStateManager.pushMatrix();
                GlStateManager.translate(hx, hy, hz);
                GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0F, 0.0F, 1.0F, 0.0F);
                float heal = player.getHealth() + player.getAbsorptionAmount();
                float percent = Math.min(Math.max(heal / player.getMaxHealth(), 0.0F), 1.0F);
                Color healthColor = ColorUtil.getHealthBlend(percent);
                float height = player.height + 0.2F;
                RenderUtil.drawRect3D(0.57250005F, -0.027500002F, 0.7275F, height + 0.027500002F, Color.black.getRGB());
                RenderUtil.drawRect3D(0.6F, 0.0F, 0.70000005F, height, Color.darkGray.getRGB());
                RenderUtil.drawRect3D(0.6F, 0.0F, 0.70000005F, height * percent, healthColor.getRGB());
                GlStateManager.popMatrix();
            }
        }
        RenderUtil.disableRenderState();
    }

    private void renderSkeleton(EntityPlayer player, float partialTicks) {
        double x = RenderUtil.lerpDouble(player.posX, player.lastTickPosX, partialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
        double y = RenderUtil.lerpDouble(player.posY, player.lastTickPosY, partialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
        double z = RenderUtil.lerpDouble(player.posZ, player.lastTickPosZ, partialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.color(1, 1, 1, 0.7F);
        org.lwjgl.opengl.GL11.glLineWidth(2);
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINE_STRIP);
        org.lwjgl.opengl.GL11.glVertex3d(0, 0, 0);
        org.lwjgl.opengl.GL11.glVertex3d(0, 1.5, 0);
        org.lwjgl.opengl.GL11.glEnd();
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINE_STRIP);
        org.lwjgl.opengl.GL11.glVertex3d(0, 0, 0);
        org.lwjgl.opengl.GL11.glVertex3d(0, -0.75, 0);
        org.lwjgl.opengl.GL11.glEnd();
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINE_STRIP);
        org.lwjgl.opengl.GL11.glVertex3d(0, 0.2, 0);
        org.lwjgl.opengl.GL11.glVertex3d(-0.6, -0.2, 0);
        org.lwjgl.opengl.GL11.glEnd();
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINE_STRIP);
        org.lwjgl.opengl.GL11.glVertex3d(0, 0.2, 0);
        org.lwjgl.opengl.GL11.glVertex3d(0.6, -0.2, 0);
        org.lwjgl.opengl.GL11.glEnd();
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINE_STRIP);
        org.lwjgl.opengl.GL11.glVertex3d(0, -0.75, 0);
        org.lwjgl.opengl.GL11.glVertex3d(-0.3, -1.5, 0);
        org.lwjgl.opengl.GL11.glEnd();
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINE_STRIP);
        org.lwjgl.opengl.GL11.glVertex3d(0, -0.75, 0);
        org.lwjgl.opengl.GL11.glVertex3d(0.3, -1.5, 0);
        org.lwjgl.opengl.GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}
