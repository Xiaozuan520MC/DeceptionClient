package com.easycaikuai.deceptionclient.gui.font;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class FontRenderer {
    private static final int[] colorCode = new int[32];
    private final boolean antiAlias;

    static {
        for (int i = 0; i < 32; i++) {
            int base = (i >> 3 & 0x1) * 85;
            int r = (i >> 2 & 0x1) * 170 + base;
            int g = (i >> 1 & 0x1) * 170 + base;
            int b = (i & 0x1) * 170 + base;
            if (i == 6) r += 85;
            if (i >= 16) { r /= 4; g /= 4; b /= 4; }
            colorCode[i] = (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
        }
    }

    public final float drawCenteredString(String text, float x, float y, int color) {
        return drawString(text, x - getStringWidth(text) / 2.0F, y, color);
    }

    public final float drawCenteredString(String text, double x, double y, int color) {
        return drawString(text, (float) (x - getStringWidth(text) / 2.0D), (float) y, color);
    }

    public final void drawCenteredStringWithShadow(String text, float x, float y, int color) {
        drawStringWithShadow(text, x - getStringWidth(text) / 2.0F, y, color);
    }

    private final byte[][] charwidth = new byte[256][];
    private final int[] textures = new int[256];
    private final FontRenderContext context = new FontRenderContext(new AffineTransform(), true, true);
    private Font font = null;

    private float size = 0.0F;
    private int fontWidth = 0;
    private int fontHeight = 0;
    private int textureWidth = 0;
    private int textureHeight = 0;

    public FontRenderer(Font font) {
        this.antiAlias = true;
        this.font = font;
        this.size = font.getSize2D();
        Arrays.fill(this.textures, -1);
        Rectangle2D maxBounds = font.getMaxCharBounds(this.context);
        this.fontWidth = (int) Math.ceil(maxBounds.getWidth());
        this.fontHeight = (int) Math.ceil(maxBounds.getHeight());
        if (this.fontWidth > 127 || this.fontHeight > 127) throw new IllegalArgumentException("Font size to large!");
        this.textureWidth = resizeToOpenGLSupportResolution(this.fontWidth * 16);
        this.textureHeight = resizeToOpenGLSupportResolution(this.fontHeight * 16);
    }

    public FontRenderer(Font font, boolean antiAlias) {
        this.antiAlias = antiAlias;
        this.font = font;
        this.size = font.getSize2D();
        Arrays.fill(this.textures, -1);
        Rectangle2D maxBounds = font.getMaxCharBounds(this.context);
        this.fontWidth = (int) Math.ceil(maxBounds.getWidth());
        this.fontHeight = (int) Math.ceil(maxBounds.getHeight());
        if (this.fontWidth > 127 || this.fontHeight > 127) throw new IllegalArgumentException("Font size to large!");
        this.textureWidth = resizeToOpenGLSupportResolution(this.fontWidth * 16);
        this.textureHeight = resizeToOpenGLSupportResolution(this.fontHeight * 16);
    }

    public final int getHeight() {
        return this.fontHeight / 2;
    }

    protected final int drawChar(char chr, float x, float y) {
        int region = chr >> 8;
        int id = chr & 0xFF;
        int xTexCoord = (id & 0xF) * this.fontWidth;
        int yTexCoord = (id >> 4) * this.fontHeight;
        int width = getOrGenerateCharWidthMap(region)[id];
        GlStateManager.bindTexture(getOrGenerateCharTexture(region));
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GL11.glBegin(7);
        GL11.glTexCoord2d(wrapTextureCoord(xTexCoord, this.textureWidth), wrapTextureCoord(yTexCoord, this.textureHeight));
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2d(wrapTextureCoord(xTexCoord, this.textureWidth), wrapTextureCoord(yTexCoord + this.fontHeight, this.textureHeight));
        GL11.glVertex2f(x, y + this.fontHeight);
        GL11.glTexCoord2d(wrapTextureCoord(xTexCoord + width, this.textureWidth), wrapTextureCoord(yTexCoord + this.fontHeight, this.textureHeight));
        GL11.glVertex2f(x + width, y + this.fontHeight);
        GL11.glTexCoord2d(wrapTextureCoord(xTexCoord + width, this.textureWidth), wrapTextureCoord(yTexCoord, this.textureHeight));
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();
        return width;
    }

    public int drawString(String str, float x, float y, int color) {
        return drawString(str, x, y, color, false);
    }

    public int drawString(String str, double x, double y, int color) {
        return drawString(str, (float) x, (float) y, color, false);
    }

    public final int drawString(String str, float x, float y, int color, boolean darken) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        str = str.replace("▬", "=");
        y -= 2.0F;
        x *= 2.0F;
        y *= 2.0F;
        y -= 2.0F;
        int offset = 0;
        if (darken) color = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;

        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        float a = (color >> 24 & 0xFF) / 255.0F;
        if (a == 0.0F) a = 1.0F;
        GlStateManager.color(r, g, b, a);
        GL11.glPushMatrix();
        GL11.glScaled(0.5D, 0.5D, 0.5D);
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char chr = chars[i];
            if (chr == '§' && i != chars.length - 1) {
                i++;
                int ci = "0123456789abcdef".indexOf(chars[i]);
                if (ci != -1) {
                    if (darken) ci |= 0x10;
                    int c = colorCode[ci];
                    GlStateManager.color((c >> 16 & 0xFF) / 255.0F, (c >> 8 & 0xFF) / 255.0F, (c & 0xFF) / 255.0F, a);
                }
            } else {
                offset += drawChar(chr, x + offset, y);
            }
        }
        GL11.glPopMatrix();
        return offset;
    }

    public float getMiddleOfBox(float height) {
        return height / 2.0F - getHeight() / 2.0F;
    }

    public final int getStringWidth(String text) {
        if (text == null) return 0;
        int width = 0;
        char[] currentData = text.toCharArray();
        int size = text.length();
        int i = 0;
        while (i < size) {
            char chr = currentData[i];
            if (chr == '§') { i++; }
            else { width += getOrGenerateCharWidthMap(chr >> 8)[chr & 0xFF]; }
            i++;
        }
        return width / 2;
    }

    public final float getSize() { return this.size; }

    private final int generateCharTexture(int id) {
        int textureId = GL11.glGenTextures();
        int offset = id << 8;
        BufferedImage img = new BufferedImage(this.textureWidth, this.textureHeight, 2);
        Graphics2D g = (Graphics2D) img.getGraphics();
        if (this.antiAlias) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setFont(this.font);
        g.setColor(Color.WHITE);
        FontMetrics fontMetrics = g.getFontMetrics();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                String chr = String.valueOf((char) (y << 4 | x | offset));
                g.drawString(chr, x * this.fontWidth, y * this.fontHeight + fontMetrics.getAscent());
            }
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, this.textureWidth, this.textureHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, imageToBuffer(img));
        return textureId;
    }

    private int getOrGenerateCharTexture(int id) {
        if (this.textures[id] == -1) {
            this.textures[id] = generateCharTexture(id);
        }
        return this.textures[id];
    }

    private int resizeToOpenGLSupportResolution(int size) {
        int power = 0;
        for (; size > 1 << power; power++);
        return 1 << power;
    }

    private byte[] generateCharWidthMap(int id) {
        int offset = id << 8;
        byte[] widthmap = new byte[256];
        for (int i = 0; i < widthmap.length; i++) {
            widthmap[i] = (byte) (int) Math.ceil(this.font.getStringBounds(String.valueOf((char) (i | offset)), this.context).getWidth());
        }
        return widthmap;
    }

    private final byte[] getOrGenerateCharWidthMap(int id) {
        if (this.charwidth[id] == null) {
            this.charwidth[id] = generateCharWidthMap(id);
        }
        return this.charwidth[id];
    }

    private double wrapTextureCoord(int coord, int size) {
        return (double) coord / size;
    }

    private static final ByteBuffer imageToBuffer(BufferedImage img) {
        int[] arr = img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
        ByteBuffer buf = ByteBuffer.allocateDirect(4 * arr.length);
        for (int i : arr) {
            buf.putInt(i << 8 | i >> 24 & 0xFF);
        }
        buf.flip();
        return buf;
    }

    protected final void finalize() {
        for (int textureId : this.textures) {
            if (textureId != -1) GL11.glDeleteTextures(textureId);
        }
    }

    public final void drawStringWithShadow(String newstr, float i, float i1, int rgb) {
        drawString(newstr, i + 0.5F, i1 + 0.5F, rgb, true);
        drawString(newstr, i, i1, rgb);
    }
}