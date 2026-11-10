package dev.saisree.results;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tiny helpers for flat JSON objects, so the project needs no external libraries. */
final class Json {
    private static final Pattern FIELD =
            Pattern.compile("\"(\\w+)\"\\s*:\\s*(\"((?:[^\"\\\\]|\\\\.)*)\"|-?\\d+(?:\\.\\d+)?)");

    private Json() {}

    static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Reads a flat object such as {"name":"login","durationMs":120} into a map of strings. */
    static Map<String, String> parseFlat(String body) {
        Map<String, String> out = new HashMap<>();
        Matcher m = FIELD.matcher(body);
        while (m.find()) {
            out.put(m.group(1), m.group(3) != null ? m.group(3) : m.group(2));
        }
        return out;
    }
}
