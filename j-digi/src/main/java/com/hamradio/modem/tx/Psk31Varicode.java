package com.hamradio.modem.tx;

import java.util.HashMap;
import java.util.Map;

public final class Psk31Varicode {

    private static final String[] ASCII_TO_VARICODE = new String[128];
    private static final Map<String, Character> VARICODE_TO_ASCII = new HashMap<>();

    static {
        // 0 - 31 control characters
        ASCII_TO_VARICODE[0] = "1010101011";   // NUL
        ASCII_TO_VARICODE[1] = "1011011011";   // SOH
        ASCII_TO_VARICODE[2] = "1011101101";   // STX
        ASCII_TO_VARICODE[3] = "1101110111";   // ETX
        ASCII_TO_VARICODE[4] = "1011101011";   // EOT
        ASCII_TO_VARICODE[5] = "1101011111";   // ENQ
        ASCII_TO_VARICODE[6] = "1011101111";   // ACK
        ASCII_TO_VARICODE[7] = "1011111101";   // BEL
        ASCII_TO_VARICODE[8] = "1011111111";   // BS
        ASCII_TO_VARICODE[9] = "11101111";     // HT
        ASCII_TO_VARICODE[10] = "11101";       // LF
        ASCII_TO_VARICODE[11] = "1101101111";  // VT
        ASCII_TO_VARICODE[12] = "1011011101";  // FF
        ASCII_TO_VARICODE[13] = "11111";       // CR
        ASCII_TO_VARICODE[14] = "1101110101";  // SO
        ASCII_TO_VARICODE[15] = "1110101011";  // SI
        ASCII_TO_VARICODE[16] = "1011110111";  // DLE
        ASCII_TO_VARICODE[17] = "1011110101";  // DC1
        ASCII_TO_VARICODE[18] = "1110101101";  // DC2
        ASCII_TO_VARICODE[19] = "1110101111";  // DC3
        ASCII_TO_VARICODE[20] = "1101011011";  // DC4
        ASCII_TO_VARICODE[21] = "1101101011";  // NAK
        ASCII_TO_VARICODE[22] = "1101101101";  // SYN
        ASCII_TO_VARICODE[23] = "1101010111";  // ETB
        ASCII_TO_VARICODE[24] = "1101111011";  // CAN
        ASCII_TO_VARICODE[25] = "1101111101";  // EM
        ASCII_TO_VARICODE[26] = "1110110111";  // SUB
        ASCII_TO_VARICODE[27] = "1101010101";  // ESC
        ASCII_TO_VARICODE[28] = "1101011101";  // FS
        ASCII_TO_VARICODE[29] = "1110111011";  // GS
        ASCII_TO_VARICODE[30] = "1011111011";  // RS
        ASCII_TO_VARICODE[31] = "1101111111";  // US

        // 32 - 126 printable ASCII
        ASCII_TO_VARICODE[32] = "1";           // space
        ASCII_TO_VARICODE[33] = "111111111";   // !
        ASCII_TO_VARICODE[34] = "101011111";   // "
        ASCII_TO_VARICODE[35] = "111110101";   // #
        ASCII_TO_VARICODE[36] = "111011011";   // $
        ASCII_TO_VARICODE[37] = "1011010101";  // %
        ASCII_TO_VARICODE[38] = "1010111011";  // &
        ASCII_TO_VARICODE[39] = "101111111";   // '
        ASCII_TO_VARICODE[40] = "11111011";    // (
        ASCII_TO_VARICODE[41] = "11110111";    // )
        ASCII_TO_VARICODE[42] = "101101111";   // *
        ASCII_TO_VARICODE[43] = "111011111";   // +
        ASCII_TO_VARICODE[44] = "1110101";     // ,
        ASCII_TO_VARICODE[45] = "110101";      // -
        ASCII_TO_VARICODE[46] = "1010111";     // .
        ASCII_TO_VARICODE[47] = "110101111";   // /
        ASCII_TO_VARICODE[48] = "10110111";    // 0
        ASCII_TO_VARICODE[49] = "10111101";    // 1
        ASCII_TO_VARICODE[50] = "11101101";    // 2
        ASCII_TO_VARICODE[51] = "11111111";    // 3
        ASCII_TO_VARICODE[52] = "101110111";   // 4
        ASCII_TO_VARICODE[53] = "101011011";   // 5
        ASCII_TO_VARICODE[54] = "101101011";   // 6
        ASCII_TO_VARICODE[55] = "110101101";   // 7
        ASCII_TO_VARICODE[56] = "110101011";   // 8
        ASCII_TO_VARICODE[57] = "110110111";   // 9
        ASCII_TO_VARICODE[58] = "11110101";    // :
        ASCII_TO_VARICODE[59] = "110111101";   // ;
        ASCII_TO_VARICODE[60] = "111101101";   // <
        ASCII_TO_VARICODE[61] = "1010101";     // =
        ASCII_TO_VARICODE[62] = "111010111";   // >
        ASCII_TO_VARICODE[63] = "1010101111";  // ?
        ASCII_TO_VARICODE[64] = "1010111101";  // @
        ASCII_TO_VARICODE[65] = "1111101";     // A
        ASCII_TO_VARICODE[66] = "11101011";    // B
        ASCII_TO_VARICODE[67] = "10101101";    // C
        ASCII_TO_VARICODE[68] = "10110101";    // D
        ASCII_TO_VARICODE[69] = "1110111";     // E
        ASCII_TO_VARICODE[70] = "11011011";    // F
        ASCII_TO_VARICODE[71] = "11111101";    // G
        ASCII_TO_VARICODE[72] = "101010101";   // H
        ASCII_TO_VARICODE[73] = "1111111";     // I
        ASCII_TO_VARICODE[74] = "111111101";   // J
        ASCII_TO_VARICODE[75] = "101111101";   // K
        ASCII_TO_VARICODE[76] = "11010111";    // L
        ASCII_TO_VARICODE[77] = "10111011";    // M
        ASCII_TO_VARICODE[78] = "11011101";    // N
        ASCII_TO_VARICODE[79] = "10101011";    // O
        ASCII_TO_VARICODE[80] = "11010101";    // P
        ASCII_TO_VARICODE[81] = "111011101";   // Q
        ASCII_TO_VARICODE[82] = "10101111";    // R
        ASCII_TO_VARICODE[83] = "1101111";     // S
        ASCII_TO_VARICODE[84] = "1101101";     // T
        ASCII_TO_VARICODE[85] = "101010111";   // U
        ASCII_TO_VARICODE[86] = "110110101";   // V
        ASCII_TO_VARICODE[87] = "101011101";   // W
        ASCII_TO_VARICODE[88] = "101110101";   // X
        ASCII_TO_VARICODE[89] = "101111011";   // Y
        ASCII_TO_VARICODE[90] = "1010101101";  // Z
        ASCII_TO_VARICODE[91] = "111110111";   // [
        ASCII_TO_VARICODE[92] = "111101111";   // \
        ASCII_TO_VARICODE[93] = "111111011";   // ]
        ASCII_TO_VARICODE[94] = "1010111111";  // ^
        ASCII_TO_VARICODE[95] = "101101101";   // _
        ASCII_TO_VARICODE[96] = "1011011111";  // `
        ASCII_TO_VARICODE[97] = "1011";        // a
        ASCII_TO_VARICODE[98] = "1011111";     // b
        ASCII_TO_VARICODE[99] = "101111";      // c
        ASCII_TO_VARICODE[100] = "101101";     // d
        ASCII_TO_VARICODE[101] = "11";         // e
        ASCII_TO_VARICODE[102] = "111101";     // f
        ASCII_TO_VARICODE[103] = "1011011";    // g
        ASCII_TO_VARICODE[104] = "101011";     // h
        ASCII_TO_VARICODE[105] = "1101";       // i
        ASCII_TO_VARICODE[106] = "111101011";  // j
        ASCII_TO_VARICODE[107] = "10111111";   // k
        ASCII_TO_VARICODE[108] = "11011";      // l
        ASCII_TO_VARICODE[109] = "111011";     // m
        ASCII_TO_VARICODE[110] = "1111";       // n
        ASCII_TO_VARICODE[111] = "111";        // o
        ASCII_TO_VARICODE[112] = "111111";     // p
        ASCII_TO_VARICODE[113] = "110111111";  // q
        ASCII_TO_VARICODE[114] = "10101";      // r
        ASCII_TO_VARICODE[115] = "10111";      // s
        ASCII_TO_VARICODE[116] = "101";        // t
        ASCII_TO_VARICODE[117] = "110111";     // u
        ASCII_TO_VARICODE[118] = "1111011";    // v
        ASCII_TO_VARICODE[119] = "1101011";    // w
        ASCII_TO_VARICODE[120] = "11011111";   // x
        ASCII_TO_VARICODE[121] = "1011101";    // y
        ASCII_TO_VARICODE[122] = "111010101";  // z
        ASCII_TO_VARICODE[123] = "1010110111"; // {
        ASCII_TO_VARICODE[124] = "110111011";  // |
        ASCII_TO_VARICODE[125] = "1010110101"; // }
        ASCII_TO_VARICODE[126] = "1011010111"; // ~
        ASCII_TO_VARICODE[127] = "1110110101"; // DEL

        for (int i = 0; i < ASCII_TO_VARICODE.length; i++) {
            String code = ASCII_TO_VARICODE[i];
            if (code != null && !code.isBlank()) {
                VARICODE_TO_ASCII.put(code, (char) i);
            }
        }
    }

    private Psk31Varicode() {
    }

    public static String encodeToBitStream(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String normalized = input.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder bits = new StringBuilder(normalized.length() * 12);

        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);

            if (ch == '\n') {
                appendAscii(bits, '\r');
                bits.append("00");
                appendAscii(bits, '\n');
                bits.append("00");
                continue;
            }

            if (ch < 0 || ch >= 128) {
                ch = '?';
            }

            appendAscii(bits, ch);
            bits.append("00");
        }

        return bits.toString();
    }

    public static Character decodeVaricodeBits(String bits) {
        if (bits == null || bits.isEmpty()) {
            return null;
        }
        return VARICODE_TO_ASCII.get(bits);
    }

    private static void appendAscii(StringBuilder bits, char ch) {
        String code = ASCII_TO_VARICODE[ch];
        if (code == null || code.isBlank()) {
            code = ASCII_TO_VARICODE['?'];
        }
        bits.append(code);
    }
}
