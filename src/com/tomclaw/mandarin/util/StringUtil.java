package com.tomclaw.mandarin.util;

/**
 * Created with IntelliJ IDEA.
 * User: Solkin
 * Date: 13.10.13
 * Time: 20:09
 */
public class StringUtil {

    public static final int DEFAULT_ALPHABET_INDEX = '?';

    private static final String NUMERIC_REGEXP = "^[0-9]*$";

    public static int getAlphabetIndex(String nickName) {
        for (int c = 0; c < nickName.length(); c++) {
            char character = nickName.charAt(c);
            if (Character.isLetter(character)) {
                return Character.toUpperCase(character);
            }
        }
        return DEFAULT_ALPHABET_INDEX;
    }

    public static boolean isNumeric(String value) {
        return value.matches(NUMERIC_REGEXP);
    }
}
