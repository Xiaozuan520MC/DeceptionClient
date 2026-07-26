package com.easycaikuai.deceptionclient.util.gui;

import com.easycaikuai.deceptionclient.util.TimerUtil;
import com.easycaikuai.deceptionclient.util.shader.RoundedUtils;
import org.lwjgl.input.Mouse;
import java.awt.Color;

public class ScrollUtil {
    public double target;
    public double scroll;
    public double max = 25.0D;
    public TimerUtil stopwatch = new TimerUtil();
    public TimerUtil stopWatch2 = new TimerUtil();
    public boolean scrollingIsAllowed;

    public void onRender() {
        if (this.stopWatch2.hasReached(50L)) {
            float wheel = Mouse.getDWheel();
            double stretch = 30.0D;
            this.target = Math.min(Math.max(this.target + (wheel / 2.0F), this.max - ((wheel == 0.0F) ? 0.0D : stretch)), (wheel == 0.0F) ? 0.0D : stretch);
            this.stopWatch2.reset();
        }

        int iterations = (int) Math.min(this.stopwatch.getElapsedTime(), 100L);
        for (int i = 0; i < iterations; i++) {
            this.scroll = lerp(this.scroll, this.target, 0.009999999776482582D);
        }

        this.stopwatch.reset();
    }

    public void renderScrollBar(double x, double y, double maxHeight) {
        if (getMax() == 0.0D || maxHeight <= 0.0D) return;
        double percentage = getScroll() / getMax();
        double scrollBarHeight = maxHeight - getMax() / (getMax() - maxHeight) * maxHeight;
        this.scrollingIsAllowed = (scrollBarHeight < maxHeight);
        if (!this.scrollingIsAllowed) return;
        double scrollY = y + maxHeight * percentage - scrollBarHeight * percentage;
        Color color = new Color(255, 255, 255, 60);
        RoundedUtils.drawRound((float) x, (float) scrollY, 1.0F, (float) scrollBarHeight, 0.5F, color);
    }

    public void reset() {
        this.scroll = 0.0D;
        this.target = 0.0D;
    }

    public double getScroll() { return this.scroll; }
    public double getMax() { return this.max; }
    public void setMax(double max) { this.max = max; }
    private double lerp(double a, double b, double t) { return a + (b - a) * t; }
}