package com.elvero.shop.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s_-]+");

    private SlugUtils() {}

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String cleaned = NON_LATIN.matcher(noAccents).replaceAll("");
        String slug = WHITESPACE.matcher(cleaned).replaceAll("-");
        return slug.toLowerCase(Locale.ROOT).replaceAll("^-+|-+$", "");
    }
}
