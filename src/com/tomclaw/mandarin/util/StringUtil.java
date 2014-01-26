package com.tomclaw.mandarin.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

    public static String streamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            total.append(line + "\n");
        }
        return total.toString();
    }
}
