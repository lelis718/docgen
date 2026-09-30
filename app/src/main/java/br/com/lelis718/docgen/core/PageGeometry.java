package br.com.lelis718.docgen.core;

public final class PageGeometry {

    public static final PageGeometry A4 = new PageGeometry(595f, 842f, 36f, 36f, 36f, 36f);

    private final float width;
    private final float height;
    private final float marginLeft;
    private final float marginTop;
    private final float marginRight;
    private final float marginBottom;

    public PageGeometry(float width, float height, float marginLeft, float marginTop,
                        float marginRight, float marginBottom) {
        this.width = width;
        this.height = height;
        this.marginLeft = marginLeft;
        this.marginTop = marginTop;
        this.marginRight = marginRight;
        this.marginBottom = marginBottom;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float marginLeft() {
        return marginLeft;
    }

    public float marginTop() {
        return marginTop;
    }

    public Box contentBox() {
        return new Box(marginLeft, marginTop, width - marginRight, height - marginBottom);
    }

    public Box pageBox() {
        return new Box(0f, 0f, width, height);
    }
}
