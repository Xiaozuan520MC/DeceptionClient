package com.easycaikuai.deceptionclient.util.animation;

public class RiseAnim {
    private Easing easing;
    private long duration;
    private long millis;
    private long startTime;
    private double startValue;
    private double destinationValue;
    private double value;
    private boolean finished;

    public RiseAnim(Easing easing, long duration) {
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
    }

    public void run(double destinationValue) {
        this.millis = System.currentTimeMillis();
        if (this.destinationValue != destinationValue) {
            this.destinationValue = destinationValue;
            reset();
        } else {
            this.finished = (this.millis - this.duration > this.startTime);
            if (this.finished) {
                this.value = destinationValue;
                return;
            }
        }
        double result = this.easing.getFunction().apply(getProgress());
        if (this.value > destinationValue) {
            this.value = this.startValue - (this.startValue - destinationValue) * result;
        } else {
            this.value = this.startValue + (destinationValue - this.startValue) * result;
        }
    }

    public double getProgress() {
        return (System.currentTimeMillis() - this.startTime) / (double) this.duration;
    }

    public void reset() {
        this.startTime = System.currentTimeMillis();
        this.startValue = this.value;
        this.finished = false;
    }

    public Easing getEasing() { return this.easing; }
    public void setEasing(Easing easing) { this.easing = easing; }
    public long getDuration() { return this.duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public double getValue() { return this.value; }
    public void setValue(double value) { this.value = value; }
    public boolean isFinished() { return this.finished; }
}