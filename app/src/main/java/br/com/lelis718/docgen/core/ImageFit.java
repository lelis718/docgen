package br.com.lelis718.docgen.core;

public final class ImageFit {

    private static final float EPSILON = 0.01f;

    private ImageFit() {
    }

    public static Box contain(float sourceWidth, float sourceHeight, Box target) {
        if (sourceWidth <= 0f || sourceHeight <= 0f) {
            return new Box(target.left, target.top, target.left, target.top);
        }
        float targetWidth = target.width();
        float targetHeight = target.height();
        if (targetWidth <= 0f || targetHeight <= 0f) {
            return new Box(target.left, target.top, target.left, target.top);
        }
        float scale = Math.min(targetWidth / sourceWidth, targetHeight / sourceHeight);
        float width = sourceWidth * scale;
        float height = sourceHeight * scale;
        float dx = (targetWidth - width) / 2f;
        float dy = (targetHeight - height) / 2f;
        return new Box(target.left + dx, target.top + dy, target.left + dx + width, target.top + dy + height);
    }

    public static Box topAligned(float sourceWidth, float sourceHeight, float maxWidth, float maxHeight, float left, float top) {
        if (sourceWidth <= 0f || sourceHeight <= 0f) {
            return new Box(left, top, left, top);
        }
        float scale = Math.min(maxWidth / sourceWidth, maxHeight / sourceHeight);
        float width = sourceWidth * scale;
        float height = sourceHeight * scale;
        if (width < EPSILON) {
            width = 0f;
        }
        if (height < EPSILON) {
            height = 0f;
        }
        return Box.of(left, top, width, height);
    }
}
