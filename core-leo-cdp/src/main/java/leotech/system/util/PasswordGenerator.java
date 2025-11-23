package leotech.system.util;

import java.security.SecureRandom;

public class PasswordGenerator {

    // Character sets
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}<>?";

    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be >= 8");
        }

        StringBuilder sb = new StringBuilder(length);

        // Ensure at least one of each (stronger password)
        sb.append(randomChar(UPPER));
        sb.append(randomChar(LOWER));
        sb.append(randomChar(DIGITS));
        sb.append(randomChar(SYMBOLS));

        // Fill the rest with random mix
        for (int i = sb.length(); i < length; i++) {
            sb.append(randomChar(ALL));
        }

        // Shuffle to avoid predictable first 4 chars
        return shuffle(sb.toString());
    }

    private static char randomChar(String src) {
        return src.charAt(RANDOM.nextInt(src.length()));
    }

    private static String shuffle(String input) {
        char[] a = input.toCharArray();
        for (int i = a.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return new String(a);
    }

}
