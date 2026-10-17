package ru.client.utils.render;

public final class Shapes {
    private Shapes() {}

    public static final float[] CIRCLE = circle(20);
    public static final float[] STAR = star(5, 1f, 0.45f);
    public static final float[] DIAMOND = {0, 1, 0.6f, 0, 0, -1, -0.6f, 0};
    public static final float[] HEART = heart(28);

    private static float[] circle(int n) {
        float[] p = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            p[i * 2] = (float) Math.cos(a);
            p[i * 2 + 1] = (float) Math.sin(a);
        }
        return p;
    }

    private static float[] star(int points, float outer, float inner) {
        float[] p = new float[points * 4];
        for (int i = 0; i < points * 2; i++) {
            double a = Math.PI / 2 + Math.PI * i / points;
            float r = (i % 2 == 0) ? outer : inner;
            p[i * 2] = (float) Math.cos(a) * r;
            p[i * 2 + 1] = (float) Math.sin(a) * r;
        }
        return p;
    }

    private static float[] heart(int n) {
        float[] p = new float[n * 2];
        for (int i = 0; i < n; i++) {
            double t = Math.PI * 2 * i / n;
            double x = 16 * Math.pow(Math.sin(t), 3);
            double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
            p[i * 2] = (float) (x / 17.0);
            p[i * 2 + 1] = (float) ((y + 2) / 17.0);
        }
        return p;
    }
}
