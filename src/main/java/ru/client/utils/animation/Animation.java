package ru.client.utils.animation;

public final class Animation {
    private float speed;
    private float value, target;
    private long lastTime = System.nanoTime();

    public Animation(float speed, float initial) {
        this.speed = speed;
        this.value = this.target = initial;
    }

    public void setTarget(float target) { this.target = target; }

    public float update() {
        long now = System.nanoTime();
        float dt = Math.min((now - lastTime) / 1_000_000_000f, 0.1f);
        lastTime = now;
        value += (target - value) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.001f) value = target;
        return value;
    }

    public float get() { return value; }
    public void setSpeed(float speed) { this.speed = speed; }
    public void snap(float v) { value = target = v; }
}
