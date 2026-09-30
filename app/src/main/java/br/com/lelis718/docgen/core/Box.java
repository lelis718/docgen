package br.com.lelis718.docgen.core;

public final class Box {

    public final float left;
    public final float top;
    public final float right;
    public final float bottom;

    public Box(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public static Box of(float left, float top, float width, float height) {
        return new Box(left, top, left + width, top + height);
    }

    public float width() {
        return right - left;
    }

    public float height() {
        return bottom - top;
    }

    public Box translated(float dx, float dy) {
        return new Box(left + dx, top + dy, right + dx, bottom + dy);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Box)) {
            return false;
        }
        Box box = (Box) other;
        return closeTo(left, box.left) && closeTo(top, box.top)
                && closeTo(right, box.right) && closeTo(bottom, box.bottom);
    }

    @Override
    public int hashCode() {
        int result = floatHash(left);
        result = 31 * result + floatHash(top);
        result = 31 * result + floatHash(right);
        result = 31 * result + floatHash(bottom);
        return result;
    }

    @Override
    public String toString() {
        return "Box(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }

    private static boolean closeTo(float a, float b) {
        return Math.abs(a - b) < 0.01f;
    }

    private static int floatHash(float value) {
        return Float.floatToIntBits(value);
    }
}
