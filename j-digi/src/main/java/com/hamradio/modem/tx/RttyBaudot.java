package com.hamradio.modem.tx;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class RttyBaudot {

    public static final int LTRS = 0x1F;
    public static final int FIGS = 0x1B;

    private static final Map<Character, Integer> LETTERS = new HashMap<>();
    private static final Map<Character, Integer> FIGURES = new HashMap<>();

    static {
        putLetters('E', 0x01);
        putLetters('\n', 0x02);
        putLetters('A', 0x03);
        putLetters(' ', 0x04);
        putLetters('S', 0x05);
        putLetters('I', 0x06);
        putLetters('U', 0x07);
        putLetters('\r', 0x08);
        putLetters('D', 0x09);
        putLetters('R', 0x0A);
        putLetters('J', 0x0B);
        putLetters('N', 0x0C);
        putLetters('F', 0x0D);
        putLetters('C', 0x0E);
        putLetters('K', 0x0F);
        putLetters('T', 0x10);
        putLetters('Z', 0x11);
        putLetters('L', 0x12);
        putLetters('W', 0x13);
        putLetters('H', 0x14);
        putLetters('Y', 0x15);
        putLetters('P', 0x16);
        putLetters('Q', 0x17);
        putLetters('O', 0x18);
        putLetters('B', 0x19);
        putLetters('G', 0x1A);
        putLetters('M', 0x1C);
        putLetters('X', 0x1D);
        putLetters('V', 0x1E);

        putFigures('3', 0x01);
        putFigures('\n', 0x02);
        putFigures('-', 0x03);
        putFigures(' ', 0x04);
        putFigures('\'', 0x05);
        putFigures('8', 0x06);
        putFigures('7', 0x07);
        putFigures('\r', 0x08);
        putFigures('$', 0x09);
        putFigures('4', 0x0A);
        putFigures(',', 0x0C);
        putFigures('!', 0x0D);
        putFigures(':', 0x0E);
        putFigures('(', 0x0F);
        putFigures('5', 0x10);
        putFigures('"', 0x11);
        putFigures(')', 0x12);
        putFigures('2', 0x13);
        putFigures('#', 0x14);
        putFigures('6', 0x15);
        putFigures('0', 0x16);
        putFigures('1', 0x17);
        putFigures('9', 0x18);
        putFigures('?', 0x19);
        putFigures('&', 0x1A);
        putFigures('.', 0x1C);
        putFigures('/', 0x1D);
        putFigures(';', 0x1E);
    }

    private RttyBaudot() {
    }

    public enum ShiftState {
        LETTERS,
        FIGURES
    }

    public static EncodedText encodeText(String input) {
        String text = normalize(input);

        int[] temp = new int[Math.max(16, text.length() * 4)];
        int count = 0;
        ShiftState shift = ShiftState.LETTERS;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == '\n') {
                if (count + 2 >= temp.length) {
                    temp = grow(temp);
                }
                temp[count++] = 0x08; // CR
                temp[count++] = 0x02; // LF
                continue;
            }

            if (LETTERS.containsKey(ch)) {
                if (shift != ShiftState.LETTERS) {
                    if (count + 1 >= temp.length) {
                        temp = grow(temp);
                    }
                    temp[count++] = LTRS;
                    shift = ShiftState.LETTERS;
                }

                if (count + 1 >= temp.length) {
                    temp = grow(temp);
                }
                temp[count++] = LETTERS.get(ch);
                continue;
            }

            if (FIGURES.containsKey(ch)) {
                if (shift != ShiftState.FIGURES) {
                    if (count + 1 >= temp.length) {
                        temp = grow(temp);
                    }
                    temp[count++] = FIGS;
                    shift = ShiftState.FIGURES;
                }

                if (count + 1 >= temp.length) {
                    temp = grow(temp);
                }
                temp[count++] = FIGURES.get(ch);
                continue;
            }

            if (shift != ShiftState.LETTERS) {
                if (count + 1 >= temp.length) {
                    temp = grow(temp);
                }
                temp[count++] = LTRS;
                shift = ShiftState.LETTERS;
            }

            if (count + 1 >= temp.length) {
                temp = grow(temp);
            }
            temp[count++] = LETTERS.get(' ');
        }

        int[] codes = new int[count];
        System.arraycopy(temp, 0, codes, 0, count);
        return new EncodedText(codes);
    }

    private static String normalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String text = input.toUpperCase(Locale.ROOT);
        text = text.replace("\r\n", "\n");
        text = text.replace('\r', '\n');
        return text;
    }

    private static int[] grow(int[] src) {
        int[] dst = new int[src.length * 2];
        System.arraycopy(src, 0, dst, 0, src.length);
        return dst;
    }

    private static void putLetters(char c, int code) {
        LETTERS.put(c, code);
    }

    private static void putFigures(char c, int code) {
        FIGURES.put(c, code);
    }

    public static final class EncodedText {
        private final int[] codes;

        public EncodedText(int[] codes) {
            this.codes = codes;
        }

        public int[] getCodes() {
            return codes;
        }
    }
}
