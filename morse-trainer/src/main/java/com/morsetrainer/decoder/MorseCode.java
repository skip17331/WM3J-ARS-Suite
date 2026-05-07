package com.morsetrainer.decoder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Bidirectional Morse Code mapping. Uses '.' for dit, '-' for dah.
 * Includes letters, digits, common punctuation, and a few prosigns.
 */
public final class MorseCode {

    private static final Map<Character, String> CHAR_TO_MORSE;
    private static final Map<String, Character> MORSE_TO_CHAR;

    static {
        Map<Character, String> m = new HashMap<>();
        // Letters
        m.put('A', ".-");    m.put('B', "-...");  m.put('C', "-.-.");  m.put('D', "-..");
        m.put('E', ".");     m.put('F', "..-.");  m.put('G', "--.");   m.put('H', "....");
        m.put('I', "..");    m.put('J', ".---");  m.put('K', "-.-");   m.put('L', ".-..");
        m.put('M', "--");    m.put('N', "-.");    m.put('O', "---");   m.put('P', ".--.");
        m.put('Q', "--.-");  m.put('R', ".-.");   m.put('S', "...");   m.put('T', "-");
        m.put('U', "..-");   m.put('V', "...-");  m.put('W', ".--");   m.put('X', "-..-");
        m.put('Y', "-.--");  m.put('Z', "--..");
        // Digits
        m.put('0', "-----"); m.put('1', ".----"); m.put('2', "..---"); m.put('3', "...--");
        m.put('4', "....-"); m.put('5', "....."); m.put('6', "-...."); m.put('7', "--...");
        m.put('8', "---.."); m.put('9', "----.");
        // Punctuation
        m.put('.',  ".-.-.-"); m.put(',', "--..--"); m.put('?', "..--..");
        m.put('\'', ".----."); m.put('!', "-.-.--"); m.put('/', "-..-.");
        m.put('(',  "-.--.");  m.put(')', "-.--.-"); m.put('&', ".-...");
        m.put(':',  "---..."); m.put(';', "-.-.-."); m.put('=', "-...-");
        m.put('+',  ".-.-.");  m.put('-', "-....-"); m.put('_', "..--.-");
        m.put('"',  ".-..-."); m.put('$', "...-..-"); m.put('@', ".--.-.");

        CHAR_TO_MORSE = Collections.unmodifiableMap(m);

        Map<String, Character> reverse = new HashMap<>();
        for (Map.Entry<Character, String> e : m.entrySet()) reverse.put(e.getValue(), e.getKey());
        MORSE_TO_CHAR = Collections.unmodifiableMap(reverse);
    }

    private MorseCode() {}

    /** Returns the morse pattern for a character, or null if unknown. */
    public static String forChar(char c) {
        return CHAR_TO_MORSE.get(Character.toUpperCase(c));
    }

    /** Returns the character for a morse pattern, or null if unknown. */
    public static Character forPattern(String pattern) {
        return MORSE_TO_CHAR.get(pattern);
    }

    public static boolean isSupported(char c) {
        return CHAR_TO_MORSE.containsKey(Character.toUpperCase(c));
    }

    public static Map<Character, String> all() { return CHAR_TO_MORSE; }
}
