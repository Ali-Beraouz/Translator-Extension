package ma.aui.darija.model;

import java.util.Locale;

/**
 * Writing system used for the Darija output.
 * Darija is written both in Arabic script and in Latin letters ("Arabizi", e.g. "kifach dayr?").
 */
public enum DarijaScript {
    ARABIC,
    LATIN;

    /** Parses "arabic"/"latin" (case-insensitive). Null or blank means ARABIC. */
    public static DarijaScript parse(String value) {
        if (value == null || value.isBlank()) {
            return ARABIC;
        }
        try {
            return DarijaScript.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
