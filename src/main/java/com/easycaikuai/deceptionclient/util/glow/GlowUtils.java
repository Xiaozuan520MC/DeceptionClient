package com.easycaikuai.deceptionclient.util.glow;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class GlowUtils {
    private static final int MAX_CACHE = 40;
    private static final Map<String, Integer> cache = new LinkedHashMap<String, Integer>(16, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            if (size() > MAX_CACHE) { GL11.glDeleteTextures(eldest.getValue()); return true; }
            return false;
        }
    };

    private static float[] createKernel(int radius) {
        int r = Math.max(radius, 2), size = r * 2 + 1;
        float[] kernel = new float[size];
        float sigma22 = 2f * (r / 2f) * (r / 2f);
        float total = 0;
        for (int i = -r; i <= r; i++) { float v = (float) Math.exp(-(i * i) / sigma22); kernel[i + r] = v; total += v; }
        for (int i = 0; i < size; i++) kernel[i] /= total;
        return kernel;
    }

    private static BufferedImage blur(BufferedImage img, int radius) {
        int w = img.getWidth(), h = img.getHeight();
        float[] kernel = createKernel(radius);
        int r = radius;
        BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float a = 0, rd = 0, gn = 0, bl = 0;
            for (int k = -r; k <= r; k++) {
                int px = Math.max(0, Math.min(w - 1, x + k));
                int rgb = img.getRGB(px, y); float wt = kernel[k + r];
                a += ((rgb >> 24) & 255) * wt; rd += ((rgb >> 16) & 255) * wt; gn += ((rgb >> 8) & 255) * wt; bl += (rgb & 255) * wt;
            }
            tmp.setRGB(x, y, ((int)a << 24) | ((int)rd << 16) | ((int)gn << 8) | (int)bl);
        }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float a = 0, rd = 0, gn = 0, bl = 0;
            for (int k = -r; k <= r; k++) {
                int py2 = Math.max(0, Math.min(h - 1, y + k));
                int rgb = tmp.getRGB(x, py2); float wt = kernel[k + r];
                a += ((rgb >> 24) & 255) * wt; rd += ((rgb >> 16) & 255) * wt; gn += ((rgb >> 8) & 255) * wt; bl += (rgb & 255) * wt;
            }
            out.setRGB(x, y, ((int)a << 24) | ((int)rd << 16) | ((int)gn << 8) | (int)bl);
        }
        return out;
    }

    public static void drawGlow(float x, float y, float w, float h, int radius, Color color) {
        String key = w + "_" + h + "_" + radius + "_" + color.getRGB();
        Integer texture = cache.get(key);
        if (texture == null) {
            int iw = (int) w + radius * 2, ih = (int) h + radius * 2;
            BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(color); g.fillRoundRect(radius, radius, (int) w, (int) h, radius, radius); g.dispose();
            BufferedImage blurred = blur(img, radius);
            texture = GL11.glGenTextures(); cache.put(key, texture);
            GlStateManager.bindTexture(texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            int[] pixels = new int[blurred.getWidth() * blurred.getHeight()];
            blurred.getRGB(0, 0, blurred.getWidth(), blurred.getHeight(), pixels, 0, blurred.getWidth());
            ByteBuffer buf = BufferUtils.createByteBuffer(pixels.length * 4);
            for (int px : pixels) { buf.put((byte)((px >> 16) & 255)); buf.put((byte)((px >> 8) & 255)); buf.put((byte)(px & 255)); buf.put((byte)((px >> 24) & 255)); }
            buf.flip();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, blurred.getWidth(), blurred.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        }
        GlStateManager.enableBlend(); GlStateManager.disableAlpha(); GlStateManager.enableTexture2D();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
        GlStateManager.bindTexture(texture);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x - radius, y - radius);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + w + radius, y - radius);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + w + radius, y + h + radius);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x - radius, y + h + radius);
        GL11.glEnd();
        GlStateManager.bindTexture(0); GlStateManager.enableAlpha(); GlStateManager.disableBlend();
        GL11.glColor4f(1, 1, 1, 1);
    }
}
