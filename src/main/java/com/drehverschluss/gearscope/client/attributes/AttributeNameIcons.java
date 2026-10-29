package com.drehverschluss.gearscope.client.attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

/**
 * Icon resource packs put an icon glyph in front of translated attribute names, e.g.
 * "§f\uDB80\uDCFF §rAttack Damage" (Icons) or "§f\uD833\uDD03§r Attack Damage" (RPG Series Icons). The list
 * shows icons in its own column, so it splits such a glyph off the name: leading formatting codes, one icon
 * code point (private use, unassigned or symbol) and the whitespace after it.
 */
final class AttributeNameIcons {
    private static final char FORMATTING_PREFIX = '\u00A7';

    /** The icon part (formatting codes + glyph), if any, and the remaining name; styles are preserved. */
    record Split(@Nullable FormattedText icon, FormattedText name) {
    }

    private AttributeNameIcons() {
    }

    static Split split(Component name) {
        StringBuilder text = new StringBuilder();
        List<Style> styles = new ArrayList<>();
        name.visit((style, part) -> {
            text.append(part);
            for (int i = 0; i < part.length(); i++) {
                styles.add(style);
            }
            return Optional.empty();
        }, Style.EMPTY);

        int nameStart = iconEnd(text);
        if (nameStart < 0) {
            return new Split(null, name);
        }
        // The icon part ends before the whitespace that separates it from the name.
        int iconEnd = nameStart;
        while (iconEnd > 0 && Character.isWhitespace(text.charAt(iconEnd - 1))) {
            iconEnd--;
        }
        return new Split(slice(text, styles, 0, iconEnd), slice(text, styles, nameStart, text.length()));
    }

    /** Index where the name starts after a leading icon glyph, or -1 if there is no such glyph. */
    private static int iconEnd(CharSequence text) {
        int i = skipFormatting(text, 0);
        if (i >= text.length()) {
            return -1;
        }
        int codePoint = Character.codePointAt(text, i);
        if (!isIconCodePoint(codePoint)) {
            return -1;
        }
        i = skipFormatting(text, i + Character.charCount(codePoint));
        if (i >= text.length() || !Character.isWhitespace(text.charAt(i))) {
            return -1;
        }
        return i + 1;
    }

    private static int skipFormatting(CharSequence text, int start) {
        int i = start;
        while (i + 1 < text.length() && text.charAt(i) == FORMATTING_PREFIX) {
            i += 2;
        }
        return i;
    }

    private static boolean isIconCodePoint(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.PRIVATE_USE || type == Character.UNASSIGNED || type == Character.OTHER_SYMBOL;
    }

    private static FormattedText slice(CharSequence text, List<Style> styles, int start, int end) {
        List<FormattedText> parts = new ArrayList<>();
        int runStart = start;
        for (int i = start + 1; i <= end; i++) {
            if (i == end || !styles.get(i).equals(styles.get(runStart))) {
                parts.add(FormattedText.of(text.subSequence(runStart, i).toString(), styles.get(runStart)));
                runStart = i;
            }
        }
        return parts.isEmpty() ? FormattedText.EMPTY : FormattedText.composite(parts);
    }
}
