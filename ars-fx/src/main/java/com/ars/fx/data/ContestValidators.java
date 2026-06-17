package com.ars.fx.data;

/**
 * Live field validators keyed by {@code ContestPlugin.FieldDef.getValidator()}.
 * Empty values pass here (required-ness is enforced separately at save time).
 */
public final class ContestValidators {
    private ContestValidators() {}

    public static boolean valid(String validator, String value) {
        if (validator == null || validator.isBlank()) return true;
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) return true;
        return switch (validator) {
            case "numeric"      -> v.matches("\\d+");
            case "ss_check"     -> v.matches("\\d{2}");                 // 2-digit year first licensed
            case "fd_class"     -> v.matches("(?i)\\d+[A-F]");          // Field Day class e.g. 2A, 1E
            case "maidenhead"   -> v.matches("(?i)[A-R]{2}\\d{2}([A-X]{2})?");
            case "maidenhead6"  -> v.matches("(?i)[A-R]{2}\\d{2}[A-X]{2}");
            default             -> true;
        };
    }
}
