package com.easycaikuai.deceptionclient.module.modules.render;

import com.easycaikuai.deceptionclient.event.EventTarget;
import com.easycaikuai.deceptionclient.events.Render2DEvent;
import com.easycaikuai.deceptionclient.module.Module;
import com.easycaikuai.deceptionclient.property.properties.BooleanProperty;
import com.easycaikuai.deceptionclient.property.properties.FloatProperty;
import com.easycaikuai.deceptionclient.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GifDisplay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty gifMode = new ModeProperty("GIF", 0, new String[]{"AngryPig", "Best", "CryCat", "Dancer", "DieCat", "DiePig", "Kabo", "NoneBig", "PigFucker", "YaoMao"});
    public final BooleanProperty lockRatio = new BooleanProperty("Lock Ratio", true);
    public final FloatProperty posX = new FloatProperty("X", 200.0F, 0.0F, 3000.0F);
    public final FloatProperty posY = new FloatProperty("Y", 100.0F, 0.0F, 3000.0F);
    public final FloatProperty imgWidth = new FloatProperty("Width", 128.0F, 1.0F, 2000.0F);
    public final FloatProperty imgHeight = new FloatProperty("Height", 128.0F, 1.0F, 2000.0F, () -> !lockRatio.getValue());

    private List<Integer> frameDelays;
    private List<Integer> textureIds;
    private int currentFrame;
    private long lastFrameTime;
    private String loadedGif = "";
    private int originalWidth;
    private int originalHeight;

    public GifDisplay() {
        super("GifDisplay", false, true);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (!isEnabled()) return;

        String currentGif = gifMode.getModeString();
        if (!currentGif.equals(loadedGif)) {
            loadGif(currentGif);
        }

        if (textureIds == null || textureIds.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= frameDelays.get(currentFrame)) {
            currentFrame = (currentFrame + 1) % textureIds.size();
            lastFrameTime = now;
        }

        int texId = textureIds.get(currentFrame);
        if (texId == 0) return;

        int drawWidth = (int) (float) imgWidth.getValue();
        int drawHeight;

        if (lockRatio.getValue() && originalWidth > 0 && originalHeight > 0) {
            drawHeight = (int) (drawWidth * originalHeight / originalWidth);
        } else {
            drawHeight = (int) (float) imgHeight.getValue();
        }

        int x = (int) (float) posX.getValue();
        int y = (int) (float) posY.getValue();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.bindTexture(texId);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, drawWidth, drawHeight, drawWidth, drawHeight);
    }

    private void loadGif(String name) {
        unloadTextures();
        currentFrame = 0;
        lastFrameTime = System.currentTimeMillis();

        try {
            IResource resource = mc.getResourceManager().getResource(
                    new ResourceLocation("minecraft", "deceptionclient/texture/gif/" + name + ".gif"));
            InputStream inputStream = resource.getInputStream();

            ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
            ImageInputStream iis = ImageIO.createImageInputStream(inputStream);
            reader.setInput(iis);

            int count = reader.getNumImages(true);
            List<BufferedImage> frames = new ArrayList<>();
            List<Integer> delays = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                BufferedImage frame = reader.read(i);
                frames.add(frame);

                int delay = 100;
                try {
                    IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                    for (int j = 0; j < root.getChildNodes().getLength(); j++) {
                        if (root.getChildNodes().item(j) instanceof IIOMetadataNode) {
                            IIOMetadataNode node = (IIOMetadataNode) root.getChildNodes().item(j);
                            if ("GraphicControlExtension".equals(node.getNodeName())) {
                                String delayStr = node.getAttribute("delayTime");
                                if (!delayStr.isEmpty()) {
                                    delay = Integer.parseInt(delayStr) * 10;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }

                delays.add(Math.max(delay, 20));
            }
            iis.close();
            reader.dispose();

            if (frames.isEmpty()) {
                return;
            }

            this.loadedGif = name;
            this.originalWidth = frames.get(0).getWidth();
            this.originalHeight = frames.get(0).getHeight();
            this.frameDelays = delays;
            this.textureIds = new ArrayList<>();
            for (BufferedImage frame : frames) {
                int[] pixels = frame.getRGB(0, 0, frame.getWidth(), frame.getHeight(), null, 0, frame.getWidth());

                int texId = GL11.glGenTextures();
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

                ByteBuffer buffer = ByteBuffer.allocateDirect(4 * pixels.length).order(ByteOrder.nativeOrder());
                for (int pixel : pixels) {
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
                buffer.flip();

                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, frame.getWidth(), frame.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
                textureIds.add(texId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisabled() {
        unloadTextures();
    }

    private void unloadTextures() {
        if (textureIds != null) {
            for (Iterator<Integer> iterator = textureIds.iterator(); iterator.hasNext(); ) {
                int texId = iterator.next();
                if (texId != 0) {
                    GL11.glDeleteTextures(texId);
                }
            }
        }
        this.frameDelays = null;
        this.textureIds = null;
        this.loadedGif = "";
        this.currentFrame = 0;
    }
}