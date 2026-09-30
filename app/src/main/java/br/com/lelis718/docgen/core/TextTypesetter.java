package br.com.lelis718.docgen.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TextTypesetter {

    public interface LineMeasurer {
        float measure(String line);
    }

    private TextTypesetter() {
    }

    public static List<String> wrap(String text, float maxWidth, LineMeasurer measurer) {
        List<String> lines = new ArrayList<>();
        if (text == null || maxWidth <= 0f) {
            return Collections.unmodifiableList(lines);
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n").replace("\t", "    ");
        for (String paragraph : normalized.split("\n", -1)) {
            wrapParagraph(paragraph, maxWidth, measurer, lines);
        }
        return Collections.unmodifiableList(lines);
    }

    private static void wrapParagraph(String paragraph, float maxWidth, LineMeasurer measurer,
                                      List<String> lines) {
        int leading = 0;
        while (leading < paragraph.length() && paragraph.charAt(leading) == ' ') {
            leading++;
        }
        String indent = paragraph.substring(0, leading);
        String body = paragraph.substring(leading).trim();
        if (body.isEmpty()) {
            lines.add(indent);
            return;
        }
        String[] words = body.split(" +");
        String current = indent;
        for (String word : words) {
            String candidate;
            if (current.isEmpty() || current.charAt(current.length() - 1) == ' ') {
                candidate = current + word;
            } else {
                candidate = current + " " + word;
            }
            if (current.isEmpty() || measurer.measure(candidate) <= maxWidth) {
                current = candidate;
            } else {
                lines.add(current);
                current = word;
            }
            if (!current.isEmpty() && measurer.measure(current) > maxWidth) {
                current = hardBreak(current, maxWidth, measurer, lines);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current);
        }
    }

    private static String hardBreak(String word, float maxWidth, LineMeasurer measurer,
                                    List<String> lines) {
        StringBuilder chunk = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            String candidate = chunk.toString() + c;
            if (chunk.length() > 0 && measurer.measure(candidate) > maxWidth) {
                lines.add(chunk.toString());
                chunk.setLength(0);
                chunk.append(c);
            } else {
                chunk.append(c);
            }
        }
        return chunk.toString();
    }
}
