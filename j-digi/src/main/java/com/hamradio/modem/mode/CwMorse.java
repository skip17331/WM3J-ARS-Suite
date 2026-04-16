package com.hamradio.modem.mode;

import java.util.HashMap;
import java.util.Map;

/**
 * ITU Morse code tables for CW encode/decode.
 *
 * Codes are represented as strings of '.' (dit) and '-' (dah).
 * Input characters are case-folded to upper-case before lookup.
 */
public final class CwMorse {

    // ── Letters A–Z ───────────────────────────────────────────────────
    private static final String[] LETTER_CODES = {
        ".-",    // A
        "-...",  // B
        "-.-.",  // C
        "-..",   // D
        ".",     // E
        "..-.",  // F
        "--.",   // G
        "....",  // H
        "..",    // I
        ".---",  // J
        "-.-",   // K
        ".-..",  // L
        "--",    // M
        "-.",    // N
        "---",   // O
        ".--.",  // P
        "--.-",  // Q
        ".-.",   // R
        "...",   // S
        "-",     // T
        "..-",   // U
        "...-",  // V
        ".--",   // W
        "-..-",  // X
        "-.--",  // Y
        "--.."   // Z
    };

    // ── Digits 0–9 ────────────────────────────────────────────────────
    private static final String[] DIGIT_CODES = {
        "-----",  // 0
        ".----",  // 1
        "..---",  // 2
        "...--",  // 3
        "....-",  // 4
        ".....",  // 5
        "-....",  // 6
        "--...",  // 7
        "---..",  // 8
        "----."   // 9
    };

    // ── Punctuation ───────────────────────────────────────────────────
    private static final Object[][] PUNCT_CODES = {
        { '.',  ".-.-.-" },
        { ',',  "--..--" },
        { '?',  "..--.." },
        { '\'', ".----." },
        { '!',  "-.-.--" },
        { '/',  "-..-."  },
        { '(',  "-.--."  },
        { ')',  "-.--.-" },
        { '&',  ".-..."  },
        { ':',  "---..." },
        { ';',  "-.-.-." },
        { '=',  "-...-"  },
        { '+',  ".-.-."  },
        { '-',  "-....-" },
        { '_',  "..--.---" },   // also used for CW break
        { '"',  ".-..-." },
        { '$',  "...-..-"},
        { '@',  ".--.-." },
    };

    // ── Lookup maps ───────────────────────────────────────────────────
    private static final Map<Character, String> CHAR_TO_CODE = new HashMap<>();
    private static final Map<String, Character> CODE_TO_CHAR = new HashMap<>();

    static {
        for (int i = 0; i < LETTER_CODES.length; i++) {
            char c = (char) ('A' + i);
            CHAR_TO_CODE.put(c, LETTER_CODES[i]);
            CODE_TO_CHAR.put(LETTER_CODES[i], c);
        }
        for (int i = 0; i < DIGIT_CODES.length; i++) {
            char c = (char) ('0' + i);
            CHAR_TO_CODE.put(c, DIGIT_CODES[i]);
            CODE_TO_CHAR.put(DIGIT_CODES[i], c);
        }
        for (Object[] entry : PUNCT_CODES) {
            char   c    = (Character) entry[0];
            String code = (String)    entry[1];
            CHAR_TO_CODE.put(c, code);
            CODE_TO_CHAR.put(code, c);
        }
    }

    private CwMorse() {}

    /**
     * Return the ITU Morse code for {@code c} (case-insensitive), or
     * {@code null} if the character has no standard code.
     *
     * @param c  input character (letter, digit, or punctuation)
     * @return   code string of '.' and '-', e.g. {@code ".-"} for 'A'
     */
    public static String encode(char c) {
        return CHAR_TO_CODE.get(Character.toUpperCase(c));
    }

    /**
     * Return the ASCII character for a Morse code string, or
     * {@code null} if the code is not recognised.
     *
     * @param code  dot-dash string, e.g. {@code ".-"}
     * @return      decoded character, or {@code null}
     */
    public static Character decode(String code) {
        if (code == null || code.isEmpty()) return null;
        return CODE_TO_CHAR.get(code);
    }
}
